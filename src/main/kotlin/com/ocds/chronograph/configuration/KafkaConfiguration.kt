package com.ocds.chronograph.configuration

import com.ocds.chronograph.configuration.property.KafkaConsumerProperties
import com.ocds.chronograph.configuration.property.KafkaProducerProperties
import com.ocds.chronograph.service.KafkaProducerService
import com.ocds.chronograph.service.KafkaProducerServiceImpl
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * The Java-configuration of Services.
 */
@Configuration
@EnableConfigurationProperties(
    value = [
        KafkaProducerProperties::class,
        KafkaConsumerProperties::class
    ]
)
@EnableKafka
class KafkaConfiguration @Autowired constructor(
    val kafkaProperties: KafkaProperties,
    val kafkaConsumerProperties: KafkaConsumerProperties,
    val kafkaProducerProperties: KafkaProducerProperties
) {
    @Bean
    fun kafkaProducerService(): KafkaProducerService =
        KafkaProducerServiceImpl(kafkaTemplate = kafkaTemplate(), kafkaProducerProperties = kafkaProducerProperties)

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    fun producerConfigs(): Map<String, Any> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
    )
}
