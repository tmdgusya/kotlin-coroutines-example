import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext

@OptIn(InternalCoroutinesApi::class)
suspend fun main() = coroutineScope {
    val dispatcher = newSingleThreadContext(name = "ServiceCall")

    val defer1 = async(context = dispatcher) {
        delay(1000)
        println("Hello")
    }

    val defer2 = async(context = dispatcher) {
        delay(1000)
        println(" World")
    }

    if (defer1.isCancelled) {
        val exception = defer1.getCancellationException()
        println(exception.message)
    }

    defer1.await()
    defer2.await()
}