package project

import arrow.core.Either
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import project.extra.v2ray.dto.V2rayConfig
import project.utils.ConnectionChecker
import project.utils.OpenPortFinder
import project.utils.Resources
import project.utils.fromJson
import java.io.Closeable
import java.io.File

class V2RayConfigEvaluator(
    outputBean: V2rayConfig.OutboundBean,
) : Closeable {

    private val config = buildServerConfig(outputBean)

    private lateinit var exec: Process

    private var _port: Int? = null
    private val port get() = _port!!

    suspend fun connect() {
        _port = requireNotNull(OpenPortFinder.findOpenPort()) {
            "wtf port is null"
        }
        withContext(Dispatchers.IO) {

            exec = Runtime.getRuntime().exec(getV2RayExecutablePath())
            exec.outputStream.bufferedWriter().use {
                it.write(getTestConfigString())
            }
//            exec.inputStream.bufferedReader().use {
//                while (true){
//                    val read = it.read()
//                    if (read==-1)break
//                    System.out.write(read)
//                }
//            }
        }
        delay(500)
    }

    suspend fun test(): Either<Throwable, Long> {
        connect()
        val client = HttpClient() {
            engine {
                proxy = ProxyBuilder.http("http://localhost:$port")
            }
        }
        return Either.catch {
            ConnectionChecker(client).checkInternetAccess()
        }
    }


    private fun getTestConfigString(): String {
        return configWithModifiedPort(port)
            .toPrettyPrinting()
    }

    private fun buildServerConfig(config: V2rayConfig.OutboundBean): V2rayConfig {
        val v2rayConfig: V2rayConfig = getServerConfig()
        v2rayConfig.outbounds[0] = config
        return v2rayConfig
    }


    private fun configWithModifiedPort(httpPort: Int): V2rayConfig {
        val fullConfig = config.copy(
            inbounds = ArrayList<V2rayConfig.InboundBean>().apply {
                add(
                    V2rayConfig.InboundBean(
                        tag = "http",
                        port = httpPort,
                        protocol = "http",
                        listen = "127.0.0.1",
                        sniffing = null,
                    )
                )
            }
        )
        return fullConfig
    }

    override fun close() {
        try {
            this.exec.destroy()
        } catch (_: Exception) {
        }
    }
}

private val gson = Gson()
private val blueprintConfig by lazy {
    gson.fromJson<V2rayConfig>(Resources["v2ray_config.json"])
}

private fun getServerConfig(): V2rayConfig {
    return blueprintConfig.copy()
}

fun getV2RayExecutablePath(): String {
    val path = System.getProperty("V2RAY_HOME")
        ?: System.getenv("V2RAY_HOME")
    val execName = "v2ray"
    return if (path == null) {
        execName
    } else {
        File(path, execName).path.also {
            println(it)
        }
    }

}