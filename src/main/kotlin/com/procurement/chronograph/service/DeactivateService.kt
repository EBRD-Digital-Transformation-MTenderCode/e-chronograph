package com.procurement.chronograph.service

import com.procurement.chronograph.channel.ReceiveDeactivateChannel
import com.procurement.chronograph.domain.task.Task
import com.procurement.chronograph.exception.task.DeactivateTaskException
import com.procurement.chronograph.repository.TaskRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

interface DeactivateService {
    fun run()
}

@Service
class DeactivateServiceImpl @Autowired constructor(
    private val taskRepository: TaskRepository,
    @Qualifier("deactivateChannel") private val deactivateChannel: ReceiveDeactivateChannel
) : DeactivateService {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DeactivateService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.debug("Starting 'DeactivateService'...")
        while (true) {
            select<Unit> {
                deactivateChannel.onReceive { task -> task.deactivate() }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }

    private fun Task.deactivate() = try {
        taskRepository.deactivate(task = this)
        true
    } catch (ex: Exception) {
        when (ex) {
            is DeactivateTaskException -> {
                log.error(ex.message, ex)
            }

            else -> {
                log.error("Error of deactivate task.", ex)
            }
        }
        false
    }
}
