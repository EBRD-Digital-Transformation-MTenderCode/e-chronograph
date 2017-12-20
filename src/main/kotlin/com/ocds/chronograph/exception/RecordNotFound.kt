package com.ocds.chronograph.exception

import com.ocds.chronograph.model.domain.Key
import com.ocds.chronograph.model.domain.request.RequestId

class RecordNotFound(val requestId: RequestId, val key: Key) : RuntimeException()
