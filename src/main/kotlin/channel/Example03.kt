package channel

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
    val channel = Channel<Int>()

    launch(SupervisorJob()) {
        repeat(5) { index ->
            try {
                delay(1000)
                println("Producing Index")
                if (index == 3) {
                    throw IllegalArgumentException("")
                }
                channel.send(index)
            } catch (e: Exception) {
                channel.close()
                throw e
            }
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index")
        }
    }
}