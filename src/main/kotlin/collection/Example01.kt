package collection

fun main() {
    val collection = mutableListOf<Int>()

    for (i in 0 until 100000000) collection.add(i)

    collection
        .map { it * 2 }
        .filter { it <= 10 }
        .map { it * it }
        .filter { it % 2 == 0 }
        .forEach { println("List = $it") }

}

//fun main() {
//    val collection = mutableListOf<Int>()
//
//    for (i in 0 until 100000000) collection.add(i)
//
//    collection
//        .asSequence()
//        .map { it * 2 }
//        .filter { it <= 10 }
//        .map { it * it }
//        .filter { it % 2 == 0 }
//        .forEach { println("Sequence = $it") }
//}