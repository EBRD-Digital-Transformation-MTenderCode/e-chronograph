package com.procurement.chronograph

import com.procurement.chronograph.configuration.ApplicationConfiguration
import com.procurement.chronograph.service.ServiceRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackageClasses = [ApplicationConfiguration::class],
    exclude = [LiquibaseAutoConfiguration::class, JdbcTemplateAutoConfiguration::class]
)
class ChronographApplication

fun main(args: Array<String>) {
    runApplication<ChronographApplication>(*args)
        .let { ctx ->
            ctx.getBean(ServiceRunner::class.java).run()
        }
}
