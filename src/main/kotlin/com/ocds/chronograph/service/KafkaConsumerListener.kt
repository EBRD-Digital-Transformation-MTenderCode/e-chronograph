package com.ocds.chronograph.service

import com.ocds.chronograph.configuration.KafkaConfiguration
import kotlinx.coroutines.experimental.runBlocking
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.config.ContainerProperties
import org.springframework.kafka.support.Acknowledgment

class KafkaConsumerListener(
    private val kafkaConfiguration: KafkaConfiguration,
    private val messageHandler: suspend (data: ConsumerRecord<String, String>,
                                         acknowledgment: Acknowledgment?,
                                         consumer: Consumer<*, *>?) -> Unit
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(KafkaConsumerListener::class.java)
    }

    fun run() {
        val containerProps = ContainerProperties(kafkaConfiguration.kafkaConsumerProperties.topic)
        containerProps.messageListener = AcknowledgingConsumerAwareMessageListener<String, String> { data, acknowledgment, consumer ->
            runBlocking {
                messageHandler(data, acknowledgment, consumer)
            }
        }

        createContainer(containerProps).apply {
            beanName = "ChronographMessageListener"
            start()
        }
    }

    private fun createContainer(
        containerProps: ContainerProperties): KafkaMessageListenerContainer<String, String> {
        val props = consumerProps()
        val cf = DefaultKafkaConsumerFactory<String, String>(props)
        return KafkaMessageListenerContainer(cf, containerProps)
    }

    private fun consumerProps(): Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaConfiguration.kafkaProperties.bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to kafkaConfiguration.kafkaConsumerProperties.grounId,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
    )
}

