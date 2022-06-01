package coroutines_book

fun main() {
    val seq = sequence {
        yield(1)
        yield(2)
        yield(3)
    }

    println(seq.filter { it > 1 }) // not evaluated

    println(seq.filter { it > 1 }.toList()) // realize
}
