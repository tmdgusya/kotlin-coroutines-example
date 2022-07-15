package actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface Operation {
    fun compute(value: Value): Int
}

sealed class Operator : Operation

object Plus : Operator() {
    override fun compute(value: Value): Int {
        return value.num1 + value.num2
    }
}

object Minus : Operator() {
    override fun compute(value: Value): Int {
        return value.num1 - value.num2
    }
}

object Multiply : Operator() {
    override fun compute(value: Value): Int {
        return value.num1 * value.num2
    }
}

object Devide : Operator() {
    override fun compute(value: Value): Int {
        return value.num1 / value.num2
    }
}

class Result(
    val result: CompletableDeferred<Int>
) : Operator() {
    override fun compute(value: Value): Int {
        result.complete(value.num1 + value.num2)
        return -1
    }
}

data class Value(
    val num1: Int,
    val num2: Int
)

data class Request(
    val value: Value,
    val operator: Operator,
)

fun CoroutineScope.calcualtor(): Channel<Request> {
    val channel = Channel<Request>()

    launch {
        var result = 0
        for (operationRequest in channel) {
            val operator = operationRequest.operator
            val value = operationRequest.value
            when (operator) {
                is Result -> {
                    operator.compute(Value(num1 = result, num2 = 0))
                }

                else -> {
                    result += operator.compute(value)
                }
            }

        }
    }
    return channel
}

suspend fun main(): Unit = coroutineScope {
    val calculator: SendChannel<Request> = calcualtor()
    val result = CompletableDeferred<Int>()

    calculator.send(
        Request(
            value = Value(1, 2),
            operator = Plus
        )
    )

    calculator.send(
        Request(
            value = Value(3, 5),
            operator = Plus
        )
    )

    calculator.send(
        Request(
            value = Value(0, 0),
            operator = Result(result)
        )
    )

    println(result.await()) // 11

    calculator.close()
}