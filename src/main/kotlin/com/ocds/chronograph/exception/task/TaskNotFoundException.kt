package com.ocds.chronograph.exception.task

import com.ocds.chronograph.model.domain.Key
import com.ocds.chronograph.model.domain.request.RequestId

class TaskNotFoundException(val requestId: RequestId, val key: Key, message: String)
    : RuntimeException(message)
