package com.procurement.chronograph.exception.task

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId

class DeactivateTaskException(val requestId: RequestId, val key: Key, exception: Throwable)
    : RuntimeException("Error of deactivate task on database (request id: $requestId, key = $key).", exception)
