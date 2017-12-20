package com.ocds.chronograph.exception.task

import com.ocds.chronograph.model.domain.Key
import com.ocds.chronograph.model.domain.request.RequestId

class TaskAlreadyException(val requestId: RequestId, val key: Key, exception: Throwable)
    : RuntimeException("Task (request id: $requestId, key: $key) is already present.", exception)
