package com.procurement.chronograph.channel

import com.procurement.chronograph.domain.command.Command
import com.procurement.chronograph.domain.request.MarkRequest
import com.procurement.chronograph.domain.request.Request
import com.procurement.chronograph.domain.response.ErrorResponse
import com.procurement.chronograph.domain.task.Task
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

interface SendCommandRequestChannel : SendChannel<Request>
interface ReceiveCommandRequestChannel : ReceiveChannel<Request>
class CommandRequestChannel : LinkedListChannel<Request>(),
                              SendCommandRequestChannel,
                              ReceiveCommandRequestChannel

interface SendCommandChannel : SendChannel<Command>
interface ReceiveCommandChannel : ReceiveChannel<Command>
class CommandChannel : LinkedListChannel<Command>(),
                       SendCommandChannel,
                       ReceiveCommandChannel

interface SendCacheChannel : SendChannel<Task>
interface ReceiveCacheChannel : ReceiveChannel<Task>
class CacheChannel : LinkedListChannel<Task>(),
                     SendCacheChannel,
                     ReceiveCacheChannel

interface SendFilterChannel : SendChannel<Set<Task>>
interface ReceiveFilterChannel : ReceiveChannel<Set<Task>>
class FilterChannel : LinkedListChannel<Set<Task>>(),
                      SendFilterChannel,
                      ReceiveFilterChannel

interface SendNotificationChannel : SendChannel<Task>
interface ReceiveNotificationChannel : ReceiveChannel<Task>
class NotificationChannel : LinkedListChannel<Task>(),
                            SendNotificationChannel,
                            ReceiveNotificationChannel

interface SendErrorChannel : SendChannel<ErrorResponse>
interface ReceiveErrorChannel : ReceiveChannel<ErrorResponse>
class ErrorChannel : LinkedListChannel<ErrorResponse>(),
                     SendErrorChannel,
                     ReceiveErrorChannel

interface SendDeactivateChannel : SendChannel<Task>
interface ReceiveDeactivateChannel : ReceiveChannel<Task>
class DeactivateChannel : LinkedListChannel<Task>(),
                          SendDeactivateChannel,
                          ReceiveDeactivateChannel

interface SendMarkRequestChannel : SendChannel<MarkRequest>
interface ReceiveMarkRequestChannel : ReceiveChannel<MarkRequest>
class MarkRequestChannel : LinkedListChannel<MarkRequest>(),
                           SendMarkRequestChannel,
                           ReceiveMarkRequestChannel
