package project.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import project.ConfigResultItem
import project.testConfigs

fun startServer() {
    val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 80
    val users = buildMap {
        val user = System.getenv("SERVER_USERNAME")
        user?.let {
            put(it, System.getenv("SERVER_PASSWORD") ?: "")
        }
    }
    println("starting server on port :$port")
    embeddedServer(
        Netty,
        port = port
    ) {
        install(ContentNegotiation) {
            json()
        }
        install(Authentication) {
            basic {
                realm = "Access to the '/' path"
                validate { credentials ->
                    val allowed = users[credentials.name] == credentials.password
                    if (allowed || users.isEmpty()) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }


        routing {
            authenticate {
                post {
                    val body = call.receiveText()
                    val configs = body.lines().filter { it.isNotBlank() }
                    val flow = testConfigs(64, configs)
                    val result = flow.map {
                        ConfigResultItem(it.first, it.second?.orNull() ?: -1)
                    }.toList()
                    call.respond(result)
                }
            }
        }
    }.start(true)
}