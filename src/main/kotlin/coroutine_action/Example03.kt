package coroutine_action

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val exceptionHandler2 = CoroutineExceptionHandler { CoroutineContext, throwable ->
    println(throwable.message)
}

suspend fun main() {
    withContext(exceptionHandler2) {
        val a = launch(SupervisorJob()) {
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch(SupervisorJob()) {
            throw RuntimeException("ㅋㅋ")
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
