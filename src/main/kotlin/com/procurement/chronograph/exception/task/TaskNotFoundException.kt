package com.procurement.chronograph.exception.task

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId

class TaskNotFoundException(val requestId: RequestId, val key: Key, message: String)
    : RuntimeException(message)
