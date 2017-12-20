package com.ocds.chronograph.model.domain.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.ocds.chronograph.model.domain.request.RequestId
import java.time.LocalDateTime

@JsonPropertyOrder("status")
sealed class Response(@field:JsonProperty("status") val status: Status) {
    enum class Status {
        NOTIFICATION,
        ERROR
    }
}

@JsonPropertyOrder("status", "data")
class NotificationResponse(@field:JsonProperty("data") val data: Data)
    : Response(Status.NOTIFICATION) {
    data class Data(
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String,
        @field:JsonProperty("launchTime") @field:JsonInclude(JsonInclude.Include.NON_NULL) val launchTime: LocalDateTime,
        @field:JsonProperty("metaData") @field:JsonInclude(JsonInclude.Include.NON_NULL) val metaData: String
    )
}

@JsonPropertyOrder("status", "errorCode")
sealed class ErrorResponse(
    @field:JsonProperty("errorCode") val errorCode: ErrorCode
) : Response(Status.ERROR) {
    enum class ErrorCode {
        INVALID_PAYLOAD_OF_REQUEST,
        SAVE_REQUEST,
        EXPIRE_REQUEST,
        SCHEDULE_TASK_ERROR,
        REPLACE_TASK_ERROR,
        CANCEL_TASK_ERROR
    }
}

@JsonPropertyOrder("status", "errorCode", "data")
data class ParseBodyRequestErrorResponse(
    @field:JsonProperty("data") val data: String
) : ErrorResponse(ErrorCode.INVALID_PAYLOAD_OF_REQUEST)

@JsonPropertyOrder("status", "errorCode", "data")
data class SaveBodyRequestErrorResponse(
    @field:JsonProperty("data") val data: Data
) : ErrorResponse(ErrorCode.SAVE_REQUEST) {
    data class Data(
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String,
        @field:JsonProperty("launchTime") @field:JsonInclude(JsonInclude.Include.NON_NULL) val launchTime: LocalDateTime? = null,
        @field:JsonProperty("metaData") @field:JsonInclude(JsonInclude.Include.NON_NULL) val metaData: String? = null,
        @field:JsonProperty("sentTime") val sentTime: LocalDateTime,
        @field:JsonProperty("receivedTime") val receivedTime: LocalDateTime
    )
}

@JsonPropertyOrder("status", "errorCode", "data")
data class ExpireLaunchTimeErrorResponse(
    @field:JsonProperty("data") val data: Data
) : ErrorResponse(ErrorCode.EXPIRE_REQUEST) {
    data class Data(
        @field:JsonProperty("requestId") val requestId: RequestId,
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String,
        @field:JsonProperty("launchTime") @field:JsonInclude(JsonInclude.Include.NON_NULL) val launchTime: LocalDateTime? = null,
        @field:JsonProperty("metaData") @field:JsonInclude(JsonInclude.Include.NON_NULL) val metaData: String? = null,
        @field:JsonProperty("sentTime") val sentTime: LocalDateTime,
        @field:JsonProperty("receivedTime") val receivedTime: LocalDateTime
    )
}

@JsonPropertyOrder("status", "errorCode", "data")
data class ScheduleErrorResponse(
    @field:JsonProperty("data") val data: Data
) : ErrorResponse(ErrorCode.SCHEDULE_TASK_ERROR) {
    data class Data(
        @field:JsonProperty("requestId") val requestId: RequestId,
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String,
        @field:JsonProperty("launchTime") val launchTime: LocalDateTime,
        @field:JsonProperty("metaData") val metaData: String
    )
}

@JsonPropertyOrder("status", "errorCode", "data")
data class ReplaceErrorResponse(
    @field:JsonProperty("data") val data: Data
) : ErrorResponse(ErrorCode.REPLACE_TASK_ERROR) {
    data class Data(
        @field:JsonProperty("requestId") val requestId: RequestId,
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String,
        @field:JsonProperty("newLaunchTime") val newLaunchTime: LocalDateTime,
        @field:JsonProperty("metaData") val metaData: String
    )
}

@JsonPropertyOrder("status", "errorCode", "data")
data class CancelErrorResponse(
    @field:JsonProperty("data") val data: Data
) : ErrorResponse(ErrorCode.CANCEL_TASK_ERROR) {
    data class Data(
        @field:JsonProperty("requestId") val requestId: RequestId,
        @field:JsonProperty("ocid") val ocid: String,
        @field:JsonProperty("phase") val phase: String
    )
}
