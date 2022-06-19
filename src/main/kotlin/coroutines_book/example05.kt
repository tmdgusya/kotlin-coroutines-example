package coroutines_book

import kotlinx.coroutines.delay

suspend fun a() {
    val user = readUser()
    b()
    b()
    b()
    println(user)
}

suspend fun b() {
    for (i in 1..10) {
        c(i)
    }
}

suspend fun c(i: Int) {
    delay(i * 100L)
    var a = 100
    println("Tick")
}

suspend fun readUser(): String {
    return "user"
}
