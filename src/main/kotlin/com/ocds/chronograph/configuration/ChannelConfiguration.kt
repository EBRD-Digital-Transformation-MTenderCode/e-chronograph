package com.ocds.chronograph.configuration

import com.ocds.chronograph.channel.*
//import com.ocds.chronograph.configuration.property.ChannelProperties
//import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The Java-configuration of Channels.
 */
@Configuration
class ChannelConfiguration {
    @Bean("commandRequestChannel")
    fun commandRequestChannel(): CommandRequestChannel {
        return CommandRequestChannel()
    }

    @Bean("commandChannel")
    fun commandChannel(): CommandChannel {
        return CommandChannel()
    }

    @Bean("cacheChannel")
    fun cacheChannel(): CacheChannel {
        return CacheChannel()
    }

    @Bean("filterChannel")
    fun filterChannel(): FilterChannel {
        return FilterChannel()
    }

    @Bean("notificationChannel")
    fun notificationChannel(): NotificationChannel {
        return NotificationChannel()
    }

    @Bean("errorChannel")
    fun errorChannel(): ErrorChannel {
        return ErrorChannel()
    }

    @Bean("deactivateChannel")
    fun deactivateChannel(): DeactivateChannel {
        return DeactivateChannel()
    }

    @Bean("markRequestChannel")
    fun markRequestChannel(): MarkRequestChannel {
        return MarkRequestChannel()
    }
}
