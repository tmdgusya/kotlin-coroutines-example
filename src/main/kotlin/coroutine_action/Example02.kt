package coroutine_action

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    println(throwable.message)
}

suspend fun main() {
    CoroutineScope(Dispatchers.IO).launch(exceptionHandler) {
        println("Current Thread : ${Thread.currentThread().name}")
        val a = launch {
            println("Current Thread A : ${Thread.currentThread().name}")
            delay(1000)
            throw RuntimeException("Test")
            10
        }
        println("a is calculated")
        val b = launch {
            println("Current Thread B : ${Thread.currentThread().name}")
            delay(1000)
            20
        }
        println("Current Thread : ${Thread.currentThread().name}")
        println(a) // 10
        println(b) // 20
    }.join()
}
