package project.utils

import com.google.gson.Gson
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf


inline fun <reified T> Gson.fromJson(json: String) =
    fromJson<T>(json, typeOf<T>().javaType)