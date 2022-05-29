import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {

    GlobalScope.launch {
        delay(1000)
        println("World")
    }
    println("Hello, ")
    runBlocking { // blocking 을 해주는 coroutines
        delay(1000)
    }
}
