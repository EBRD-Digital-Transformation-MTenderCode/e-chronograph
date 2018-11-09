package com.procurement.chronograph.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.chronograph.configuration.property.TickerProperties
import com.procurement.chronograph.repository.RequestRepository
import com.procurement.chronograph.repository.TaskRepository
import com.procurement.chronograph.service.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * The Java-configuration of Services.
 */
@Configuration
@Import(
    value = [
        ChannelConfiguration::class,
        KafkaConfiguration::class
    ]
)
@EnableConfigurationProperties(
    value = [
        TickerProperties::class
    ]
)
class ServiceConfiguration @Autowired constructor(
    val mapper: ObjectMapper,
    val tickerProperties: TickerProperties,
    val kafkaConfiguration: KafkaConfiguration,
    val channelConfiguration: ChannelConfiguration,
    val taskRepository: TaskRepository,
    val requestRepository: RequestRepository) {

    @Bean
    fun commandRequestService(): CommandRequestService {
        return CommandRequestServiceImpl(tickerProperties = tickerProperties,
                                         commandRequestChannel = channelConfiguration.commandRequestChannel(),
                                         commandChannel = channelConfiguration.commandChannel()
        )
    }

    @Bean
    fun cacheService(): CacheService {
        return CacheServiceImpl(tickerProperties = tickerProperties,
                                cacheChannel = channelConfiguration.cacheChannel(),
                                filterChannel = channelConfiguration.filterChannel()
        )
    }

    @Bean
    fun filterService(): FilterService {
        return FilterServiceImpl(taskRepository = taskRepository,
                                 filterChannel = channelConfiguration.filterChannel(),
                                 notificationChannel = channelConfiguration.notificationChannel()
        )
    }

    @Bean
    fun responseService(): ResponseService {
        return ResponseServiceImpl(mapper = mapper,
                                   kafkaProducerService = kafkaConfiguration.kafkaProducerService(),
                                   notificationChannel = channelConfiguration.notificationChannel(),
                                   deactivateChannel = channelConfiguration.deactivateChannel(),
                                   errorChannel = channelConfiguration.errorChannel(),
                                   markRequestChannel = channelConfiguration.markRequestChannel()

        )
    }

    @Bean
    fun requestService(): RequestService {
        return RequestServiceImpl(kafkaConfiguration = kafkaConfiguration,
                                  mapper = mapper,
                                  requestRepository = requestRepository,
                                  errorChannel = channelConfiguration.errorChannel(),
                                  commandRequestChannel = channelConfiguration.commandRequestChannel()
        )
    }

    @Bean
    fun persistService(): PersistService {
        return PersistServiceImpl(taskRepository = taskRepository,
                                  commandChannel = channelConfiguration.commandChannel(),
                                  cacheChannel = channelConfiguration.cacheChannel(),
                                  errorChannel = channelConfiguration.errorChannel()

        )
    }

    @Bean
    fun deactivateService(): DeactivateService {
        return DeactivateServiceImpl(taskRepository = taskRepository,
                                     deactivateChannel = channelConfiguration.deactivateChannel()
        )
    }

    @Bean
    fun markRequestService(): MarkRequestService {
        return MarkRequestServiceImpl(requestRepository = requestRepository,
                                      markRequestChannel = channelConfiguration.markRequestChannel()
        )
    }

    @Bean
    fun serviceRunner(): ServiceRunner {
        return ServiceRunnerImpl(
            commandRequestService = commandRequestService(),
            cacheService = cacheService(),
            filterService = filterService(),
            responseService = responseService(),
            requestService = requestService(),
            persistService = persistService(),
            deactivateService = deactivateService(),
            markRequestService = markRequestService()
        )
    }
}
