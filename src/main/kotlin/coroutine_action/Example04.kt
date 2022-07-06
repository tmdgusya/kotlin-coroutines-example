package coroutine_action

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private val exceptionHandler = CoroutineExceptionHandler { CoroutineContext, throwable ->
    println(throwable.message)
}

suspend fun main() {
    runBlocking(exceptionHandler) {
        val a = async(SupervisorJob()) {
            throw RuntimeException("zzz")
            10
        }
        println("a is calculated")
        val b = async {
            delay(1000)
            20
        }
        delay(1000)
        println(a.await()) // 10
        println(b.await()) // 20
    }
}
