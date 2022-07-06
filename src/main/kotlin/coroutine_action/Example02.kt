package coroutine_action

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    println(throwable.message)
}

suspend fun main() = withContext(SupervisorJob() + exceptionHandler) {
    val a = coroutineScope {
        delay(1000)
        throw RuntimeException("Test")
        10
    }
    println("a is calculated")
    val b = coroutineScope {
        delay(1000)
        20
    }
    println(a) // 10
    println(b) // 20
}

// fun main() = runBlocking {
//    val a = runBlocking {
//        delay(3000)
//        throw RuntimeException("Test")
//        10
//    }
//    println("a is calculated")
//    val b = runBlocking {
//        delay(3000)
//        20
//    }
//    println(a) // 10
//    println(b) // 20
// }
