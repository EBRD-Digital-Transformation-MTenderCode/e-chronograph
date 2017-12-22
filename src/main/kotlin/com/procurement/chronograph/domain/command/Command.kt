package com.procurement.chronograph.domain.command

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.TimeRange
import com.procurement.chronograph.domain.request.RequestId
import java.time.LocalDateTime

sealed class Command {
    abstract val timeRange: TimeRange
}

data class ScheduleTaskCommand(
    val requestId: RequestId,
    val key: Key,
    val launchTime: LocalDateTime,
    val metaData: String,
    override val timeRange: TimeRange
) : Command()

data class ReplaceTaskCommand(
    val requestId: RequestId,
    val key: Key,
    val newLaunchTime: LocalDateTime,
    val metaData: String,
    override val timeRange: TimeRange
) : Command()

data class CancelTaskCommand(
    val requestId: RequestId,
    val key: Key,
    override val timeRange: TimeRange
) : Command()

data class LoadTaskCommand(
    override val timeRange: TimeRange
) : Command()
