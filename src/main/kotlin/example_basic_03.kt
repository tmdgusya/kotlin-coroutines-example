import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * delay 를 하는 코드는 좋은 코드가 아님 그래서 join() 을 통해서 기다리게 만들어야 함.
 */
fun main() = runBlocking {
    val job = GlobalScope.launch {
        delay(1000)
        println("World")
    }
    println("Hello, ")
    job.join()
}
