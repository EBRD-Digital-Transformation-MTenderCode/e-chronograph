package com.ocds.chronograph.configuration.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ticker")
data class TickerProperties(
    var process: ProcessProperties = ProcessProperties(),
    var cache: CacheProperties = CacheProperties()
)

data class ProcessProperties(
    var repeatTime: Long = 5000,
    var advanceTime: Long = 2000
)

data class CacheProperties(
    var repeatTime: Long = 1000
)
