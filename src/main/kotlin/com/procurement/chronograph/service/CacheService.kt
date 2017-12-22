package com.procurement.chronograph.service

import com.procurement.chronograph.cache.CacheTask
import com.procurement.chronograph.channel.ReceiveCacheChannel
import com.procurement.chronograph.channel.SendFilterChannel
import com.procurement.chronograph.configuration.property.TickerProperties
import com.procurement.chronograph.domain.task.Task
import com.procurement.chronograph.times.AbstractTicker
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

interface CacheService {
    fun run()
}

@Service
class CacheServiceImpl @Autowired constructor(
    tickerProperties: TickerProperties,
    @Qualifier("cacheChannel") private val cacheChannel: ReceiveCacheChannel,
    @Qualifier("filterChannel") private val filterChannel: SendFilterChannel
) : CacheService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(CacheService::class.java)
    }

    private val cache = CacheTask()

    private val tickerChannel = LinkedListChannel<LocalDateTime>()

    private val ticker: AbstractTicker =
        object : AbstractTicker(repeatTime = Duration.ofMillis(tickerProperties.cache.repeatTime)) {
            override val channel: LinkedListChannel<LocalDateTime>
                get() = tickerChannel
            override val log: Logger
                get() = Companion.log
        }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.info("Starting 'CacheService'...")
        while (true) {
            select<Unit> {
                cacheChannel.onReceive { it.caching() }
                tickerChannel.onReceive { it.polling().processing() }
            }
        }
    }

    override fun run() {
        coroutine.start()
        ticker.start()
    }

    private fun Task.caching() {
        log.debug("Received a task from queue for save to cache: $this.")
        cache.push(this)
        log.debug("A task was append in cache: $this.")
    }

    private fun LocalDateTime.polling(): Collection<Set<Task>> {
        log.debug("Retrieving a set of tasks from cache for processing by a specific time: $this.")
        val tasks = cache.poll(this)
        log.debug("Received a set of tasks from cache for processing: $tasks by a specific time: $this.")
        return tasks
    }

    private suspend fun Collection<Set<Task>>.processing() {
        this.forEach { tasks ->
            filterChannel.send(tasks)
            log.debug("A set of tasks: $tasks was sent to filtering.")
        }
    }
}
