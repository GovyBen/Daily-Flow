package com.mhss.app.tracking.domain.id

fun interface TrackingIdGenerator {
    fun newId(): String
}
