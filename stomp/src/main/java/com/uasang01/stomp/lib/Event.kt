package com.uasang01.stomp.lib

/**
 * Created by uasang01 on 14/07/22.
 */

class Event {

    var type: Type? = null
    var exception: Throwable? = null

    constructor(type: Type) {
        this.type = type
    }

    constructor(type: Type, throwable: Throwable) {
        this.type = type
        this.exception = throwable
    }

    enum class Type {
        OPENED,
        CLOSED,
        ERROR
    }
}
