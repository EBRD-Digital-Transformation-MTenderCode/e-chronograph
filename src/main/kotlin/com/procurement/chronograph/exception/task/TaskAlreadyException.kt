package com.procurement.chronograph.exception.task

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId

class TaskAlreadyException(val requestId: RequestId, val key: Key, exception: Throwable)
    : RuntimeException("Task (request id: $requestId, key: $key) is already present.", exception)
