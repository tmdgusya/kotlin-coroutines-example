package coroutines_book

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun test() = withContext(Dispatchers.IO) {
    launch {
        delay(3000)
        println("${Thread.currentThread().name} Launch 1")
        // Some Api Calls
        println("hello, ")
    }
    launch {
        delay(100)
        println("${Thread.currentThread().name} Launch 2")
    }

    launch {
        delay(500)
        println("${Thread.currentThread().name} Launch 3")
    }

    return@withContext "ApiCalls Result"
}

suspend fun main() {
    println(test())
}
