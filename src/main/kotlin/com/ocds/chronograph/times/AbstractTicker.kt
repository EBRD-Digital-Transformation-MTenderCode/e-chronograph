package com.ocds.chronograph.times

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.time.delay
import org.slf4j.Logger
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import kotlin.coroutines.experimental.CoroutineContext

abstract class AbstractTicker(
    private val repeatTime: Duration,
    private val truncate: TemporalUnit = ChronoUnit.SECONDS,
    private val context: CoroutineContext = CommonPool) {

    abstract val log: Logger
    abstract val channel: LinkedListChannel<LocalDateTime>

    fun start(delayTime: Duration = Duration.ZERO) {
        launch(context = context) {
            delay(delayTime)
            while (true) {
                val time = tick()
                channel.send(time)
                log.debug("Ticking: $time.")
                delay(repeatTime)
            }
        }
    }

    private fun tick() = nowUTC(truncate)
}
