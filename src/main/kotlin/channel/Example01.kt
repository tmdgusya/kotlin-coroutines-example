package channel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
    val channel = Channel<Int>()

    launch {
        repeat(5) { index ->
            delay(1000)
            println("Producing Index")
            channel.send(index)
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index")
        }
    }
}