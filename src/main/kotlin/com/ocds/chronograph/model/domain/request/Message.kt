package com.ocds.chronograph.model.domain.request

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ocds.chronograph.times.nowUTC
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

sealed class Message {
    abstract val sentTime: LocalDateTime
    abstract val receivedTime: LocalDateTime
}

data class ScheduleMessage(
    @field:NotNull @field:JsonProperty("body") val body: ScheduleMessageBody,
    @field:NotNull @field:JsonProperty("sentTime") override val sentTime: LocalDateTime,
    @field:NotNull @field:JsonProperty("receivedTime") override val receivedTime: LocalDateTime = nowUTC()
) : Message()

data class ReplaceMessage(
    @field:NotNull @field:JsonProperty("body") val body: ReplaceMessageBody,
    @field:NotNull @field:JsonProperty("sentTime") override val sentTime: LocalDateTime,
    @field:NotNull @field:JsonProperty("receivedTime") override val receivedTime: LocalDateTime = nowUTC()
) : Message()

data class CancelMessage(
    @field:NotNull @field:JsonProperty("body") val body: CancelMessageBody,
    @field:NotNull @field:JsonProperty("sentTime") override val sentTime: LocalDateTime,
    @field:NotNull @field:JsonProperty("receivedTime") override val receivedTime: LocalDateTime = nowUTC()
) : Message()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "action")
@JsonSubTypes(
    JsonSubTypes.Type(value = ScheduleMessageBody::class, name = "schedule"),
    JsonSubTypes.Type(value = ReplaceMessageBody::class, name = "cancel"),
    JsonSubTypes.Type(value = CancelMessageBody::class, name = "replace")
)
sealed class MessageBody

data class ScheduleMessageBody @JsonCreator constructor(
    @field:NotNull @field:JsonProperty("ocid") @param:JsonProperty("ocid") val ocid: String,
    @field:NotNull @field:JsonProperty("phase") @param:JsonProperty("phase") val phase: String,
    @field:NotNull @field:JsonProperty("newLaunchTime") @param:JsonProperty("newLaunchTime") val launchTime: LocalDateTime,
    @field:NotNull @field:JsonProperty("metaData") @param:JsonProperty("metaData") val metaData: String
) : MessageBody()

data class ReplaceMessageBody @JsonCreator constructor(
    @field:NotNull @field:JsonProperty("ocid") @param:JsonProperty("ocid") val ocid: String,
    @field:NotNull @field:JsonProperty("phase") @param:JsonProperty("phase") val phase: String,
    @field:NotNull @field:JsonProperty("newLaunchTime") @param:JsonProperty("newLaunchTime") val launchTime: LocalDateTime,
    @field:NotNull @field:JsonProperty("metaData") @param:JsonProperty("metaData") val metaData: String
) : MessageBody()

data class CancelMessageBody @JsonCreator constructor(
    @field:NotNull @field:JsonProperty("ocid") @param:JsonProperty("ocid") val ocid: String,
    @field:NotNull @field:JsonProperty("phase") @param:JsonProperty("phase") val phase: String
) : MessageBody()
