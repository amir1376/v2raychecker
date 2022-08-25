package project.extra.v2ray.extension

import java.net.URI
import java.net.URLConnection

val URLConnection.responseLength: Long
    get() = contentLengthLong

val URI.idnHost: String
    get() = (host!!).replace("[", "").replace("]", "")


