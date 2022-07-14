package com.uasang01.stomp.lib

data class StompMessage(
    val command: String?,
    val payload: String?,
    val headers: String?
)
