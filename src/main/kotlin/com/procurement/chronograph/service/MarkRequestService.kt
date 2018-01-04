package com.procurement.chronograph.service

import com.procurement.chronograph.channel.ReceiveMarkRequestChannel
import com.procurement.chronograph.domain.request.MarkRequest
import com.procurement.chronograph.exception.RecordNotFound
import com.procurement.chronograph.repository.RequestRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

interface MarkRequestService {
    fun run()
}

@Service
class MarkRequestServiceImpl @Autowired constructor(
    private val requestRepository: RequestRepository,
    @Qualifier("markRequestChannel") private val markRequestChannel: ReceiveMarkRequestChannel
) : MarkRequestService {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DeactivateService::class.java)
    }

    private val coroutine = launch(context = CommonPool, start = CoroutineStart.LAZY) {
        log.info("Starting 'DeactivateService'...")
        while (true) {
            select<Unit> {
                markRequestChannel.onReceive { requestId -> mark(requestId) }
            }
        }
    }

    override fun run() {
        coroutine.start()
    }

    private fun mark(markRequest: MarkRequest) = try {
        requestRepository.markAsUsed(markRequest)
    } catch (ex: Exception) {
        when (ex) {
            is RecordNotFound -> {
                log.error("Request with id: ${ex.requestId} and key: ${ex.key} not found for marking as used.", ex)
            }

            else -> {
                log.error("Error marking request with id: ${markRequest.id} and key: ${markRequest.key} as used.", ex)
            }
        }
    }
}
