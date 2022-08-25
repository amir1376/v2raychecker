package project.utils

import arrow.core.identity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <A, A2, B, B2> Flow<Pair<A, B>>.mapPair(
    transformFirst: suspend (A) -> A2,
    transformSecond: suspend (B) -> B2,
): Flow<Pair<A2, B2>> {
    return map {
        transformFirst(it.first) to transformSecond(it.second)
    }
}

fun <A, A2, B> Flow<Pair<A, B>>.mapFirst(
    transformFirst: suspend (A) -> A2,
): Flow<Pair<A2, B>> {
    return mapPair(transformFirst, ::identity)
}

fun <A, B, B2> Flow<Pair<A, B>>.mapSecond(
    transformSecond: suspend (B) -> B2,
): Flow<Pair<A, B2>> {
    return mapPair(::identity, transformSecond)
}


private const val DEFAULT_CONCURRENT_MAP_CAPACITY = Channel.BUFFERED
fun <A, A2, B, B2> Flow<Pair<A, B>>.concurrentMapPair(
    capacity: Int = DEFAULT_CONCURRENT_MAP_CAPACITY,
    transformFirst: suspend (A) -> A2,
    transformSecond: suspend (B) -> B2,
): Flow<Pair<A2, B2>> {
    return concurrentMap(capacity) {
        transformFirst(it.first) to transformSecond(it.second)
    }
}

fun <A, A2, B> Flow<Pair<A, B>>.concurrentMapFirst(
    capacity: Int = DEFAULT_CONCURRENT_MAP_CAPACITY,
    transformFirst: suspend (A) -> A2,
): Flow<Pair<A2, B>> {
    return concurrentMapPair(capacity, transformFirst, ::identity)
}

fun <A, B, B2> Flow<Pair<A, B>>.concurrentMapSecond(
    capacity: Int = DEFAULT_CONCURRENT_MAP_CAPACITY,
    transformSecond: suspend (B) -> B2,
): Flow<Pair<A, B2>> {
    return concurrentMapPair(capacity, ::identity, transformSecond)
}
