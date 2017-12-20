package com.ocds.chronograph.model.domain

import java.time.Duration
import java.time.LocalDateTime

sealed class TimeRange {
    abstract operator fun contains(time: LocalDateTime): Boolean
}

object EmptyTimeRange : TimeRange() {
    override fun contains(time: LocalDateTime): Boolean {
        return false
    }
}

data class OpenTimeRange(val endExclusive: LocalDateTime, val delayTime: Duration) : TimeRange() {
    override operator fun contains(time: LocalDateTime): Boolean {
        return time < endExclusive
    }
}

data class ClosedTimeRange(val start: LocalDateTime, val endExclusive: LocalDateTime) : TimeRange() {
    override operator fun contains(time: LocalDateTime): Boolean {
        return start <= time && time < endExclusive
    }
}
