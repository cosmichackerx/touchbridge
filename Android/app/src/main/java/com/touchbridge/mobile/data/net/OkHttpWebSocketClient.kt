package com.touchbridge.mobile.data.net

import com.touchbridge.mobile.data.codec.ProtocolCodec
import com.touchbridge.mobile.data.crypto.SecureChannel
import com.touchbridge.mobile.domain.model.ConnectionInfo
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.ControlMode
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.model.KeyboardThemeId
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import com.touchbridge.mobile.util.AppLogger
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpWebSocketClient @Inject constructor() : ConnectionRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    private var password: String = ""

    @Volatile
    private var sessionKey: ByteArray? = null

    private val _connectionInfo = MutableStateFlow(ConnectionInfo())
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()

    private val _incomingMode = MutableStateFlow(ControlMode.Trackpad)
    override val incomingMode: StateFlow<ControlMode> = _incomingMode.asStateFlow()

    private val _incomingKeyboardTheme = MutableStateFlow(KeyboardThemeId.Dark)
    override val incomingKeyboardTheme: StateFlow<KeyboardThemeId> = _incomingKeyboardTheme.asStateFlow()

    private val _screenFrame = MutableStateFlow<ByteArray?>(null)
    override val screenFrame: StateFlow<ByteArray?> = _screenFrame.asStateFlow()

    override suspend fun connect(
        webSocketUrl: String,
        pin: String?,
        deviceName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        _connectionInfo.value = ConnectionInfo(ConnectionState.Connecting)
        AppLogger.i("WebSocket", "Connecting to $webSocketUrl")

        password = pin ?: ""
        sessionKey = null
        webSocket?.cancel()
        webSocket = null

        try {
            val requestBuilder = Request.Builder().url(webSocketUrl)
            if (webSocketUrl.contains("ngrok", ignoreCase = true)) {
                requestBuilder.addHeader("ngrok-skip-browser-warning", "true")
            }
            val request = requestBuilder.build()
            val latch = java.util.concurrent.CountDownLatch(1)
            var connectError: String? = null
            var connected = false

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    AppLogger.d("WebSocket", "Socket open, sending hello")
                    // Password is never sent; it only authenticates via the encrypted proof.
                    val hello = ProtocolCodec.hello(
                        device = deviceName,
                        os = "Android ${android.os.Build.VERSION.RELEASE}",
                        pin = null
                    )
                    ws.send(hello)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val obj = JSONObject(text)
                        when (obj.getString("t")) {
                            "challenge" -> {
                                val salt = Base64.getDecoder().decode(obj.getString("salt"))
                                val key = SecureChannel.deriveKey(password, salt)
                                sessionKey = key
                                val (nonce, proof) = SecureChannel.makeAuthProof(key)
                                val auth = JSONObject()
                                    .put("t", "auth")
                                    .put("nonce", Base64.getEncoder().encodeToString(nonce))
                                    .put("proof", Base64.getEncoder().encodeToString(proof))
                                    .toString()
                                AppLogger.d("WebSocket", "Got challenge, sending auth proof")
                                ws.send(auth)
                            }
                            "welcome" -> {
                                connected = true
                                val name = obj.getString("name")
                                AppLogger.i("WebSocket", "Welcome from desktop: $name (encrypted)")
                                _connectionInfo.value = ConnectionInfo(
                                    ConnectionState.Connected,
                                    desktopName = name
                                )
                                startHeartbeat(ws)
                                latch.countDown()
                            }
                            "error" -> {
                                connectError = obj.optString("code", "unknown")
                                AppLogger.e("WebSocket", "Server error: $connectError")
                                _connectionInfo.value = ConnectionInfo(
                                    ConnectionState.Error,
                                    errorMessage = connectError
                                )
                                latch.countDown()
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("WebSocket", "Failed to parse message: $text", e)
                    }
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    val key = sessionKey ?: return
                    val opened = SecureChannel.open(key, bytes.toByteArray()) ?: run {
                        AppLogger.w("WebSocket", "Dropped undecryptable frame")
                        return
                    }
                    val (kind, plain) = opened
                    if (kind == SecureChannel.KIND_SCREEN) {
                        _screenFrame.value = plain
                        return
                    }
                    if (kind != SecureChannel.KIND_TEXT) return
                    try {
                        val obj = JSONObject(String(plain, Charsets.UTF_8))
                        when (obj.getString("t")) {
                            "pong" -> { /* heartbeat ok */ }
                            "mode" -> {
                                val mode = ControlMode.fromWire(obj.optString("name"))
                                AppLogger.i("WebSocket", "Desktop switched mode: $mode")
                                _incomingMode.value = mode
                            }
                            "kbtheme" -> {
                                val theme = KeyboardThemeId.fromWire(obj.optString("name"))
                                AppLogger.i("WebSocket", "Desktop set keyboard theme: $theme")
                                _incomingKeyboardTheme.value = theme
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("WebSocket", "Failed to parse encrypted frame", e)
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    AppLogger.e("WebSocket", "Connection failure", t)
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
                    AppLogger.i("WebSocket", "Closed code=$code reason=$reason")
                    _connectionInfo.value = ConnectionInfo(ConnectionState.Disconnected)
                }
            })

            // Must not block the UI thread — this is why we wrap the whole connect in IO.
            latch.await(12, TimeUnit.SECONDS)

            if (connected) Result.success(Unit)
            else {
                webSocket?.cancel()
                webSocket = null
                val err = connectError ?: "Timed out connecting to $webSocketUrl"
                AppLogger.e("WebSocket", err)
                _connectionInfo.value = ConnectionInfo(
                    ConnectionState.Error,
                    errorMessage = err
                )
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            AppLogger.e("WebSocket", "Connect threw", e)
            _connectionInfo.value = ConnectionInfo(
                ConnectionState.Error,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        AppLogger.i("WebSocket", "Disconnecting")
        heartbeatJob?.cancel()
        webSocket?.close(1000, "user disconnect")
        webSocket = null
        sessionKey = null
        _screenFrame.value = null
        _connectionInfo.value = ConnectionInfo(ConnectionState.Disconnected)
    }

    override suspend fun setScreenStream(on: Boolean) {
        AppLogger.i("WebSocket", "Screen mirroring -> $on")
        if (!on) _screenFrame.value = null
        sealText(ProtocolCodec.screen(on))
    }

    override suspend fun sendEvent(event: InputEvent) {
        val json = when (event) {
            is InputEvent.Click -> ProtocolCodec.click(event.button, event.action)
            is InputEvent.Key -> ProtocolCodec.key(event.code, event.action)
            is InputEvent.Chord -> ProtocolCodec.chord(event.mods, event.code)
            is InputEvent.Text -> ProtocolCodec.text(event.value)
            is InputEvent.Media -> ProtocolCodec.media(event.key)
            is InputEvent.Mode -> ProtocolCodec.mode(event.name)
            is InputEvent.Move -> return sendBinaryMove(event.dx, event.dy)
            is InputEvent.Scroll -> return sendBinaryScroll(event.dx, event.dy)
        }
        sealText(json)
    }

    override suspend fun sendBinaryMove(dx: Int, dy: Int) {
        sealAndSend(SecureChannel.KIND_BINARY, ProtocolCodec.moveFrame(dx, dy))
    }

    override suspend fun sendBinaryScroll(dx: Int, dy: Int) {
        sealAndSend(SecureChannel.KIND_BINARY, ProtocolCodec.scrollFrame(dx, dy))
    }

    private fun sealText(json: String) {
        sealAndSend(SecureChannel.KIND_TEXT, json.toByteArray(Charsets.UTF_8))
    }

    private fun sealAndSend(kind: Byte, payload: ByteArray) {
        val key = sessionKey ?: return
        val ws = webSocket ?: return
        val envelope = SecureChannel.seal(key, kind, payload)
        ws.send(envelope.toByteString())
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(1000)
                sealText(ProtocolCodec.ping(System.currentTimeMillis()))
            }
        }
    }
}
