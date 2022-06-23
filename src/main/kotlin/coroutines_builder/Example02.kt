package coroutines_builder

import kotlin.concurrent.thread

fun main() {
    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    println("Hello, ")
    Thread.sleep(2000)
}
