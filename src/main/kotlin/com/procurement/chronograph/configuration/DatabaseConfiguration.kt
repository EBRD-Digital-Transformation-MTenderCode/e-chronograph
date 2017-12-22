package com.procurement.chronograph.configuration

import com.procurement.chronograph.repository.RequestRepository
import com.procurement.chronograph.repository.RequestRepositoryImpl
import com.procurement.chronograph.repository.TaskRepository
import com.procurement.chronograph.repository.TaskRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 * The Java-configuration of Database.
 */
@Configuration
@EnableTransactionManagement
class DatabaseConfiguration @Autowired constructor(
    val dataSource: DataSource
) {
    @Bean
    fun jdbcTemplate() = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun taskRepository(): TaskRepository = TaskRepositoryImpl(
        jdbcTemplate = jdbcTemplate())

    @Bean
    fun requestRepository(): RequestRepository = RequestRepositoryImpl(
        jdbcTemplate = jdbcTemplate())
}
