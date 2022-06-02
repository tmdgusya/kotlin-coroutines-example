package coroutines_book

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    println("Before")

    suspendCoroutine<Unit> { continuation ->
        println(continuation) // SafeContinuation for Continuation at coroutines_book.Example03Kt.main(example03.kt:9)
        continuation.resume(Unit)
    }

    println("After")
}
