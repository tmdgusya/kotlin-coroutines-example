import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 기존에 Join 을 유지해줘야 했던 이유는 GlobalScope 와 RunBlockingScope 가 연관이 없었기 때문인데, 그래서 runBlocking Scope 에서 실행시켜서 Join 을 없앴음
 * Coroutines 의 Join 을 명시적으로 사용하지 않아도 되는 좋은 이유
 */
fun main() = runBlocking {

    this.launch { // child coroutines
        delay(1000)
        println("World")
    }
    println("Hello, ")
}
