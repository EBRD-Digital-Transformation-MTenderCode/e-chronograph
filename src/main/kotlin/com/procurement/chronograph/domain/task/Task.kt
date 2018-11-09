package com.procurement.chronograph.domain.task

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId
import java.time.LocalDateTime

class Task(
    val requestId: RequestId,
    val key: Key,
    val launchTime: LocalDateTime,
    val metaData: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Task

        if (key.ocid != other.key.ocid) return false
        if (key.phase != other.key.phase) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.ocid.hashCode()
        result = 31 * result + key.phase.hashCode()
        return result
    }

    override fun toString(): String {
        return "Task(key='$key', newLaunchTime=$launchTime, metaData='$metaData')"
    }
}
