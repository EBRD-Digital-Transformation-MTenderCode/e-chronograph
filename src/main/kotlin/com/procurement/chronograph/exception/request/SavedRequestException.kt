package com.procurement.chronograph.exception.request

class SavedRequestException(request: String,
                            msg: String = "Error of save request '$request' to database.",
                            exception: Throwable? = null) : RuntimeException(msg, exception)
