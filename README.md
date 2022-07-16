# Android-Stomp-Client
Stomp library for Android


## Use


<settings.gradle>
```kotlin
dependencyResolutionManagement {
    ...
    repositories {
        ...       
        maven { url "https://jitpack.io" }
    }
}
```

<build.gradle>
```kotlin
...
dependencies {
    ...
    implementation 'com.github.uasang01:Android-Stomp-Client:$latest_version'
    ...
}
```



## Example 

``` kotlin

lateinit var stompConnection: Disposable
lateinit var topic: Disposable
lateinit var sender: Disposable

private var compositeDisposable = CompositeDisposable()

val url = "ws://example.com/endpoint"
val client = OkHttpClient()

val stomp = StompClient(client)
stomp.url = "$url/websocket"

val headers = HashMap<String, String>()
headers["key"] = "value"


// connect
stompConnection = stomp.connect(headers)
    .subscribe {
        when (it.type) {
            Event.Type.OPENED -> {

            }
            Event.Type.CLOSED -> {

            }
            Event.Type.ERROR -> {
            }
        }
    }
compositeDisposable.add(stompConnection)

// subscribe
topic = stomp.subscribe("/destination").subscribe { Log.i(TAG, it) }

compositeDisposable.add(topic)

// send
sender = stomp.send("/destination", "dummy message").subscribe {
    if (it) {
    }
}
compositeDisposable.add(sender)

// unsubscribe
// disconnect
compositeDisposable.dispose()
     
```
