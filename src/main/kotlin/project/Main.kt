package project

import arrow.core.Either
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import project.extra.v2ray.util.AngConfigManager
import project.server.startServer
import project.utils.concurrentMapSecond
import project.utils.mapSecond
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking<Unit> {
    println(
        """
****** V2Ray Configs Checker ******
""".trimIndent()
    )
    val argParser = ArgParser(args)
    if (argParser.has("-server")) {
        startServer()
        return@runBlocking
    }
    val filePath = args.lastOrNull()?.takeIf {
        !it.startsWith("-")
    }
    val maxConcurrent = argParser["--concurrent"]?.toIntOrNull() ?: 120
    val outputPath = argParser["--output"]
    val asJson = argParser.has("-json")
    argParser["-v2ray"]?.let {
        System.setProperty("V2RAY_HOME", it)
    }
    val configsString = getConfigs(filePath)
    val testResult = testConfigs(maxConcurrent, configsString)

    if (outputPath != null) {
        writeToOutputFile(asJson, outputPath, testResult)
        return@runBlocking
    }

    defaultPrint(testResult, configsString)
}

suspend fun writeToOutputFile(
    asJson: Boolean,
    outputPath: String,
    testResult: Flow<Pair<String, Either<Throwable, Long>?>>
) {
    val r = if (asJson) {
        val map = testResult
            .toList()
            .map {
                mapOf(
                    "config" to (it.first),
                    "delay" to (it.second?.orNull() ?: -1)
                )
            }
        Gson().toJson(
            map
        )
    } else {
        filterInvalids(testResult)
            .toList()
            .joinToString("\n")
    }
    println("writing output to $outputPath")
    File(outputPath).writeText(r)
    println("completed.")
}

suspend fun defaultPrint(testResult: Flow<Pair<String, Either<Throwable, Long>?>>, configsString: List<String>) {
    println("---------- printing result ----------")
    var works = 0
    testResult
        .mapSecond {
            when (it) {
                null -> {
                    return@mapSecond "Invalid"
                }

                else -> it.fold({ e ->
                    e.message ?: "error"
                }, { delay ->
                    if (delay > 0) {
                        works++
                    }
                    "$delay"
                })
            }
        }
        .onEach { (config, delay) ->
            println("""$config $delay""")
        }
        .toList()
    println("---------- done ----------")

    println("total: ${configsString.size} works: $works")
}

suspend fun filterInvalids(testResult: Flow<Pair<String, Either<Throwable, Long>?>>): Flow<String> {
    return testResult
        .mapSecond { it?.orNull() }
        .map { it.first }
}

suspend fun testConfigs(
    maxConcurrent: Int,
    configs: List<String>
): Flow<Pair<String, Either<Throwable, Long>?>> {
    return configs
        .map {
            it to kotlin.runCatching {
                AngConfigManager.getConfigFromString(it).outboundBean
            }.getOrNull()
        }
        .asFlow()
        .concurrentMapSecond(maxConcurrent) {
            if (it == null) return@concurrentMapSecond null
            V2RayConfigEvaluator(it).use { ce ->
                ce.test()
            }
        }
}

fun getConfigs(filePath: String?): List<String> {
    val configs = if (filePath == null) {
        println(
            """
            no file path provided
            reading configs from input stream
            """.trimIndent()
        )
        getConfigsFromStream()
    } else {
        try {
            File(filePath).readText()
        } catch (e: Exception) {
            println("error : file $filePath " + e.message)
            exitProcess(-1)
        }
    }
    return configs.lines().filter {
        it.isNotBlank()
    }
}

fun getConfigsFromStream(): String {
    return System.`in`.bufferedReader().readText()
}
