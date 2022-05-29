import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    launch { // child coroutines
        myWorld()
    }
    println("Hello, ")
}

suspend fun myWorld() {
    delay(1000)
    println("World")
}
