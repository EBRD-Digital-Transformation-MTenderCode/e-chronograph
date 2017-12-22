package com.procurement.chronograph.domain.request

import com.procurement.chronograph.domain.Key

data class MarkRequest(val id: RequestId, val key: Key)
