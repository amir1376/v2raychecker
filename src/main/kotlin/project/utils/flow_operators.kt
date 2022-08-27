package project.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.time.Duration

fun <T, R> Flow<T>.concurrentMap(
    capacity: Int = Channel.BUFFERED,
    transformBlock: suspend (T) -> R
): Flow<R> {
    return flow {
        coroutineScope {
            map {
                async {
                    transformBlock(
                        it
                    )
                }
            }
                .buffer(capacity)
                .map {
                    it.await()
                }
                .let {
                    emitAll(it)
                }
        }
    }
}

fun <T> Flow<T>.throttle(waitMillis: Int) = flow {
    coroutineScope {
        val context = coroutineContext
        var nextTime = 0L
        var delayPost: Deferred<Unit>? = null
        collect {
            val current = System.currentTimeMillis()
            if (nextTime < current) {
                nextTime = current + waitMillis
                emit(it)
                delayPost?.cancel()
            } else {
                val delayNext = nextTime
                delayPost?.cancel()
                delayPost = async(Dispatchers.Default) {
                    delay(nextTime - current)
                    if (delayNext == nextTime) {
                        nextTime = System.currentTimeMillis() + waitMillis
                        withContext(context) {
                            emit(it)
                        }
                    }
                }
            }
        }
    }
}


fun <T> Flow<T>.rateLimit(limit: Long, per: Duration): Flow<T> {
    return rateLimit(limit, per.inWholeMilliseconds)
}

fun <T> Flow<T>.rateLimit(limit: Long, per: Long) = flow<T> {
    coroutineScope {
        val context = coroutineContext
        var lastStartTime = System.currentTimeMillis()
        var remainingInDuration = limit
        val items = LinkedList<T>()
        var isDone = false
        launch(context) {
            collect {
                items.add(it)
            }
            isDone = true
        }
        launch(Dispatchers.Default) {
            while (isActive) {
                yield()
                if (remainingInDuration > 0) {
                    val removeFirst = items.removeFirstOrNull()
                    if (removeFirst != null) {
                        withContext(context) {
                            emit(removeFirst)
                        }
                        remainingInDuration--
                    } else {
                        if (isDone) {
                            break
                        }
                    }

                } else {
                    val waitUntil = lastStartTime + per
                    delay(waitUntil - System.currentTimeMillis())
                    lastStartTime = System.currentTimeMillis()
                    remainingInDuration = limit
                }
            }
        }
    }
}
