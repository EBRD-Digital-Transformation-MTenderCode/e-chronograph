package com.procurement.chronograph.service

import com.procurement.chronograph.configuration.property.KafkaProducerProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface KafkaProducerService {
    fun send(response: String)
}

@Service
class KafkaProducerServiceImpl @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProducerProperties: KafkaProducerProperties
) : KafkaProducerService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(KafkaProducerServiceImpl::class.java)
    }

    override fun send(response: String) = try {
        run {
            log.debug("Sending response: '$response' to topic: '${kafkaProducerProperties.topic}'")
            kafkaTemplate.send(kafkaProducerProperties.topic, response).get()
            log.debug("Response $response was sent to topic: ${kafkaProducerProperties.topic}")
        }
    } catch (ex: Exception) {
        log.error("Error of send response to channel of Kafka.", ex)
    }
}
