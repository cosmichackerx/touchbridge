package com.touchbridge.mobile.data.codec

object ProtocolConstants {
    const val MAGIC = "TBRDG"
    const val VERSION = 1
    const val DISCOVERY_PORT = 47832
    const val CONTROL_PORT = 47831
    const val WS_PATH = "/tb"

    const val OPCODE_MOVE: Byte = 0x01
    const val OPCODE_SCROLL: Byte = 0x02
}

object ProtocolCodec {

    fun discoverRequest(): String =
        """{"magic":"${ProtocolConstants.MAGIC}","t":"discover","v":${ProtocolConstants.VERSION}}"""

    fun hello(device: String, os: String, pin: String?): String {
        val pinPart = if (pin != null) ""","pin":"$pin"""" else ""
        return """{"t":"hello","v":${ProtocolConstants.VERSION},"device":"$device","os":"$os"$pinPart}"""
    }

    fun ping(ts: Long): String = """{"t":"ping","ts":$ts}"""

    fun click(button: String, action: String = "click"): String =
        """{"t":"click","button":"$button","action":"$action"}"""

    fun key(code: String, action: String = "tap"): String =
        """{"t":"key","code":"$code","action":"$action"}"""

    fun chord(mods: List<String>, code: String): String {
        val modArray = mods.joinToString(",") { "\"$it\"" }
        return """{"t":"chord","mods":[$modArray],"code":"$code"}"""
    }

    fun media(key: String): String =
        """{"t":"media","key":"$key"}"""

    fun mode(name: String): String =
        """{"t":"mode","name":"$name"}"""

    fun screen(on: Boolean): String =
        """{"t":"screen","on":$on}"""

    fun text(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"t":"text","value":"$escaped"}"""
    }

    fun moveFrame(dx: Int, dy: Int): ByteArray {
        val buf = ByteArray(5)
        buf[0] = ProtocolConstants.OPCODE_MOVE
        writeInt16(buf, 1, dx)
        writeInt16(buf, 3, dy)
        return buf
    }

    fun scrollFrame(dx: Int, dy: Int): ByteArray {
        val buf = ByteArray(5)
        buf[0] = ProtocolConstants.OPCODE_SCROLL
        writeInt16(buf, 1, dx)
        writeInt16(buf, 3, dy)
        return buf
    }

    private fun writeInt16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
