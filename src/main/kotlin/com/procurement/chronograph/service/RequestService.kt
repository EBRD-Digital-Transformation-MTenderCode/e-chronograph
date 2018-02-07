package com.procurement.chronograph.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.chronograph.channel.SendCommandRequestChannel
import com.procurement.chronograph.channel.SendErrorChannel
import com.procurement.chronograph.configuration.KafkaConfiguration
import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.*
import com.procurement.chronograph.domain.response.ExpireLaunchTimeErrorResponse
import com.procurement.chronograph.domain.response.ParseBodyRequestErrorResponse
import com.procurement.chronograph.domain.response.SaveBodyRequestErrorResponse
import com.procurement.chronograph.exception.request.SavedRequestException
import com.procurement.chronograph.repository.RequestRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface RequestService {
    fun run()
}

@Service
class RequestServiceImpl @Autowired constructor(
    private val kafkaConfiguration: KafkaConfiguration,
    private val mapper: ObjectMapper,
    private val requestRepository: RequestRepository,
    @Qualifier("commandRequestChannel") private val commandRequestChannel: SendCommandRequestChannel,
    @Qualifier("errorChannel") private val errorChannel: SendErrorChannel
) : RequestService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.info("Starting 'RequestService'...")

        load().forEach { request ->
            log.debug("A request was received for processing: $request.")
            request.send()
            log.debug("A request was sent for processing: $request.")
        }

        KafkaConsumerListener(kafkaConfiguration) { data, _, consumer ->
            parse(data)
                ?.let { message ->
                    message.save()
                        ?.let { requestId ->
                            when (message) {
                                is ScheduleMessage -> message.toRequest(requestId).send()
                                is ReplaceMessage -> message.toRequest(requestId).send()
                                is CancelMessage -> message.toRequest(requestId).send()
                            }
                        }
                }
            consumer?.commitSync()
        }.run()
    }

    override fun run() {
        coroutine.start()
    }

    private fun load(): List<Request> = try {
        requestRepository.load()
    } catch (ex: Exception) {
        log.error("Error of load requests from database.")
        emptyList()
    }

    private suspend fun parse(consumerRecord: ConsumerRecord<String, String>): Message? = try {
        val sentTime: LocalDateTime = consumerRecord.getSentTime()
        val body = mapper.readValue(consumerRecord.value(), MessageBody::class.java)

        when (body) {
            is ScheduleMessageBody -> {
                ScheduleMessage(body = body, sentTime = sentTime)
            }

            is ReplaceMessageBody -> {
                ReplaceMessage(body = body, sentTime = sentTime)
            }

            is CancelMessageBody -> {
                CancelMessage(body = body, sentTime = sentTime)
            }
        }
    } catch (ex: Exception) {
        log.error("Error of convert 'JSON' to request body: ${consumerRecord.value()}", ex)
        errorChannel.send(
            ParseBodyRequestErrorResponse(data = consumerRecord.value())
        )
        null
    }

    private fun ConsumerRecord<String, String>.getSentTime(): LocalDateTime =
        if (this.timestamp() < 0)
            LocalDateTime.now()
        else
            LocalDateTime.ofInstant(Instant.ofEpochSecond(this.timestamp() / 1000), ZoneId.of("UTC"))

    private suspend fun Message.save(): RequestId? = try {
        requestRepository.save(this)
    } catch (ex: Exception) {
        when (ex) {
            is SavedRequestException -> {
                log.error("Error of save 'RequestMessage' to database: $this", ex)
                errorChannel.send(
                    this.getSaveBodyRequestErrorResponse()
                )
            }

            else -> {
                log.error("Error of save 'RequestMessage' to database: $this", ex)
            }
        }
        null
    }

    private fun Message.getSaveBodyRequestErrorResponse(): SaveBodyRequestErrorResponse = SaveBodyRequestErrorResponse(
        data = when (this) {
            is ScheduleMessage -> SaveBodyRequestErrorResponse.Data(
                ocid = this.body.ocid,
                phase = this.body.phase,
                launchTime = this.body.launchTime,
                metaData = this.body.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

            is ReplaceMessage -> SaveBodyRequestErrorResponse.Data(
                ocid = this.body.ocid,
                phase = this.body.phase,
                launchTime = this.body.launchTime,
                metaData = this.body.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

            is CancelMessage -> SaveBodyRequestErrorResponse.Data(
                ocid = this.body.ocid,
                phase = this.body.phase,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

        }
    )

    private fun Message.toRequest(requestId: RequestId) =
        when (this) {
            is ScheduleMessage -> ScheduleRequest(
                id = requestId,
                key = Key(ocid = this.body.ocid,
                          phase = this.body.phase
                ),
                launchTime = this.body.launchTime,
                metaData = this.body.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )
            is ReplaceMessage -> ReplaceRequest(
                id = requestId,
                key = Key(ocid = this.body.ocid,
                          phase = this.body.phase
                ),
                newLaunchTime = this.body.launchTime,
                metaData = this.body.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )
            is CancelMessage -> CancelRequest(
                id = requestId,
                key = Key(ocid = this.body.ocid,
                          phase = this.body.phase
                ),
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )
        }

    private suspend fun Request.send() {
        when (this) {
            is ScheduleRequest -> {
                if (this.sentTime.isBefore(this.launchTime))
                    sendRequest(this)
                else
                    sendError(this)
            }

            is ReplaceRequest -> {
                if (this.sentTime.isBefore(this.newLaunchTime))
                    sendRequest(this)
                else
                    sendError(this)
            }

            is CancelRequest -> sendRequest(this)
        }
    }

    private suspend fun sendRequest(request: Request) {
        log.debug("Sending request to command service ($request).")
        commandRequestChannel.send(request)
        log.debug("A request was sent to command service. ($request)")
    }

    private suspend fun sendError(request: Request) {
        log.debug("Sending error about expiry of launch-time ($request).")
        errorChannel.send(
            request.getExpireLaunchTimeErrorResponse()
        )
        log.debug("A error about expiry of launch-time was sent ($request).")
    }

    private fun Request.getExpireLaunchTimeErrorResponse(): ExpireLaunchTimeErrorResponse = ExpireLaunchTimeErrorResponse(
        data = when (this) {
            is ScheduleRequest -> ExpireLaunchTimeErrorResponse.Data(
                requestId = this.id,
                ocid = this.key.ocid,
                phase = this.key.phase,
                launchTime = this.launchTime,
                metaData = this.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

            is ReplaceRequest -> ExpireLaunchTimeErrorResponse.Data(
                requestId = this.id,
                ocid = this.key.ocid,
                phase = this.key.phase,
                launchTime = this.newLaunchTime,
                metaData = this.metaData,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

            is CancelRequest -> ExpireLaunchTimeErrorResponse.Data(
                requestId = this.id,
                ocid = this.key.ocid,
                phase = this.key.phase,
                sentTime = this.sentTime,
                receivedTime = this.receivedTime
            )

        }
    )
}
