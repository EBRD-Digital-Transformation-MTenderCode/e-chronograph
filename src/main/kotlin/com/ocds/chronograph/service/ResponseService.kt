package com.ocds.chronograph.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ocds.chronograph.channel.ReceiveErrorChannel
import com.ocds.chronograph.channel.ReceiveNotificationChannel
import com.ocds.chronograph.channel.SendDeactivateChannel
import com.ocds.chronograph.channel.SendMarkRequestChannel
import com.ocds.chronograph.model.domain.Key
import com.ocds.chronograph.model.domain.request.MarkRequest
import com.ocds.chronograph.model.domain.response.*
import com.ocds.chronograph.model.domain.task.Task
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

interface ResponseService {
    fun run()
}

@Service
class ResponseServiceImpl @Autowired constructor(
    private val mapper: ObjectMapper,
    private val kafkaProducerService: KafkaProducerService,
    @Qualifier("notificationChannel") private val notificationChannel: ReceiveNotificationChannel,
    @Qualifier("errorChannel") private val errorChannel: ReceiveErrorChannel,
    @Qualifier("deactivateChannel") private val deactivateChannel: SendDeactivateChannel,
    @Qualifier("markRequestChannel") private val markRequestChannel: SendMarkRequestChannel
) : ResponseService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ResponseService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.debug("Starting 'ResponseService'...")
        while (true) {
            select<Unit> {
                notificationChannel.onReceive { task -> task.processing() }
                errorChannel.onReceive { error -> error.processing() }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }

    private suspend fun Task.processing() {
        val notification = NotificationResponse(
            data = NotificationResponse.Data(
                ocid = this.key.ocid,
                phase = this.key.phase,
                launchTime = this.launchTime,
                metaData = this.metaData
            )
        )
        notification.toJson()?.let { json ->
            kafkaProducerService.send(json)
            this.deactivate()
        }
    }

    private suspend fun ErrorResponse.processing() {
        val response = this
        when (response) {
            is ParseBodyRequestErrorResponse -> {
                response.send()
            }

            is SaveBodyRequestErrorResponse -> {
                response.send()
            }

            is ExpireLaunchTimeErrorResponse -> {
                response.send()
                markRequestChannel.send(
                    MarkRequest(id = response.data.requestId,
                                key = Key(ocid = response.data.ocid,
                                          phase = response.data.phase))
                )
            }

            is ScheduleErrorResponse -> {
                response.send()
                markRequestChannel.send(
                    MarkRequest(id = response.data.requestId,
                                key = Key(ocid = response.data.ocid,
                                          phase = response.data.phase))
                )
            }

            is ReplaceErrorResponse -> {
                response.send()
                markRequestChannel.send(
                    MarkRequest(id = response.data.requestId,
                                key = Key(ocid = response.data.ocid,
                                          phase = response.data.phase))
                )
            }

            is CancelErrorResponse -> {
                response.send()
                markRequestChannel.send(
                    MarkRequest(id = response.data.requestId,
                                key = Key(ocid = response.data.ocid,
                                          phase = response.data.phase))
                )
            }
        }
    }

    private suspend fun ErrorResponse.send() {
        this.toJson()?.let { json ->
            kafkaProducerService.send(json)
        }
    }

    private suspend fun Task.deactivate() {
        log.debug("Sending task to deactivate: $this.")
        deactivateChannel.send(this)
        log.debug("Task was sent for deactivate: $this.")
    }

    private fun <T> T.toJson(): String? = try {
        mapper.writeValueAsString(this)
    } catch (ex: Exception) {
        log.error("Error of convert object to 'JSON'.", ex)
        null
    }
}
