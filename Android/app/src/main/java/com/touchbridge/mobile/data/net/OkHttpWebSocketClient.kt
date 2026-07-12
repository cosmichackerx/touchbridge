package com.touchbridge.mobile.data.net

import com.touchbridge.mobile.data.codec.ProtocolCodec
import com.touchbridge.mobile.data.codec.ProtocolConstants
import com.touchbridge.mobile.domain.model.ConnectionInfo
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpWebSocketClient @Inject constructor() : ConnectionRepository {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    private val _connectionInfo = MutableStateFlow(ConnectionInfo())
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()

    override suspend fun connect(
        host: String,
        port: Int,
        pin: String?,
        deviceName: String
    ): Result<Unit> {
        _connectionInfo.value = ConnectionInfo(ConnectionState.Connecting)

        return try {
            val url = "ws://$host:$port${ProtocolConstants.WS_PATH}"
            val request = Request.Builder().url(url).build()
            val latch = java.util.concurrent.CountDownLatch(1)
            var connectError: String? = null
            var connected = false

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    val hello = ProtocolCodec.hello(
                        device = deviceName,
                        os = "Android ${android.os.Build.VERSION.RELEASE}",
                        pin = pin
                    )
                    ws.send(hello)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val obj = JSONObject(text)
                        when (obj.getString("t")) {
                            "welcome" -> {
                                connected = true
                                _connectionInfo.value = ConnectionInfo(
                                    ConnectionState.Connected,
                                    desktopName = obj.getString("name")
                                )
                                startHeartbeat(ws)
                                latch.countDown()
                            }
                            "error" -> {
                                connectError = obj.optString("code", "unknown")
                                _connectionInfo.value = ConnectionInfo(
                                    ConnectionState.Error,
                                    errorMessage = connectError
                                )
                                latch.countDown()
                            }
                            "pong" -> { /* heartbeat ok */ }
                        }
                    } catch (_: Exception) { /* ignore */ }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    if (!connected) {
                        connectError = t.message ?: "connection failed"
                        _connectionInfo.value = ConnectionInfo(
                            ConnectionState.Error,
                            errorMessage = connectError
                        )
                        latch.countDown()
                    } else {
                        _connectionInfo.value = ConnectionInfo(ConnectionState.Disconnected)
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _connectionInfo.value = ConnectionInfo(ConnectionState.Disconnected)
                }
            })

            latch.await(10, TimeUnit.SECONDS)

            if (connected) Result.success(Unit)
            else Result.failure(Exception(connectError ?: "timeout"))
        } catch (e: Exception) {
            _connectionInfo.value = ConnectionInfo(ConnectionState.Error, errorMessage = e.message)
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "user disconnect")
        webSocket = null
        _connectionInfo.value = ConnectionInfo(ConnectionState.Disconnected)
    }

    override suspend fun sendEvent(event: InputEvent) {
        val ws = webSocket ?: return
        val json = when (event) {
            is InputEvent.Click -> ProtocolCodec.click(event.button, event.action)
            is InputEvent.Key -> ProtocolCodec.key(event.code, event.action)
            is InputEvent.Text -> ProtocolCodec.text(event.value)
            is InputEvent.Move -> return sendBinaryMove(event.dx, event.dy)
            is InputEvent.Scroll -> return sendBinaryScroll(event.dx, event.dy)
            is InputEvent.Mode -> """{"t":"mode","name":"${event.name}"}"""
        }
        ws.send(json)
    }

    override suspend fun sendBinaryMove(dx: Int, dy: Int) {
        webSocket?.send(okio.ByteString.of(*ProtocolCodec.moveFrame(dx, dy)))
    }

    override suspend fun sendBinaryScroll(dx: Int, dy: Int) {
        webSocket?.send(okio.ByteString.of(*ProtocolCodec.scrollFrame(dx, dy)))
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(1000)
                ws.send(ProtocolCodec.ping(System.currentTimeMillis()))
            }
        }
    }
}
