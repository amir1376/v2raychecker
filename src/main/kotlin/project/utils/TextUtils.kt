package project.utils

fun String.startWithOneOf(vararg list: String): Boolean {
    return list.any {
        startsWith(it)
    }
}