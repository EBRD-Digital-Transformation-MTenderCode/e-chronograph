package com.procurement.chronograph.repository

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId
import com.procurement.chronograph.domain.task.Task
import com.procurement.chronograph.exception.RecordNotFound
import com.procurement.chronograph.exception.task.*
import com.procurement.chronograph.times.nowUTC
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime

interface TaskRepository {
    fun load(endPeriod: LocalDateTime): List<Task>

    fun load(startPeriod: LocalDateTime, endPeriod: LocalDateTime): List<Task>

    fun exists(key: Key): Boolean

    fun save(task: Task)

    fun replace(task: Task)

    fun cancel(requestId: RequestId, key: Key)

    fun deactivate(task: Task)
}

@Repository
class TaskRepositoryImpl @Autowired constructor(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : TaskRepository {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TaskRepositoryImpl::class.java)

        @Language("PostgreSQL")
        const val LOAD_SQL = """
SELECT
  request_id,
  ocid,
  phase,
  launch_time,
  task_data
FROM tasks
WHERE active = TRUE AND launch_time < :endTime
"""

        @Language("PostgreSQL")
        const val LOAD_BETWEEN_SQL = """
SELECT
  request_id,
  ocid,
  phase,
  launch_time,
  task_data
FROM tasks
WHERE active = TRUE AND launch_time >= :startTime AND launch_time < :endTime
"""

        @Language("PostgreSQL")
        const val EXISTS_SQL = """
SELECT exists(SELECT 1
              FROM tasks
              WHERE active = TRUE AND ocid = :ocid AND phase = :phase)
"""

        @Language("PostgreSQL")
        const val SAVE_SQL = """
INSERT INTO tasks (request_id, active, ocid, phase, launch_time, task_data, created_time)
VALUES (:request_id, TRUE, :ocid, :phase, :newLaunchTime, :taskData, :createdTime)
RETURNING request_id;
"""

        @Language("PostgreSQL")
        const val CANCEL_SQL = """
UPDATE tasks
SET active = FALSE, canceled_time = :canceledTime
WHERE active = TRUE AND ocid = :ocid AND phase = :phase
RETURNING request_id;
"""

        @Language("PostgreSQL")
        const val DEACTIVATE_SQL = """
UPDATE tasks
SET active = FALSE, deactivate_time = :deactivateTime
WHERE active = TRUE AND ocid = :ocid AND phase = :phase
RETURNING request_id;
"""

        @Language("PostgreSQL")
        const val MARK_REQUEST_SQL = """
UPDATE requests
SET used = TRUE
WHERE used = FALSE AND id = :requestId;
"""
    }

    @Transactional(readOnly = true)
    override fun load(endPeriod: LocalDateTime): List<Task> = jdbcTemplate.query(
        LOAD_SQL,
        mapOf("endTime" to endPeriod),
        this::mappingTask
    )

    @Transactional(readOnly = true)
    override fun load(startPeriod: LocalDateTime, endPeriod: LocalDateTime): List<Task> = jdbcTemplate.query(
        LOAD_BETWEEN_SQL,
        mapOf("startTime" to startPeriod, "endTime" to endPeriod),
        this::mappingTask
    )

    @Transactional(readOnly = true)
    override fun exists(key: Key): Boolean = jdbcTemplate.query(
        EXISTS_SQL,
        mapOf("ocid" to key.ocid,
              "phase" to key.phase
        ),
        this::mappingExists
    ).single()

    @Transactional
    override fun save(task: Task) = try {
        saveTask(task)
        requestProcessed(requestId = task.requestId)
        log.debug("The task was saved (request id: ${task.requestId}, task: ${task.key}).")
    } catch (ex: Exception) {
        when (ex) {
            is DuplicateKeyException -> throw TaskAlreadyException(requestId = task.requestId,
                                                                   key = task.key,
                                                                   exception = ex
            )
            else -> throw SavedTaskException(task.requestId, task.key, ex)
        }
    }

    @Transactional
    override fun replace(task: Task) = try {
        val oldRequestId: Long = cancelTask(task.requestId, task.key)
        saveTask(task)
        requestProcessed(requestId = task.requestId)
        log.debug("The task (request id: $oldRequestId, task: ${task.key}) was replaced (request id: ${task.requestId}, task: ${task.key})")
    } catch (ex: Exception) {
        when (ex) {
            is RecordNotFound ->
                throw TaskNotFoundException(requestId = ex.requestId,
                                            key = ex.key,
                                            message = "Task (request id: ${ex.requestId}, key: ${ex.key}) for replace not found."
                )
            else -> throw ReplaceTaskException(task.requestId, task.key, ex)
        }
    }

    @Transactional
    override fun deactivate(task: Task) = try {
        deactivateTask(task.requestId, task.key)
        log.debug("The task was deactivated (request id: ${task.requestId}, key: ${task.key}).")
    } catch (ex: Exception) {
        when (ex) {
            is RecordNotFound ->
                throw TaskNotFoundException(requestId = task.requestId,
                                            key = task.key,
                                            message = "Task for deactivate not found (request id: ${task.requestId}, key: ${task.key})."
                )
            else -> throw DeactivateTaskException(task.requestId, task.key, ex)
        }
    }

    @Transactional
    override fun cancel(requestId: RequestId, key: Key) = try {
        val oldRequestId: Long = cancelTask(requestId, key)
        requestProcessed(requestId = requestId)
        log.debug("The task (request id: $oldRequestId, key: $key) was cancelled (request id: $requestId, key: $key)")
    } catch (ex: Exception) {
        when (ex) {
            is RecordNotFound ->
                throw TaskNotFoundException(requestId = requestId,
                                            key = key,
                                            message = "Task for cancel not found (request id: $requestId, key: $key)."
                )
            else -> throw CancelTaskException(requestId, key, ex)
        }
    }

    private fun saveTask(task: Task) = jdbcTemplate.queryForObject(
        SAVE_SQL,
        mapOf("request_id" to task.requestId,
              "ocid" to task.key.ocid,
              "phase" to task.key.phase,
              "newLaunchTime" to task.launchTime,
              "taskData" to task.metaData,
              "createdTime" to nowUTC()
        ),
        this::mappingId
    )

    private fun cancelTask(requestId: RequestId, key: Key): RequestId = jdbcTemplate.query(
        CANCEL_SQL,
        mapOf(
            "ocid" to key.ocid,
            "phase" to key.phase,
            "canceledTime" to nowUTC()
        ),
        this::mappingId
    ).getOrElse(0, { throw RecordNotFound(requestId = requestId, key = key) })

    private fun deactivateTask(requestId: RequestId, key: Key): RequestId = jdbcTemplate.query(
        DEACTIVATE_SQL,
        mapOf("ocid" to key.ocid,
              "phase" to key.phase,
              "deactivateTime" to nowUTC()
        ),
        this::mappingId
    ).getOrElse(0, { throw RecordNotFound(requestId = requestId, key = key) })

    private fun requestProcessed(requestId: RequestId) = jdbcTemplate.update(
        MARK_REQUEST_SQL,
        mapOf("requestId" to requestId)
    )

    private fun mappingTask(rs: ResultSet, rowNum: Int): Task = Task(
        requestId = rs.getRequestId(),
        key = Key(
            ocid = rs.getOcid(),
            phase = rs.getPhase()
        ),
        launchTime = rs.getLaunchTime(),
        metaData = rs.getData()
    )

    private fun mappingId(rs: ResultSet, rowNum: Int): Long = rs.getLong(1)

    private fun mappingExists(rs: ResultSet, rowNum: Int): Boolean = rs.getBoolean(1)

    private fun ResultSet.getRequestId() = this.getLong(1)
    private fun ResultSet.getOcid() = this.getString(2)
    private fun ResultSet.getPhase() = this.getString(3)
    private fun ResultSet.getLaunchTime() = this.getTimestamp(4).toLocalDateTime()
    private fun ResultSet.getData() = this.getString(5)
}
