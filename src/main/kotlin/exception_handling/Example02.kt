package exception_handling

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    launch {
        delay(1000)
        try {
            throw Error("Some error")
        } catch (e: Throwable) {
            println(e.message)
        }
    }

    launch {
        delay(2000)
        println("Will not be printed")
    }
}
