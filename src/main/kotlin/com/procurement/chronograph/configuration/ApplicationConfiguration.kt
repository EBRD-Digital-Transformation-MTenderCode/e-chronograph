package com.procurement.chronograph.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * The Java-configuration of application.
 */
@Configuration
@Import(
    value = [
        DatabaseConfiguration::class,
        ChannelConfiguration::class,
        ServiceConfiguration::class,
        KafkaConfiguration::class
    ]
)
class ApplicationConfiguration
