package com.touchbridge.mobile.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption for the control channel, mirroring the desktop [SecureChannel].
 *
 * A 256-bit key is derived from the shared password with PBKDF2-HMAC-SHA256; every control
 * frame is sealed with AES-256-GCM so the ngrok relay only ever sees ciphertext.
 *
 * Envelope layout (binary WebSocket message):
 *   [0]      kind byte (0x01 = JSON/text payload, 0x02 = raw binary opcode frame)
 *   [1..13)  12-byte random nonce
 *   [13..]   ciphertext followed by the 16-byte GCM tag
 * The kind byte is bound as additional authenticated data.
 */
object SecureChannel {
    const val SALT_SIZE = 16
    const val NONCE_SIZE = 12
    const val TAG_BITS = 128
    const val TAG_SIZE = 16
    const val KEY_BITS = 256
    const val ITERATIONS = 100_000

    const val KIND_TEXT: Byte = 0x01
    const val KIND_BINARY: Byte = 0x02
    const val KIND_SCREEN: Byte = 0x03

    private val AUTH_PLAINTEXT = "TB-AUTH-v1".toByteArray(Charsets.US_ASCII)
    private val AUTH_AAD = "tb-auth".toByteArray(Charsets.US_ASCII)

    private val random = SecureRandom()

    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return factory.generateSecret(spec).encoded
    }

    fun randomBytes(length: Int): ByteArray = ByteArray(length).also { random.nextBytes(it) }

    /** Seals a payload into an encrypted envelope: kind || nonce || (ciphertext+tag). */
    fun seal(key: ByteArray, kind: Byte, plaintext: ByteArray): ByteArray {
        val nonce = randomBytes(NONCE_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(byteArrayOf(kind))
        val cipherTag = cipher.doFinal(plaintext)

        return ByteArray(1 + NONCE_SIZE + cipherTag.size).also { out ->
            out[0] = kind
            System.arraycopy(nonce, 0, out, 1, NONCE_SIZE)
            System.arraycopy(cipherTag, 0, out, 1 + NONCE_SIZE, cipherTag.size)
        }
    }

    /** Opens an encrypted envelope, returning (kind, plaintext) or null on failure. */
    fun open(key: ByteArray, envelope: ByteArray): Pair<Byte, ByteArray>? {
        if (envelope.size < 1 + NONCE_SIZE + TAG_SIZE) return null
        return try {
            val kind = envelope[0]
            val nonce = envelope.copyOfRange(1, 1 + NONCE_SIZE)
            val cipherTag = envelope.copyOfRange(1 + NONCE_SIZE, envelope.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(byteArrayOf(kind))
            kind to cipher.doFinal(cipherTag)
        } catch (e: Exception) {
            null
        }
    }

    /** Builds the client auth proof: (nonce, ciphertext+tag) of a known token. */
    fun makeAuthProof(key: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = randomBytes(NONCE_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(AUTH_AAD)
        val cipherTag = cipher.doFinal(AUTH_PLAINTEXT)
        return nonce to cipherTag
    }
}
