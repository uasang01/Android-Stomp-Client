package com.uasang01.stomp.lib

import com.uasang01.stomp.lib.constants.Codes
import com.uasang01.stomp.lib.constants.Commands
import com.uasang01.stomp.lib.constants.Headers
import io.reactivex.ObservableEmitter
import io.reactivex.Observable
import okhttp3.*
import okio.ByteString
import java.io.StringReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

class StompClient(private val okHttpClient: OkHttpClient,
                  private val reconnectAfter: Long = 1000L) :
        WebSocketListener() {

    private val logger = Logger.getLogger(javaClass.name)

    private val DEFAULT_ACK = "auto"
    private val SUPPORTED_VERSIONS = "1.1,1.2"

    private val topics = HashMap<String, String>()
    private val emitters = ConcurrentHashMap<String, ObservableEmitter<String>>()

    private var shouldBeConnected: Boolean = false
    private var connected = false

    private val customHeaders = HashMap<String, String>()

    private lateinit var webSocket: WebSocket

    private lateinit var emitter: ObservableEmitter<Event>

    lateinit var url: String

    fun connect(_headers: HashMap<String, String>? = null): Observable<Event> {
        _headers?.forEach{
            customHeaders[it.key] = it.value
        }
        return Observable
                .create<Event> {
                    emitter = it
                    shouldBeConnected = true
                    open()
                }
                .doOnDispose {
                    close()
                    shouldBeConnected = false
                }
    }

    fun join(topic: String): Observable<String> {
        return Observable
                .create<String> {

                    val topicId = UUID.randomUUID().toString()

                    val headers = HashMap<String, String>()
                    headers[Headers.ID] = topicId
                    headers[Headers.DESTINATION] = topic
                    headers[Headers.ACK] = DEFAULT_ACK
                    webSocket.send(compileMessage(Message(Commands.SUBSCRIBE, headers)))

                    emitters[topic] = it
                    topics[topic] = topicId

                    logger.log(Level.INFO, "Subscribed to: $topic id: $topicId")

                }
                .doOnDispose {

                    val topicId = topics[topic]

                    val headers = HashMap<String, String>()
                    headers[Headers.ID] = topicId!!
                    webSocket.send(compileMessage(Message(Commands.UNSUBSCRIBE, headers)))

                    emitters.remove(topic)
                    topics.remove(topicId)

                    logger.log(Level.INFO, "Unsubscribed from: $topic id: $topicId")

                }
    }

    fun send(topic: String, msg: String): Observable<Boolean> {
        return Observable
                .create<Boolean> {
                    val headers = HashMap<String, String>()
                    headers[Headers.DESTINATION] = topic
                    it.onNext(webSocket.send(compileMessage(Message(Commands.SEND, headers, msg))))
                    it.onComplete()
                }
    }

    fun isConnected() = connected

    private fun open() {
        if (!connected) {
            logger.log(Level.INFO, "Connecting...")
            val request = Request.Builder()
                    .url(url)
                    .build()
            webSocket = okHttpClient.newWebSocket(request, this)
            connected = true
        } else {
            logger.log(Level.INFO, "Already connected")
        }
    }

    private fun reconnect() {
        if (shouldBeConnected) {
            close()
            Thread.sleep(reconnectAfter)
            open()
        }
    }

    private fun close() {
        if (connected) {
            logger.log(Level.INFO, "Disconnecting...")
            webSocket.close(Codes.DEFAULT, "")
            connected = false
        } else {
            logger.log(Level.INFO, "Already disconnected")
        }
    }

    private fun parseMessage(data: String?): Message {

        if (data.isNullOrBlank())
            return Message(Commands.UNKNOWN)

        val reader = Scanner(StringReader(data))
        reader.useDelimiter("\\n")
        val command = reader.next()
        val headers = HashMap<String, String>()

        while (reader.hasNext(Message.PATTERN_HEADER)) {
            val matcher = Message.PATTERN_HEADER.matcher(reader.next())
            matcher.find()
            matcher.group(1)?.let { matcher.group(2)?.let { it1 -> headers.put(it, it1) } }
        }

        reader.skip("\\s")

        reader.useDelimiter(Message.TERMINATE_MESSAGE_SYMBOL)
        val payload = if (reader.hasNext()) reader.next() else null

        return Message(command, headers, payload!!)
    }

    private fun compileMessage(message: Message): String {
        val builder = StringBuilder()

        if (message.command != null)
            builder.append(message.command).append('\n')

        for ((key, value) in message.headers)
            builder.append(key).append(':').append(value).append('\n')
        builder.append('\n')

        if (message.payload != null)
            builder.append(message.payload).append("\n\n")

        builder.append(Message.TERMINATE_MESSAGE_SYMBOL)

        return builder.toString()
    }

    // from WebSocketListener listener

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val headers = HashMap<String, String>()
        headers[Headers.VERSION] = SUPPORTED_VERSIONS
        customHeaders.forEach{
            headers[it.key] = it.value
        }
        this.webSocket.send(compileMessage(Message(Commands.CONNECT, headers)))
        logger.log(Level.INFO, "onOpen")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        emitter.onNext(Event(Event.Type.CLOSED))
        logger.log(Level.INFO, "onClosed reason: $reason, code: $code")
//        reconnect()
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        handleMessage(parseMessage(bytes.toString()))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        handleMessage(parseMessage(text))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(code, reason)
        logger.log(Level.INFO, "onClosing reason: $reason, code: $code")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        emitter.onNext(Event(Event.Type.ERROR, t))
        logger.log(Level.INFO, "onFailure", t)
//        reconnect()
    }

    private fun handleMessage(message: Message) {
        when (message.command) {
            Commands.CONNECTED -> {
                emitter.onNext(Event(Event.Type.OPENED))
            }
            Commands.MESSAGE -> {
                val dest = message.headers[Headers.DESTINATION]
                if (dest != null) {
                    val emitter = emitters[dest]
                    if (emitter != null) {
                        emitter.onNext(message.payload!!)
                    }
                }
            }
            Commands.ERROR -> {
                emitter.onError(Throwable(message = message.headers["message"]))
            }
        }
        logger.log(Level.INFO, "MESSAGE RECEIVED\n{\n\tpayload: ${message.payload},\n\theaaders:${message.headers},\n\tcommand: ${message.command}\n}")
    }
}
