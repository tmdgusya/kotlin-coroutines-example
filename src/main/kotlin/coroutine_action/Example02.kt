package coroutine_action

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val exceptionHandler = CoroutineExceptionHandler { CoroutineContext, throwable ->
    println(throwable.message)
}

suspend fun main() {
    runBlocking(exceptionHandler) {
        val a = launch(SupervisorJob()) {
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch(SupervisorJob()) {
            delay(1000)
            throw RuntimeException("ㅋㅋ")
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
