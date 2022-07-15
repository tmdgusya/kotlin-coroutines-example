# Actor

<img width="408" alt="image" src="https://user-images.githubusercontent.com/57784077/179218185-294d4a9f-6dca-457e-abf9-a3f0116a4e52.png">

CS 에서 Actor 라는 동시성 모델이 있다. 음, 일단 말이 되게 어려운데 쉽게 말하면 동시성 연산을 수행해주는 모델이라고 생각하면 된다. 
이 Actor 라는 녀석은 **private 한 state 를 가지는데, 이건 외부에 message 로 인해 영향을 받아 변경**될 수 있다. 이게 말이 참 어려운데 코드를 보면 편하다. **이해시키기 위해 코드를 하나 작성**해왔다.

```kotlin
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
        return 0
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
```

위의 코드를 보면 Operation 이라는 Interface 가 있고, 각각의 연산들은 Operator Seald Class 를 상속받고 있다. 
Sealed Class 를 차용한 이유는 최대한 Operator 는 이 클래스임을 제한하기 위해 사용했다. 여하튼, 각 연산들은 자신만의 연산을 구현하고 있다.
우리가 여기서 중요하게 봐야할 코드는 아래 코드이다.

```kotlin
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
```

위의 코드를 보면 해당 코드는 자신만의 **priavte 한 state 인 result 를 가지고 있다. 이 result 는 왜부에서 접근이 불가능하지만, 외부의 연산에 의해 변경될 수 있는 코드**이다. 
즉, Actor Model 은 아까 설명한대로 위처럼 private 한 Model 을 가지고 있지만, 외부의 message 에 의해서 영향을 받는 구조이다. 해당 Function 은 오로지 연산만을 수행하는 책임을 가지고 있다.
따라서 우리는 Caculator 를 통해서 연산을 수행할 수 있다. 아래와 같이 Main 문에서 CalcualteRequest 를 보내면 연산이 수행되게 된다.

```kotlin
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
```

## 후기

음, 현실적으로 내가 현업에서 이런 코드를 짤일이 있을까? 싶긴한데 개인적으로 라이브러리를 만든다면 뭔가 한곳에 연산을 모으는 책임을 주고 싶을때 사용할 수 있을 것 같다는 생각이 들었다.

## 코드 저장소

https://github.com/tmdgusya/kotlin-coroutines-example/blob/master/src/main/kotlin/actor/Calculator.kt