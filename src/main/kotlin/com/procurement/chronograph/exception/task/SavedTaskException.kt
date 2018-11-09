package com.procurement.chronograph.exception.task

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId

class SavedTaskException(val requestId: RequestId, val key: Key, exception: Throwable)
    : RuntimeException("Error of save task to database (request id: $requestId, key: $key)", exception)
