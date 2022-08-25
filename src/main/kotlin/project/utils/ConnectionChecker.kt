package project.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.measureTimeMillis


class ConnectionChecker constructor(
    private val client: HttpClient
) {
    suspend fun checkInternetAccess(timeout: Long = 5000): Long {
        return withTimeoutOrNull(timeout) {
            val startTime = System.currentTimeMillis()
            val result = client
                .get("https://google.com/generate_204")
                .status.value
            val delta = System.currentTimeMillis() - startTime
            if (result == 204) {
                delta
            } else {
                error("error $result")
            }
        } ?: error("timeout")

    }
}
