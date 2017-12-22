package com.procurement.chronograph.times

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

fun nowUTC(truncate: TemporalUnit = ChronoUnit.SECONDS): LocalDateTime =
    LocalDateTime.now(ZoneOffset.UTC).truncatedTo(truncate)
