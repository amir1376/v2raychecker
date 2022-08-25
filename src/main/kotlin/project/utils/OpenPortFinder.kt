package project.utils

import java.lang.Exception
import java.net.ServerSocket

object OpenPortFinder {
    fun findOpenPort(): Int {
        ServerSocket(0).use {
            return it.localPort
        }
    }

    fun findOpePorts(count: Int): List<Int> {
        return (0 until count).mapIndexed { index, i ->
            try {
                findOpenPort()
            } catch (e: Exception) {
                println("error in $index")
                throw e
            }
        }.distinct()
    }
}