package com.ocds.chronograph.model.domain.request

import com.ocds.chronograph.model.domain.Key
import java.time.LocalDateTime

typealias RequestId = Long

sealed class Request {
    abstract val id: RequestId
    abstract val key: Key
}

data class ScheduleRequest(override val id: RequestId,
                           override val key: Key,
                           val launchTime: LocalDateTime,
                           val metaData: String,
                           val sentTime: LocalDateTime,
                           val receivedTime: LocalDateTime)
    : Request()

data class ReplaceRequest(override val id: RequestId,
                          override val key: Key,
                          val newLaunchTime: LocalDateTime,
                          val metaData: String,
                          val sentTime: LocalDateTime,
                          val receivedTime: LocalDateTime)
    : Request()

data class CancelRequest(override val id: RequestId,
                         override val key: Key,
                         val sentTime: LocalDateTime,
                         val receivedTime: LocalDateTime)
    : Request()
