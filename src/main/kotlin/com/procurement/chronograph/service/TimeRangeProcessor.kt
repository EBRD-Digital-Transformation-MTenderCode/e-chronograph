package com.procurement.chronograph.service

import com.procurement.chronograph.configuration.property.ProcessProperties
import com.procurement.chronograph.domain.ClosedTimeRange
import com.procurement.chronograph.domain.EmptyTimeRange
import com.procurement.chronograph.domain.OpenTimeRange
import com.procurement.chronograph.domain.TimeRange
import com.procurement.chronograph.times.nowUTC
import java.time.Duration
import java.time.LocalDateTime

class TimeRangeProcessor(private val processProperties: ProcessProperties) {
    private var currentTimeRange: TimeRange = EmptyTimeRange

    val timeRange: TimeRange
        get() = currentTimeRange

    fun first(): OpenTimeRange = when (currentTimeRange) {
        is EmptyTimeRange -> openTimeRange().also { currentTimeRange = it }
        else -> throw IllegalStateException()
    }

    fun next(time: LocalDateTime): ClosedTimeRange = when (currentTimeRange) {
        is OpenTimeRange -> closedTimeRange(currentTimeRange as OpenTimeRange, time).also { currentTimeRange = it }
        is ClosedTimeRange -> closedTimeRange(currentTimeRange as ClosedTimeRange, time).also { currentTimeRange = it }
        else -> throw IllegalStateException()
    }

    private fun openTimeRange(): OpenTimeRange {
        val now = nowUTC()
        val endTime = now.plus(Duration.ofMillis(processProperties.repeatTime))
        val advanceTime = endTime.minus(Duration.ofMillis(processProperties.advanceTime))
        val delayTime = Duration.between(now, advanceTime)
        return OpenTimeRange(endTime, delayTime)
    }

    private fun closedTimeRange(openTimeRange: OpenTimeRange, time: LocalDateTime): ClosedTimeRange {
        val startTime = openTimeRange.endExclusive
        val endTime = time
            .plus(Duration.ofMillis(processProperties.advanceTime))
            .plus(Duration.ofMillis(processProperties.repeatTime))
        return ClosedTimeRange(startTime, endTime)
    }

    private fun closedTimeRange(closedTimeRange: ClosedTimeRange, time: LocalDateTime): ClosedTimeRange {
        val startTime = closedTimeRange.endExclusive
        val endTime = time
            .plus(Duration.ofMillis(processProperties.advanceTime))
            .plus(Duration.ofMillis(processProperties.repeatTime))
        return ClosedTimeRange(startTime, endTime)
    }
}
