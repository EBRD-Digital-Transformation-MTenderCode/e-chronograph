package com.ocds.chronograph.exception.task

import com.ocds.chronograph.model.domain.Key
import com.ocds.chronograph.model.domain.request.RequestId

class CancelTaskException(val requestId: RequestId, val key: Key, exception: Throwable)
    : RuntimeException("Error of cancel task on database (request id: $requestId, key = $key).", exception)
