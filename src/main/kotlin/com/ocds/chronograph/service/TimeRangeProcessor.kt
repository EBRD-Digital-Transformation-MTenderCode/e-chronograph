package com.ocds.chronograph.service

import com.ocds.chronograph.configuration.property.ProcessProperties
import com.ocds.chronograph.model.domain.ClosedTimeRange
import com.ocds.chronograph.model.domain.EmptyTimeRange
import com.ocds.chronograph.model.domain.OpenTimeRange
import com.ocds.chronograph.model.domain.TimeRange
import com.ocds.chronograph.times.nowUTC
import java.time.Duration
import java.time.LocalDateTime

class TimeRangeProcessor(private val processProperties: ProcessProperties) {
    private var currentTimeRange: TimeRange = EmptyTimeRange

    val timeRange: TimeRange
        get() = currentTimeRange

    fun first(): OpenTimeRange = when (currentTimeRange) {
        is EmptyTimeRange -> {
            openTimeRange()
                .also { currentTimeRange = it }
        }

        else -> {
            throw IllegalStateException()
        }
    }

    fun next(time: LocalDateTime): ClosedTimeRange = when (currentTimeRange) {
        is OpenTimeRange -> {
            closedTimeRange(currentTimeRange as OpenTimeRange, time)
                .also { currentTimeRange = it }
        }

        is ClosedTimeRange -> {
            closedTimeRange(currentTimeRange as ClosedTimeRange, time)
                .also { currentTimeRange = it }
        }

        else -> {
            throw IllegalStateException()
        }
    }

    private fun openTimeRange(): OpenTimeRange {
        val now = nowUTC()
        val endTime = now.plusSeconds(processProperties.repeatTime)
        val advanceTime = endTime.minusSeconds(processProperties.advanceTime)
        val delayTime = Duration.between(now, advanceTime)
        return OpenTimeRange(endTime, delayTime)
    }

    private fun closedTimeRange(openTimeRange: OpenTimeRange, time: LocalDateTime): ClosedTimeRange {
        val startTime = openTimeRange.endExclusive
        val endTime = time
            .plusSeconds(processProperties.advanceTime)
            .plusSeconds(processProperties.repeatTime)
        return ClosedTimeRange(startTime, endTime)
    }

    private fun closedTimeRange(closedTimeRange: ClosedTimeRange, time: LocalDateTime): ClosedTimeRange {
        val startTime = closedTimeRange.endExclusive
        val endTime = time
            .plusSeconds(processProperties.advanceTime)
            .plusSeconds(processProperties.repeatTime)
        return ClosedTimeRange(startTime, endTime)
    }
}
