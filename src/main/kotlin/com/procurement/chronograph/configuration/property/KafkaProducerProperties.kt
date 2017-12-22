package com.procurement.chronograph.configuration.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.kafka.producer")
class KafkaProducerProperties {
    var topic: String = "default-out-topic"
}
