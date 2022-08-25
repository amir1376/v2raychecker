package project.utils

import java.util.Base64


object Base64Util {
    fun encode(data: ByteArray): String {
        return String(Base64.getEncoder().encode(data))
    }

    fun encode(data: String): String {
        return String(Base64.getEncoder().encode(data.toByteArray()))
    }

    fun decode(data: String): ByteArray {
        return try {
            Base64.getDecoder().decode(data)
        } catch (e: Exception) {
            throw e
        }
    }
}