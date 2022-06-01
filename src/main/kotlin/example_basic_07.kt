import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    launch {
        repeat(5) { i ->
            println("Coroutines A : $i")
            delay(10) // coroutines suspend
        }
    }

    launch {
        repeat(5) { i ->
            println("Coroutines B : $i")
        }
    }

    // a coroutines start

    println("Coroutines Over!!")
}

fun <T> println(msg: T) {
    kotlin.io.println("[${Thread.currentThread().name}]$msg")
}
