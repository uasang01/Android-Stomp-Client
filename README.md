# Stomp client for kotlin

[![](https://jitpack.io/v/bishoybasily/stomp.svg)](https://jitpack.io/#bishoybasily/stomp)

## Example 

``` kotlin

lateinit var stompConnection: Disposable
lateinit var topic: Disposable

val url = "ws://example.com/endpoint"
val intervalMillis = 1000L
val client = OkHttpClient()

val stomp = StompClient(url, intervalMillis, client)

// connect
stompConnection = stomp.connect().subscribe {
    when (it.type) {
        Event.Type.OPENED -> {

        }
        Event.Type.CLOSED -> {

        }
        Event.Type.ERROR -> {

        }
    }
}

// subscribe
topic = stomp.subscribe("/destination").subscribe { Log.i(TAG, it) }

// unsubscribe
topic.dispose()

// send
stomp.send("/destination", "dummy message").subscribe {
    if (it) {
    }
}

// disconnect
stompConnection.dispose()
     
```
