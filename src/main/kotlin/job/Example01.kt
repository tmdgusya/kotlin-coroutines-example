package job

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    launch {
        delay(1000)
        println("Test1")
    }
    launch {
        delay(2000)
        println("Test2")
    }
    println("All tests are done")

    val children = this.coroutineContext[Job]?.children
    val job = this.coroutineContext[Job]
    val NumbersOfChildren = children?.count()

    println("The Job($job) have $NumbersOfChildren children")
}
