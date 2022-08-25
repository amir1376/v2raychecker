package project.utils

object Resources {
    operator fun get(path: String): String {
        return this.javaClass.classLoader.getResource(path)!!.readText()
    }
}