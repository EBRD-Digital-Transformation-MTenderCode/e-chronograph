package com.procurement.chronograph.repository

import com.procurement.chronograph.exception.RecordNotFound
import com.procurement.chronograph.exception.request.SavedRequestException
import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.*
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

interface RequestRepository {
    fun load(): List<Request>

    fun save(message: Message): RequestId

    fun markAsUsed(markRequest: MarkRequest): RequestId
}

enum class Actions {
    SCHEDULE, REPLACE, CANCEL
}

@Repository
class RequestRepositoryImpl @Autowired constructor(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : RequestRepository {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestRepositoryImpl::class.java)

        @Language("PostgreSQL")
        const val LOAD_SQL = """
SELECT id, action, ocid, phase, launch_time, task_data, sent_time, received_time
FROM requests
WHERE used = FALSE
"""

        @Language("PostgreSQL")
        const val SAVE_SCHEDULE_REQUEST_SQL = """
INSERT INTO requests (action, ocid, phase, launch_time, task_data, sent_time, received_time)
VALUES (:action, :ocid, :phase, :newLaunchTime, :taskData, :sentTime, :receivedTime)
RETURNING id;
"""
        @Language("PostgreSQL")
        const val SAVE_REPLACE_REQUEST_SQL = """
INSERT INTO requests (action, ocid, phase, launch_time, task_data, sent_time, received_time)
VALUES (:action, :ocid, :phase, :newLaunchTime, :taskData, :sentTime, :receivedTime)
RETURNING id;
"""
        @Language("PostgreSQL")
        const val SAVE_CANCEL_REQUEST_SQL = """
INSERT INTO requests (action, ocid, phase, sent_time, received_time)
VALUES (:action, :ocid, :phase, :sentTime, :receivedTime)
RETURNING id;
"""

        @Language("PostgreSQL")
        const val MARK_REQUEST_AS_USED = """
UPDATE requests
SET used = TRUE
WHERE used = FALSE AND id = :requestId
RETURNING id;
"""
    }

    @Transactional
    override fun load(): List<Request> = jdbcTemplate.query(
        LOAD_SQL,
        mapOf<String, Any>(),
        this::mappingRequest
    )

    @Transactional
    override fun save(message: Message): RequestId = try {
        when (message) {
            is ScheduleMessage -> {
                message.save().also { id ->
                    log.debug("The request was saved (id: $id, $message).")
                }
            }

            is ReplaceMessage -> {
                message.save().also { id ->
                    log.debug("The request was saved (id: $id, $message).")
                }
            }

            is CancelMessage -> {
                message.save().also { id ->
                    log.debug("The request was saved (id: $id, $message).")
                }
            }
        }
    } catch (ex: Exception) {
        throw SavedRequestException(ex)
    }

    @Transactional
    override fun markAsUsed(markRequest: MarkRequest): RequestId =
        jdbcTemplate.query(
            MARK_REQUEST_AS_USED,
            mapOf("requestId" to markRequest.id),
            ::mappingId
        ).getOrElse(0, {
            throw RecordNotFound(requestId = markRequest.id,
                                                                       key = markRequest.key)
        })

    private fun ScheduleMessage.save() = jdbcTemplate.queryForObject(
        SAVE_SCHEDULE_REQUEST_SQL,
        mapOf(
            "action" to Actions.SCHEDULE.toString(),
            "ocid" to this.body.ocid,
            "phase" to this.body.phase,
            "newLaunchTime" to this.body.launchTime,
            "taskData" to this.body.metaData,
            "sentTime" to this.sentTime,
            "receivedTime" to this.receivedTime
        ),
        ::mappingId
    )

    private fun ReplaceMessage.save() = jdbcTemplate.queryForObject(
        SAVE_REPLACE_REQUEST_SQL,
        mapOf(
            "action" to Actions.REPLACE.toString(),
            "ocid" to this.body.ocid,
            "phase" to this.body.phase,
            "newLaunchTime" to this.body.launchTime,
            "taskData" to this.body.metaData,
            "sentTime" to this.sentTime,
            "receivedTime" to this.receivedTime
        ),
        ::mappingId
    )

    private fun CancelMessage.save() = jdbcTemplate.queryForObject(
        SAVE_CANCEL_REQUEST_SQL,
        mapOf(
            "action" to Actions.CANCEL.toString(),
            "ocid" to this.body.ocid,
            "phase" to this.body.phase,
            "sentTime" to this.sentTime,
            "receivedTime" to this.receivedTime
        ),
        ::mappingId
    )

    private fun mappingId(rs: ResultSet, rowNum: Int): Long = rs.getLong(1)

    private fun mappingRequest(rs: ResultSet, rowNum: Int): Request {
        val action = Actions.valueOf(rs.getAction())
        return when (action) {
            Actions.SCHEDULE -> {
                ScheduleRequest(
                    id = rs.getId(),
                    key = Key(
                        ocid = rs.getOcid(),
                        phase = rs.getPhase()
                    ),
                    launchTime = rs.getLaunchTime(),
                    metaData = rs.getData(),
                    sentTime = rs.getSentTime(),
                    receivedTime = rs.getReceivedTime()
                )
            }

            Actions.REPLACE -> {
                ReplaceRequest(
                    id = rs.getId(),
                    key = Key(
                        ocid = rs.getOcid(),
                        phase = rs.getPhase()
                    ),
                    newLaunchTime = rs.getLaunchTime(),
                    metaData = rs.getData(),
                    sentTime = rs.getSentTime(),
                    receivedTime = rs.getReceivedTime()
                )
            }

            Actions.CANCEL -> {
                CancelRequest(
                    id = rs.getId(),
                    key = Key(
                        ocid = rs.getOcid(),
                        phase = rs.getPhase()
                    ),
                    sentTime = rs.getSentTime(),
                    receivedTime = rs.getReceivedTime()
                )
            }
        }
    }

    private fun ResultSet.getId() = this.getLong(1)
    private fun ResultSet.getAction() = this.getString(2)
    private fun ResultSet.getOcid() = this.getString(3)
    private fun ResultSet.getPhase() = this.getString(4)
    private fun ResultSet.getLaunchTime() = this.getTimestamp(5).toLocalDateTime()
    private fun ResultSet.getData() = this.getString(6)
    private fun ResultSet.getSentTime() = this.getTimestamp(7).toLocalDateTime()
    private fun ResultSet.getReceivedTime() = this.getTimestamp(8).toLocalDateTime()
}
