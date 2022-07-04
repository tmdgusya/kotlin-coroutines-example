# Exception Handling

앞에서도 말했듯 Coroutine 에는 Structured Concurrency 개념이 존재해서, Children 에서 Exception 이 전파될 경우 부모 또한 취소된다고 말했었다. 그래서 우리는 부모까지 uncaught Exception 이 전파되어서 취소되는 상황을 막아야 한다면, Exception 을 Handling 해야만 한다.

## Exception Handling 방법

가장 무난하게는 Try...Catch 를 사용하는 방법이 있을 수 있다. 참고로 아래 코드 처럼 launch 를 try..catch 로 덮는것은 아무의미가 없다.

```kotlin
fun main(): Unit = runBlocking {
    try {
        launch {
            delay(1000)
            throw Error("Some error")
        }
    } catch (e: Throwable) {
        println(e.message)
    }

    launch {
        delay(2000)
        println("Will not be printed")
    }
}
```

차라리 try..catch 를 사용할꺼면 아래처럼 하는게 좋다.

```kotlin
fun main(): Unit = runBlocking {
    launch {
        delay(1000)
        try {
            throw Error("Some error")
        } catch (e: Throwable) {
            println(e.message)
        }
    }

    launch {
        delay(2000)
        println("Will not be printed")
    }
}
```

위처럼 하는 방식은 가장 무난하나, 공통적인 ExceptionHandling 로직이 필요하다면 계속해서 이렇게 코드를 중복하여 적는 것도 문제가 될 것이다.

### SupervisorJob

SupervisorJob 을 사용하여 ExceptionHandling 을 하는게 좋은데, 그 이유는 SuperviserJob 은 예외 전파를 시키지 않는다. 쉽게 얘기하면 자식의 Exception 전파를 무시한다고 생각하면 된다.

<img width="510" alt="image" src="https://user-images.githubusercontent.com/57784077/177174723-4b0ad5cf-45f7-450e-89d2-35693c4ad37d.png">

아래와 같은 코드를 실행시켰을때, 하나의 Coroutine Child 는 실패하지만, 나머지 Child 들은 그대로 수행된다.

```kotlin
fun main(): Unit = runBlocking {
    val scope = CoroutineScope(SupervisorJob())
    scope.launch { // a
        delay(1000)
        throw Error("Some error")
    }
    scope.launch { // b
        delay(2000)
        println("Will be printed")
    }
    delay(3000)
}

// Error
// Will be printed
```

위의 예시를 보면 `SupervisorJob()` 이 코루틴 하나를 실행시킬때마다, 하나의 다른 스코프를 사용한다. 라고 이해하면 조금 더 이해가 잘 갈것이다. 이렇게 이해하고 봤을때, `a` 의 scope 과 `b` 의 scope 은 다르므로 서로 영향을 받지 않는 것 이다. 우리가 전글에서 봤던 `Job()` 팩토리 메서드 처럼 일단은 생각하면 이해가 편할 것이다.

### Supervisor 를 이상하게 사용하기 쉬운 부분

아래와 같이 `superviserJob()` 을 parent 의 argument 로 넘기는 경우 문제가 발생한다. 

```kotlin
fun main(): Unit = runBlocking {
    launch(SupervisorJob()) {
        launch {
            delay(1000)
            throw Error("Some error")
        }
        launch {
            delay(2000)
            println("Will be printed")
        }
    }
    delay(3000)
}
```

왜 이런 문제가 발생할까? 이 경우에 SupervisorJob 은 Argument 로 SuperviserJob 을 받는 하나의 Child 만 가지게 된다. 지금의 예시 코드를 봤을때 Error 를 터트리는 코드를 자식으로 가지게 된다. 일상 우리가 코드를 작성할때, Api 를 여러번 호출하는 코드를 작성하는데 SuperviserJob 을 하나를 위와 같이 쓴다면, 정확하게 ExceptionHandling 이 되지 않을 것이다.

## SupervisorScope

Exception 을 Handling 하는 가장 쉬운 방법은 SupervisorScope 를 사용하는 것이다. 아래 코드를 한번 보자.

```kotlin
fun main(): Unit = runBlocking {
    supervisorScope {
        launch {
            delay(1000)
            throw Error("Some error")
        }
        launch {
            delay(2000)
            println("Will be printed")
        }
    }
    delay(1000)
    println("Done")
}
```

supervisorScope 을 이용하면 위와 같이 아주 편리하게 ExceptionHandling 을 처리할 수 있다.

### CancellationException 의 전파

CancellationException 의 경우 예외가 부모로 전파되지 않는다.

```kotlin
suspend fun main(): Unit = coroutineScope {
    launch { // 1
        launch { // 2
            delay(2000)
            println("Will not be printed")
        }
        throw MyNonPropagatingException // 3
    }
    launch { // 4
        delay(2000)
        println("Will be printed")
    }
}
```

위와 같은 코드를 작성했을때 4번 코드는 정상적으로 실행된다. 그 이유는 CancellationException 은 위로 전파되지 않기때문이다.

## Exception Handler 이용

Exception 을 다룰때 Handling 할 로직을 넣어주기 위해서는 아래처럼 ExceptionHandler 를 사용하면 편하다. Exception 이 Propagate 되지도 않고, Handling 하기 매우 쉽다.

```kotlin
fun main(): Unit = runBlocking {
    val handler =
        CoroutineExceptionHandler { ctx, exception ->
            println("Caught $exception")
        }
    val scope = CoroutineScope(SupervisorJob() + handler)
    scope.launch {
        delay(1000)
        throw RuntimeException("Some error")
    }
    scope.launch {
        delay(2000)
        println("Will be printed")
    }
    delay(3000)
}
```