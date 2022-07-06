package coroutine_action

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun a() = runBlocking {
    val a = coroutineScope {
        delay(3000)
        10
    }
    println("a is calculated")
    val b = coroutineScope {
        delay(3000)
        20
    }
    println(a) // 10
    println(b) // 20
}

fun main() = runBlocking {
    val a = runBlocking {
        delay(5000)
        10
    }
    println("a is calculated")
    val b = runBlocking {
        delay(5000)
        20
    }
    println(a) // 10
    println(b) // 20
}
