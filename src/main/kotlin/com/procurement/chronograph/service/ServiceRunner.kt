package com.procurement.chronograph.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface ServiceRunner {
    fun run()
}

class ServiceRunnerImpl(
    private val commandRequestService: CommandRequestService,
    private val cacheService: CacheService,
    private val filterService: FilterService,
    private val responseService: ResponseService,
    private val requestService: RequestService,
    private val persistService: PersistService,
    private val deactivateService: DeactivateService,
    private val markRequestService: MarkRequestService
) : ServiceRunner {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ServiceRunner::class.java)
    }

    override fun run() {
        log.info("The start is processing...")
        deactivateService.run()
        markRequestService.run()
        responseService.run()
        filterService.run()
        cacheService.run()
        persistService.run()
        commandRequestService.run()
        requestService.run()
    }
}
