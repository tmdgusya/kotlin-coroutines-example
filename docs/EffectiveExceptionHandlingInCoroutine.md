# Coroutine Exception Handling

Coroutine 의 Exception Handling 은 Coroutine 을 제대로 공부하지 않으면 사용하기 힘들다. 왜냐하면 사용하는 Coroutine Builder 마다 Exception 을 전파하는 방식이 다르기 때문이다.

## Coroutine Builder 간의 차이

- launch 의 경우 Exception 이 발생하게 되면 즉시 Exception 을 위로 전파하는 성질이 있습니다.
- async 의 경우 Exception 이 발생해도 즉시 전파하지않고, **await() 이 실행될때 전파**합니다.

사실 이런 부분은 코드로 봐야 조금 더 직관적이므로 코드로 설명하겠습니다.

## launch

launch 의 경우 위에서 설명했듯이 Exception 을 그 즉시 상위로 전파하는 속성을 가지고 있습니다.

```kotlin
suspend fun main() {
    val job = runBlocking {
        val a = launch {
            delay(1000)
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch {
            delay(1000)
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

위의 코드에서 `a` job 을 실행할때 Exception 이 발생하는 순간 예외가 터지게 됩니다. 그렇다면 예외를 어떻게 처리할 수 있을까요? 가장 간단한 방법으로는 **try..catch 를 이용**할 수 있습니다.

```kotlin
suspend fun main() {
    runBlocking {
        val a = launch {
            try {
                delay(1000)
                throw RuntimeException("Test")
            } catch (e: java.lang.Exception) {
                println(e.message)
            }
        }
        println("a is calculated")
        val b = launch {
            delay(1000)
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

try..catch 를 이용해서도 손쉽게 Exception 을 컨트롤 할 수 있습니다. 근데 우리가 공통적으로 Exception Handling 하는 로직이 같다면 **계속해서 똑같은 try..catch 를 사용하거나, 이를 템플릿으로 만들어서 사용하는 코드 구조**가 될것입니다. 그래서 이를 위해 코루틴에는 ExceptionHandler 가 별도로 존재합니다.

```kotlin
val exceptionHandler = CoroutineExceptionHandler { CoroutineContext, throwable ->
    println(throwable.message)
}
```

ExceptionHandler 는 위와 같이 CoroutineContext 를 받으며, throwable 을 통한 공통적인 예외처리가 가능하게 됩니다.

```kotlin
suspend fun main() {
    runBlocking {
        val a = launch(SupervisorJob() + exceptionHandler) {
            delay(1000)
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch {
            delay(1000)
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

위와 같이 에러가 나는 부분에 **SupervisorJob Context 와 exceptionHandler** 를 이용하면 Error 를 try..catch 를 사용하지 않고도 핸들링 할 수 있습니다. (GlobalScope 를 통한 방법도 있으나, GlobalScope 를 개인적으로 쓸 상황은 많지 않다고 생각해서 적지는 않겠습니다.). 만약 runBlocking 에서 공통적으로 에러핸들러를 이용해서 에러를 핸들링 하고 싶다면 어떻게 해야할까요?

```kotlin
suspend fun main() {
    runBlocking(SupervisorJob() + exceptionHandler) {
        val a = launch {
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch {
            delay(1000)
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

이 코드가 ErrorHandling 이 될까요? 정답은 x 입니다. 이 코드는 ErrorHandling 이 되지 않습니다. 왜일까요? 이미 `a` 의 launch 에서 Exception 이 전파되어 Coroutine 전체가 Cancel 되기 때문입니다. 따라서 exceptionHandler 는 공통으로 사용하고 싶은데, Error 가 터지는 몇개 잡들은 컨트롤 하고 싶어 라고 하면 아래와 같이 코드를 작성하면 됩니다.

```kotlin
suspend fun main() {
    runBlocking(exceptionHandler) {
        val a = launch(SupervisorJob()) {
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch(SupervisorJob()) {
            delay(1000)
            throw RuntimeException("ㅋㅋ")
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

만약 runBlocking 은 Thread 를 Blocking 시키니까, Suspend 함수로 하고 싶다면 아래와 같이 코드를 작성할 수 있을 것입니다.

```kotlin
suspend fun main() {
    withContext(exceptionHandler2) {
        val a = launch(SupervisorJob()) {
            throw RuntimeException("Test")
        }
        println("a is calculated")
        val b = launch(SupervisorJob()) {
            throw RuntimeException("ㅋㅋ")
            20
        }
        delay(5000)
        println(a) // 10
        println(b) // 20
    }
}
```

## Async

Async 의 경우 위에서 설명했 듯 Exception 이 발생해도 즉시 전파 하지않고, **await() 이 실행될때 전파**합니다. 이건 **await method 의 설명을 보면 조금 더 직관적**입니다.

> ... returning the resulting value or throwing the corresponding exception if the deferred was cancelled.

기본적으로 async 는 deffered 라는 JavaScript 의 Promise 와 비슷한 객체를 만들게 되는데, 이 Deffered 가 내부에서 Exception 이 터지거나, Cancel 등으로 만약 Cancelled 됬다면, 그때 예외를 응답시킵니다.

위에서 많이 설명했으니 기본적으로 Exception Handler 와 SupervisorJob 으로 한번 Catch 해보겠습니다.

```kotlin
suspend fun main() {
    runBlocking(exceptionHandler) {
        val a = async(SupervisorJob()) {
            throw RuntimeException("zzz")
            10
        }
        println("a is calculated")
        val b = async(SupervisorJob()) {
            delay(1000)
            20
        }
        delay(5000) // 이때 까지 에러 안남.
        println(a.await()) // 10
        println(b.await()) // 20
    }
}
```

이 코드를 실행시키면 Exception 이 발생하게 됩니다. 이유가 뭘까요? launch 에서는 분명 똑같이 하면 Exception 이 Catch 됬었습니다. 이유는 위에서 설명한대로 **await() 에서 예외가 방출되기 때문**입니다. 심층적으로 코드를 까보면 더 알기 쉬운데, 이제 쉽게 설명하면 async 작업을 진행하던 도중 예외가 발생하여 Deffered 가 Cancel 상태가 됩니다. 그래서 await() 을 하는 순간 runBlocking Scope 가 취소되게 됩니다. 왜냐하면 runBlocking Scope 에서 터졌기 때문이죠. 그렇다면 어떻게 해야할까요? 정답은 **어쩔 수 없이 try..catch 를 이용**해야 합니다.

```kotlin
suspend fun main() {
    runBlocking {
        val a = async {
            try {
                throw RuntimeException("zzz")
                10
            } catch (e: java.lang.Exception) {
                println(e.message)
            }
        }
        println("a is calculated")
        val b = async {
            delay(1000)
            20
        }
        delay(1000)
        println(a.await()) // 10
        println(b.await()) // 20
    }
}
```

위와 **같이 적으면 Catch 가 잘됩니다. 그리고 b 또한 무사히 수행**됩니다. 근데 우리가 말한대로 await() 에서 예외가 발생하니까, 아래와 같이 작성해도 될까요?

```kotlin
suspend fun main() {
    runBlocking {
        val a = async {
            throw RuntimeException("zzz")
            10
        }
        println("a is calculated")
        val b = async {
            delay(1000)
            20
        }
        Thread.sleep(5000)
        try {
            println(a.await()) // 10
            println(b.await()) // 20
        } catch (e: RuntimeException) {
            println(e.message) // 여기서 에러 잡힘
        }
    }
}
```

위와 같이 작성하면 결과는 아래와 같습니다.

```
a is calculated
zzz
Exception in thread "main" java.lang.RuntimeException: zzz
```

보면 **에러가 잡혀서 "zzz" 도 찍혔지만 다시 에러가 전파되는걸 확인**할 수 있습니다. 그 이유는 Coroutine 에서 예외가 발생했기 때문에 이미 Job 은 Cancel 되었고, 그 예외가 위로 전파되기 때문입니다. 따라서 위와같이 await() 에서 try..catch 를 작성해야 한다면 아래와 같이 코드를 작성해야 합니다.

```kotlin
suspend fun main() {
    runBlocking(exceptionHandler) {
        val a = async(SupervisorJob()) {
            throw RuntimeException("zzz")
            10
        }
        println("a is calculated")
        val b = async {
            delay(1000)
            20
        }
        Thread.sleep(5000)
        try {
            println(a.await()) // 10
            println(b.await()) // 20
        } catch (e: RuntimeException) {
            println(e.message)
        }
    }
}
```

위와 같이 작성하면 예외가 전파되지 않습니다.

## 정리

위와 같은 이유로 Async 는 Exception 을 전파한다라는 단어말고도, 외부로 예외를 표출(expose) 한다 라고 쓰이는 경우도 있습니다. launch 와 async 는 위와 같은 차이를 가지고 있어서 Exception Handling 하실때 유의하여 사용해야 합니다.