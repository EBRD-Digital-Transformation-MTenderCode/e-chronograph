package com.procurement.chronograph.repository

import com.procurement.chronograph.DatabaseTestConfiguration
import com.procurement.chronograph.exception.task.TaskAlreadyException
import com.procurement.chronograph.exception.task.TaskNotFoundException
import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId
import com.procurement.chronograph.domain.task.Task
import liquibase.Contexts
import liquibase.Liquibase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class TaskRepositoryTest {
    companion object {
        private const val insertRequestId = 1L
        private const val deactivateRequestId = 2L
        private const val cancelRequestId = 3L
        private const val replaceRequestId = 4L

        private const val OCID = "ocid-1"
        private const val PHASE = "phase-1"
        private const val DATA = "json-1"

        private val LAUNCH_TIME = LocalDateTime.now().plusDays(1)
        private val SENT_TIME = LAUNCH_TIME.minusHours(2)
        private val RECEIVED_TIME = LAUNCH_TIME.minusHours(1)
    }

    @Autowired
    private lateinit var datasource: DataSource

    @Autowired
    private lateinit var liquibase: Liquibase

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @BeforeEach
    fun setup() {
        println("The database is initializing...")
        liquibase.update(Contexts())
        println("The database was initialized.")
    }

    @Test
    @DisplayName("Testing the method exists task (task is found).")
    fun existsTrue() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        assertTrue(taskRepository.exists(task.key))
    }

    @Test
    @DisplayName("Testing the method exists task (task is not found).")
    fun existsFalse() {
        val task = genTask(insertRequestId)
        assertFalse(taskRepository.exists(task.key))
    }

    @Test
    @DisplayName("Testing the method load tasks(less end date).")
    fun loadLessEnd() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        val list1 = taskRepository.load(LAUNCH_TIME)
        assertEquals(list1.size, 0)

        val list2 = taskRepository.load(LAUNCH_TIME.plusMinutes(1))
        assertEquals(list2.size, 1)
    }

    @Test
    @DisplayName("Testing the method load tasks(between start and end date).")
    fun loadBetweenStartAndEnd() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        val list1 = taskRepository.load(LAUNCH_TIME.minusDays(1), LAUNCH_TIME.minusSeconds(1))
        assertEquals(list1.size, 0)

        val list2 = taskRepository.load(LAUNCH_TIME.plusSeconds(1), LAUNCH_TIME.plusDays(1))
        assertEquals(list2.size, 0)

        val list3 = taskRepository.load(LAUNCH_TIME, LAUNCH_TIME.plusDays(1))
        assertEquals(list3.size, 1)
    }

    @Test
    @DisplayName("Testing the method save the task.")
    fun save() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        val list = taskRepository.load(LAUNCH_TIME.plusDays(1))
        assertEquals(list.size, 1)

        val actual = list.get(0)
        assertEquals(OCID, actual.key.ocid)
        assertEquals(PHASE, actual.key.phase)
        assertEquals(LAUNCH_TIME, actual.launchTime)
        assertEquals(DATA, actual.metaData)
    }

    @Test
    @DisplayName("Testing the method save task (duplication of tasks).")
    fun saveTaskAlreadyException() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)
        assertThrows(TaskAlreadyException::class.java) {
            taskRepository.save(task)
        }
    }

    @Test
    @DisplayName("Testing the method replace task.")
    fun replace() {
        appendInsertRequest()

        val newTask = genTask(insertRequestId)
        taskRepository.save(newTask)

        appendReplaceRequest()
        val replaceTask = genTask(replaceRequestId)
        taskRepository.replace(replaceTask)
    }

    @Test
    @DisplayName("Testing the method replace task. (task is not exists)")
    fun replaceTaskNotFoundException() {
        val task = genTask(replaceRequestId)
        assertThrows(TaskNotFoundException::class.java) {
            taskRepository.replace(task)
        }
    }

    @Test
    @DisplayName("Testing the method deactivate task.")
    fun deactivate() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        appendDeactivateRequest()
        taskRepository.deactivate(task)
    }

    @Test
    @DisplayName("Testing the method deactivate task. (task is not exists)")
    fun deactivateTaskNotFoundException() {
        val task = genTask(insertRequestId)
        assertThrows(TaskNotFoundException::class.java) {
            taskRepository.deactivate(task)
        }
    }

    @Test
    @DisplayName("Testing the method cancel task.")
    fun cancel() {
        appendInsertRequest()

        val task = genTask(insertRequestId)
        taskRepository.save(task)

        appendCancelRequest()
        taskRepository.cancel(cancelRequestId, task.key)
    }

    @Test
    @DisplayName("Testing the method cancel task. (task is not exists)")
    fun cancelTaskNotFoundException() {
        val key = genKey()
        assertThrows(TaskNotFoundException::class.java) {
            taskRepository.cancel(cancelRequestId, key)
        }
    }

    @AfterEach
    fun clear() {
        println("The database is clearing...")
        liquibase.dropAll()
        println("The database was cleared.")
    }

    private fun genKey() = Key(ocid = OCID, phase = PHASE)

    private fun genTask(requestId: RequestId) = Task(
        requestId = requestId,
        key = genKey(),
        launchTime = LAUNCH_TIME,
        metaData = DATA)

    private fun appendInsertRequest() {
        execute("""
            INSERT INTO requests(id, used, action, ocid, phase, launch_time, task_data, sent_time, received_time)
            VALUES($insertRequestId, false, 'CREATE', '$OCID', '$PHASE', '$LAUNCH_TIME', '$DATA', '$SENT_TIME', '$RECEIVED_TIME')
            """.trimIndent()
        )
    }

    private fun appendReplaceRequest() {
        execute("""
            INSERT INTO requests(id, used, action, ocid, phase, launch_time, task_data, sent_time, received_time)
            VALUES($replaceRequestId, false, 'REPLACE', '$OCID', '$PHASE', '$LAUNCH_TIME', '$DATA', '$SENT_TIME', '$RECEIVED_TIME')
            """.trimIndent()
        )
    }

    private fun appendDeactivateRequest() {
        execute("""
            INSERT INTO requests(id, used, action, ocid, phase, launch_time, task_data, sent_time, received_time)
            VALUES($deactivateRequestId, false, 'DEACTIVATE', '$OCID', '$PHASE', '$LAUNCH_TIME', '$DATA', '$SENT_TIME', '$RECEIVED_TIME')
            """.trimIndent()
        )
    }

    private fun appendCancelRequest() {
        execute("""
            INSERT INTO requests(id, used, action, ocid, phase, launch_time, task_data, sent_time, received_time)
            VALUES($cancelRequestId, false, 'CANCEL', '$OCID', '$PHASE', '$LAUNCH_TIME', '$DATA', '$SENT_TIME', '$RECEIVED_TIME')
            """.trimIndent()
        )
    }

    private fun execute(sql: String) {
        JdbcTemplate(datasource).apply {
            execute(sql)
        }
    }
}
