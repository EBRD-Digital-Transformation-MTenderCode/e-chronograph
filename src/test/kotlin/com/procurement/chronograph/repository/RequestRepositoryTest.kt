package com.procurement.chronograph.repository

import com.procurement.chronograph.DatabaseTestConfiguration
import com.procurement.chronograph.domain.request.*
import liquibase.Contexts
import liquibase.Liquibase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class RequestRepositoryTest {
    companion object {
        private const val OCID_SCHEDULE = "ocid-1"
        private const val OCID_REPLACE = "ocid-2"
        private const val OCID_CANCEL = "ocid-3"

        private const val PHASE_SCHEDULE = "phase-1"
        private const val PHASE_REPLACE = "phase-2"
        private const val PHASE_CANCEL = "phase-3"

        private const val DATA_SCHEDULE = "json-1"
        private const val DATA_REPLACE = "json-2"

        private val CURRENT_TIME = LocalDateTime.now()

        private val LAUNCH_TIME_SCHEDULE = CURRENT_TIME.plusDays(1)
        private val LAUNCH_TIME_REPLACE = CURRENT_TIME.plusDays(2)

        private val SENT_TIME = CURRENT_TIME.minusHours(2)
    }

    @Autowired
    private lateinit var liquibase: Liquibase

    @Autowired
    private lateinit var requestRepository: RequestRepository

    @BeforeEach
    fun setup() {
        println("The database is initializing...")
        liquibase.update(Contexts())
        println("The database was initialized.")
    }

    @Test
    @DisplayName("Testing the method load.")
    fun load() {
        val empty = requestRepository.load()
        assertEquals(empty.size, 0)

        val scheduleMessage = getScheduleRequestMessage()
        requestRepository.save(scheduleMessage)

        val replaceMessage = getReplaceRequestMessage()
        requestRepository.save(replaceMessage)

        val cancelMessage = getCancelRequestMessage()
        requestRepository.save(cancelMessage)

        val list = requestRepository.load()
        assertEquals(list.size, 3)
        list.forEach { message ->
            when (message) {
                is ScheduleRequest -> {
                    assertEquals(message.key.ocid, OCID_SCHEDULE)
                    assertEquals(message.key.phase, PHASE_SCHEDULE)
                    assertEquals(message.launchTime, LAUNCH_TIME_SCHEDULE)
                    assertEquals(message.metaData, DATA_SCHEDULE)
                }

                is ReplaceRequest -> {
                    assertEquals(message.key.ocid, OCID_REPLACE)
                    assertEquals(message.key.phase, PHASE_REPLACE)
                    assertEquals(message.newLaunchTime, LAUNCH_TIME_REPLACE)
                    assertEquals(message.metaData, DATA_REPLACE)
                }

                is CancelRequest -> {
                    assertEquals(message.key.ocid, OCID_CANCEL)
                    assertEquals(message.key.phase, PHASE_CANCEL)
                }
            }
        }
    }

    @Test
    @DisplayName("Testing the method save for 'BodyScheduleRequestMessage'.")
    fun saveSchedule() {
        val message = getScheduleRequestMessage()
        requestRepository.save(message)
    }

    @Test
    @DisplayName("Testing the method save for 'BodyReplaceRequestMessage'.")
    fun saveReplace() {
        val message = getReplaceRequestMessage()
        requestRepository.save(message)
    }

    @Test
    @DisplayName("Testing the method save for 'BodyCancelRequestMessage'.")
    fun saveCancel() {
        val message = getCancelRequestMessage()
        requestRepository.save(message)
    }

    @AfterEach
    fun clear() {
        println("The database is clearing...")
        liquibase.dropAll()
        println("The database was cleared.")
    }

    private fun getScheduleRequestMessage() =
        ScheduleMessage(
            ScheduleMessageBody(
                ocid = OCID_SCHEDULE,
                phase = PHASE_SCHEDULE,
                launchTime = LAUNCH_TIME_SCHEDULE,
                metaData = DATA_SCHEDULE
            ),
            sentTime = SENT_TIME
        )

    private fun getReplaceRequestMessage() =
        ReplaceMessage(
            ReplaceMessageBody(
                ocid = OCID_REPLACE,
                phase = PHASE_REPLACE,
                launchTime = LAUNCH_TIME_REPLACE,
                metaData = DATA_REPLACE
            ),
            sentTime = SENT_TIME
        )

    private fun getCancelRequestMessage() =
        CancelMessage(
            CancelMessageBody(
                ocid = OCID_CANCEL,
                phase = PHASE_CANCEL
            ),
            sentTime = SENT_TIME
        )
}
