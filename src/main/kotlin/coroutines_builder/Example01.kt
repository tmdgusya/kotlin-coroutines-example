package coroutines_builder

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun main() {
    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    println("Hello, ")
    Thread.sleep(2000)
}
