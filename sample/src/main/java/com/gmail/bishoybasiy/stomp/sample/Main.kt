package com.gmail.bishoybasiy.stomp.sample

import com.gmail.bishoybasily.stomp.lib.Event
import com.gmail.bishoybasily.stomp.lib.StompClient
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

fun main(args: Array<String>) {

    val logger = Logger.getLogger("Main")

    var stompConnection: Disposable
    var topic: Disposable

    val url = "ws://fidelyo-gateway-dev.eu-west-1.elasticbeanstalk.com/media-service/ws?access_token=23d04f0b-6f20-4cd4-bd0f-4c7cea8fe9a9"
    val intervalMillis = 5000L
    val client = OkHttpClient.Builder()
//                .addInterceptor { it.proceed(it.request().newBuilder().header("Authorization", "bearer 68d20faa-54c4-11e8-8195-98ded0151692").build()) }
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

    val stomp = StompClient(client, intervalMillis).apply { this@apply.url = url }

    // connect
    stompConnection = stomp.connect().subscribe {
        when (it.type) {
            Event.Type.OPENED -> {

                // subscribe
                topic = stomp.join("/chains/costa/faces")
                        .subscribe { logger.log(Level.INFO, it) }

//                // unsubscribe
//                topic.dispose()

                // send


//                stomp.send("/app/hello", Base64.getEncoder().encodeToString(File("/home/bishoybasily/Desktop/input.jpg").readBytes())).subscribe {
//                    if (it) {
//                    }
//                }


            }
            Event.Type.CLOSED -> {

            }
            Event.Type.ERROR -> {

            }
        }
    }

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

//    // disconnect
//    stompConnection.dispose()


}
