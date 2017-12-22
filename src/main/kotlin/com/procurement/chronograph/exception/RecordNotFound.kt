package com.procurement.chronograph.exception

import com.procurement.chronograph.domain.Key
import com.procurement.chronograph.domain.request.RequestId

class RecordNotFound(val requestId: RequestId, val key: Key) : RuntimeException()
