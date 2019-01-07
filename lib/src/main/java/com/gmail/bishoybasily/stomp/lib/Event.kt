package com.gmail.bishoybasily.stomp.lib

/**
 * Created by bishoy on 11/29/17.
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
