package com.ocds.chronograph.service

import com.ocds.chronograph.channel.ReceiveCommandChannel
import com.ocds.chronograph.channel.SendCacheChannel
import com.ocds.chronograph.channel.SendErrorChannel
import com.ocds.chronograph.exception.task.TaskAlreadyException
import com.ocds.chronograph.exception.task.TaskNotFoundException
import com.ocds.chronograph.model.domain.ClosedTimeRange
import com.ocds.chronograph.model.domain.EmptyTimeRange
import com.ocds.chronograph.model.domain.OpenTimeRange
import com.ocds.chronograph.model.domain.command.CancelTaskCommand
import com.ocds.chronograph.model.domain.command.LoadTaskCommand
import com.ocds.chronograph.model.domain.command.ReplaceTaskCommand
import com.ocds.chronograph.model.domain.command.ScheduleTaskCommand
import com.ocds.chronograph.model.domain.response.CancelErrorResponse
import com.ocds.chronograph.model.domain.response.ReplaceErrorResponse
import com.ocds.chronograph.model.domain.response.ScheduleErrorResponse
import com.ocds.chronograph.model.domain.task.Task
import com.ocds.chronograph.repository.TaskRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

interface PersistService {
    fun run()
}

@Service
class PersistServiceImpl @Autowired constructor(
    private val taskRepository: TaskRepository,
    @Qualifier("commandChannel") private val commandChannel: ReceiveCommandChannel,
    @Qualifier("cacheChannel") private val cacheChannel: SendCacheChannel,
    @Qualifier("errorChannel") private val errorChannel: SendErrorChannel
) : PersistService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(PersistService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.debug("Starting 'PersistService'...")
        while (true) {
            select<Unit> {
                commandChannel.onReceive { command ->
                    when (command) {
                        is LoadTaskCommand -> command.loading()
                        is ScheduleTaskCommand -> command.scheduling()
                        is ReplaceTaskCommand -> command.replacing()
                        is CancelTaskCommand -> command.cancelling()
                    }
                }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }

    private suspend fun LoadTaskCommand.loading() {
        val timeRange = this.timeRange
        when (timeRange) {
            is EmptyTimeRange -> {
                log.info("The command of 'Load' is drop, because 'TimeRange' is empty: $timeRange")
            }

            is OpenTimeRange -> {
                taskRepository.load(timeRange.endExclusive)
                    .forEach { task -> task.sentCache() }
            }

            is ClosedTimeRange -> {
                taskRepository.load(timeRange.start, timeRange.endExclusive)
                    .forEach { task -> task.sentCache() }
            }
        }
    }

    private suspend fun ScheduleTaskCommand.scheduling() {
        val task = Task(requestId = this.requestId,
                        key = this.key,
                        launchTime = this.launchTime,
                        metaData = this.metaData)
        if (task.save()) {
            val timeRange = this.timeRange
            when (timeRange) {
                is EmptyTimeRange -> {
                }

                is OpenTimeRange -> if (task.launchTime < timeRange.endExclusive) task.sentCache()
                is ClosedTimeRange -> if (task.launchTime < timeRange.endExclusive) task.sentCache()
            }
        }
    }

    private suspend fun Task.save(): Boolean = try {
        taskRepository.save(this)
        true
    } catch (ex: Exception) {
        when (ex) {
            is TaskAlreadyException -> {
                log.error(ex.message, ex)
                val error = ScheduleErrorResponse(
                    data = ScheduleErrorResponse.Data(
                        requestId = this.requestId,
                        ocid = this.key.ocid,
                        phase = this.key.phase,
                        launchTime = this.launchTime,
                        metaData = this.metaData
                    )
                )
                errorChannel.send(error)
                log.debug("A message about error create of task was sent: $error.")
            }

            else -> log.error(ex.message, ex)
        }
        false
    }

    private suspend fun ReplaceTaskCommand.replacing() {
        val task = Task(requestId = this.requestId,
                        key = this.key,
                        launchTime = this.newLaunchTime,
                        metaData = this.metaData)
        if (task.replace()) {
            val timeRange = this.timeRange
            when (timeRange) {
                is EmptyTimeRange -> {
                }

                is OpenTimeRange -> if (task.launchTime < timeRange.endExclusive) task.sentCache()
                is ClosedTimeRange -> if (task.launchTime < timeRange.endExclusive) task.sentCache()
            }
        }
    }

    private suspend fun Task.replace(): Boolean = try {
        taskRepository.replace(this)
        true
    } catch (ex: Exception) {
        when (ex) {
            is TaskNotFoundException -> {
                log.error(ex.message, ex)
                val error = ReplaceErrorResponse(
                    data = ReplaceErrorResponse.Data(
                        requestId = this.requestId,
                        ocid = this.key.ocid,
                        phase = this.key.phase,
                        newLaunchTime = this.launchTime,
                        metaData = this.metaData
                    )
                )
                errorChannel.send(error)
                log.debug("A message about error replace of task was sent: $error.")
            }

            else -> log.error(ex.message, ex)
        }
        false
    }

    private suspend fun CancelTaskCommand.cancelling() = try {
        taskRepository.cancel(this.requestId, this.key)
    } catch (ex: Exception) {
        when (ex) {
            is TaskNotFoundException -> {
                log.error(ex.message, ex)
                val error = CancelErrorResponse(
                    data = CancelErrorResponse.Data(
                        requestId = this.requestId,
                        ocid = this.key.ocid,
                        phase = this.key.phase
                    )
                )
                errorChannel.send(error)
                log.debug("A message about error cancel of task was sent: $error.")
            }

            else -> log.error(ex.message, ex)
        }
    }

    private suspend fun Task.sentCache() {
        cacheChannel.send(this)
        log.debug("A task was sent to cache: $this.")
    }
}
