package com.mhss.app.mybrain.data.security

fun String.redactSensitiveHeaders(): String =
    replace(
        Regex("(?im)(authorization|api-key|x-api-key)(\\s*[:=]\\s*)[^\\r\\n]+"),
        "$1$2[REDACTED]"
    )
