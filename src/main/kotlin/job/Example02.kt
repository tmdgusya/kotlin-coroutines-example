package job

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) {
        delay(1000)
        println("Text 1")
    }
    launch(job) {
        delay(2000)
        println("Job 2 = $job")
        println("Text 2")
    }
    println("Is Complete? ${job.complete()}")

    job.join()

    launch(job) {
        println("Text 3")
    }

    println(job.children.count()) // 0

    job.join() // not executed
    
    println("Will not be printed")
}
