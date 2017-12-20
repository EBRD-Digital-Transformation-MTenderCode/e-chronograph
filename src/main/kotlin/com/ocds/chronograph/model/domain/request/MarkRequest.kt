package com.ocds.chronograph.model.domain.request

import com.ocds.chronograph.model.domain.Key

data class MarkRequest(val id: RequestId, val key: Key)
