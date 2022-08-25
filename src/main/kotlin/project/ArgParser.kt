package project

class ArgParser(val args: Array<String>) {


    fun has(vararg key: String): Boolean {
        return listOf(*key).any {
            it in args
        }
    }

    operator fun get(key: String): String? {
        return args.find {
            it.startsWith(key)
        }?.substring("$key=".length)
    }
}