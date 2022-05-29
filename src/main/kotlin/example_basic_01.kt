import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() {

    /**
     * GlobalScope 는 안을보면 SingleObjcet 로 GlobalScope
     */
    GlobalScope.launch {
        delay(1000)
        println("World") // non-blocking main thread
    }
    println("Hello, ")
    Thread.sleep(1000) // block main-thread
}

/**
 * launch 는 Coroutine Builder
 * Coroutines 는 Coroutines Scope 에서 동작한다.
 */
