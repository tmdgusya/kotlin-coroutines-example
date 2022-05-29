import java.lang.Thread.sleep
import kotlin.concurrent.thread

fun main() {

    /**
     * GlobalScope 는 안을보면 SingleObjcet 로 GlobalScope
     */
    thread {
        sleep(1000)
        println("World") // non-blocking main thread
    }
    println("Hello, ")
    sleep(1000) // block main-thread
}

/**
 * launch 는 Coroutine Builder
 * Coroutines 는 Coroutines Scope 에서 동작한다.
 */
