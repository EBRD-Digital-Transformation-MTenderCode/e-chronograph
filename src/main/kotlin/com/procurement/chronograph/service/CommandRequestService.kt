package com.procurement.chronograph.service

import com.procurement.chronograph.channel.ReceiveCommandRequestChannel
import com.procurement.chronograph.channel.SendCommandChannel
import com.procurement.chronograph.configuration.property.TickerProperties
import com.procurement.chronograph.domain.ClosedTimeRange
import com.procurement.chronograph.domain.OpenTimeRange
import com.procurement.chronograph.domain.command.CancelTaskCommand
import com.procurement.chronograph.domain.command.LoadTaskCommand
import com.procurement.chronograph.domain.command.ReplaceTaskCommand
import com.procurement.chronograph.domain.command.ScheduleTaskCommand
import com.procurement.chronograph.domain.request.CancelRequest
import com.procurement.chronograph.domain.request.ReplaceRequest
import com.procurement.chronograph.domain.request.Request
import com.procurement.chronograph.domain.request.ScheduleRequest
import com.procurement.chronograph.times.AbstractTicker
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

interface CommandRequestService {
    fun run()
}

@Service
class CommandRequestServiceImpl @Autowired constructor(
    tickerProperties: TickerProperties,
    @Qualifier("commandRequestChannel") private val commandRequestChannel: ReceiveCommandRequestChannel,
    @Qualifier("commandChannel") private val commandChannel: SendCommandChannel
) : CommandRequestService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(CommandRequestService::class.java)
    }

    private val tickerChannel: LinkedListChannel<LocalDateTime> = LinkedListChannel()
    private val timeRangeProcessor: TimeRangeProcessor = TimeRangeProcessor(tickerProperties.process)

    private val ticker: AbstractTicker =
        object : AbstractTicker(repeatTime = Duration.ofMillis(tickerProperties.process.repeatTime)) {
            override val channel: LinkedListChannel<LocalDateTime>
                get() = tickerChannel
            override val log: Logger
                get() = Companion.log
        }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.debug("Starting 'CommandService'...")

        val openTimeRange = timeRangeProcessor.first()
        ticker.start(delayTime = openTimeRange.delayTime)
        openTimeRange.processing()

        while (true) {
            select<Unit> {
                tickerChannel.onReceive { time -> timeRangeProcessor.next(time).processing() }
                commandRequestChannel.onReceive { request -> request.processing() }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }

    private suspend fun OpenTimeRange.processing() {
        log.debug("Sending a command for retrieve from database a set of tasks by specific time: ${this.endExclusive}.")
        commandChannel.send(LoadTaskCommand(timeRange = this))
        log.debug("A command was sent for retrieve from database a set of tasks by specific time: ${this.endExclusive}.")
    }

    private suspend fun ClosedTimeRange.processing() {
        log.debug("Sending a command for retrieve from database a set of tasks by specific time range: ${this.start} - ${this.endExclusive}.")
        commandChannel.send(LoadTaskCommand(timeRange = this))
        log.debug("A command was sent for retrieve from database a set of tasks by specific time range: ${this.start} - ${this.endExclusive}.")
    }

    private suspend fun Request.processing() {
        when (this) {
            is ScheduleRequest -> {
                ScheduleTaskCommand(
                    requestId = this.id,
                    key = this.key,
                    launchTime = this.launchTime,
                    metaData = this.metaData,
                    timeRange = timeRangeProcessor.timeRange
                ).send()
            }

            is ReplaceRequest -> {
                ReplaceTaskCommand(
                    requestId = this.id,
                    key = this.key,
                    newLaunchTime = this.newLaunchTime,
                    metaData = this.metaData,
                    timeRange = timeRangeProcessor.timeRange
                ).send()
            }

            is CancelRequest -> {
                CancelTaskCommand(
                    requestId = this.id,
                    key = this.key,
                    timeRange = timeRangeProcessor.timeRange
                ).send()
            }
        }
    }

    private suspend fun ScheduleTaskCommand.send() {
        log.debug("Sending a command for scheduling a task: $this.")
        commandChannel.send(this)
        log.debug("A command was sent for scheduling a task: $this.")
    }

    private suspend fun ReplaceTaskCommand.send() {
        log.debug("Sending a command for replacing a task: $this.")
        commandChannel.send(this)
        log.debug("A command was sent for replacing a task: $this.")
    }

    private suspend fun CancelTaskCommand.send() {
        log.debug("Sending a command for canceling a task: $this.")
        commandChannel.send(this)
        log.debug("A command was sent for canceling a task: $this.")
    }
}
