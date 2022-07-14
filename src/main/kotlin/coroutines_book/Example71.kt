package coroutines_book

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun test2() = runBlocking {
    launch {
        println("${Thread.currentThread().name} Launch 1")
        delay(3000)
        // Some Api Calls
        println("hello, ")
    }
    launch {
        delay(2000)
        println("${Thread.currentThread().name} Launch 2")
    }

    launch {
        delay(3000)
        println("${Thread.currentThread().name} Launch 3")
    }

    return@runBlocking "ApiCalls Result"
}

fun main() {
    println(test2())
}
