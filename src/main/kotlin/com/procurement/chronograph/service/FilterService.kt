package com.procurement.chronograph.service

import com.procurement.chronograph.channel.ReceiveFilterChannel
import com.procurement.chronograph.channel.SendNotificationChannel
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

interface FilterService {
    fun run()
}

@Service
class FilterServiceImpl @Autowired constructor(
    private val taskRepository: TaskRepository,
    @Qualifier("filterChannel") private val filterChannel: ReceiveFilterChannel,
    @Qualifier("notificationChannel") private val notificationChannel: SendNotificationChannel
) : FilterService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(FilterService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.debug("Starting 'FilterService'...")
        while (true) {
            select<Unit> {
                filterChannel.onReceive { tasks ->
                    log.debug("Received a set of tasks from the cache for processing: $this.")
                    tasks.forEach { task ->
                        if (taskRepository.exists(task.key)) {
                            notificationChannel.send(task)
                            log.debug("A task was sent for processing: $this")
                        } else {
                            log.debug("The task was dropped, because it is inactive: $this.")
                        }
                    }
                }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }
}
