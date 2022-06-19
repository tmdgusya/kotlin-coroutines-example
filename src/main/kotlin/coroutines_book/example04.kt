package coroutines_book

import kotlinx.coroutines.delay

suspend fun myFunction() {
    println("Before")
    var counter = 0
    delay(1000) // suspending
    counter++
    println("Counter: $counter")
    println("After")
}
