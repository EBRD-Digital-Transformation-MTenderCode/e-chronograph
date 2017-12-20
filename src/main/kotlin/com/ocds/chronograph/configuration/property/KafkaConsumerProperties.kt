package com.ocds.chronograph.configuration.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.kafka.consumer")
class KafkaConsumerProperties {
    var topic: String = "default-in-topic"
    var grounId: String = "default-in-group-id"
}
