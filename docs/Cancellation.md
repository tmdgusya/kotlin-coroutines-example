# Cancellation

Cancellation 은 Coroutine 에서 아주 중요한 개념이다. Coroutine 을 지원하는 Library 라면 Cancellation 을 잘 구현해야만한다. 예를 들어, Network 통신하다가 Exception 이 발생했을때, 누군가는 Thread 를 그냥 놓아줄수도있고, 누군가는 Cancelling State 에서 가지고 있는 Resource 를 반납할 수도 있기 때문이다. 이러한 선택은 Library 의 성능을 좌지우지 할 수 있으므로, Cancellation 은 정말 중요하다.

## Basic Cancellation

기본적으로 Cancellation 이 일어났을때 아래와 같은 Effect 가 발생한다.

- **Job 이 돌다가 첫번째 중단지점에서 종료한다.** (그냥 종료된다고 생각하면 편함.)
- **Job 이 만약 children 을 가지고 있었다면, 모두 종료된다.**
- **한번이라도 Cancel 되었던 Job 이라면, 어떠한 Coroutine 에서도 재사용할수 없다.** 그러니까 Cancelling 에서 Canceled 가 되는 상황은 단 1번 존재한다는 뜻이다.

## Cancel And Join

```kotlin
suspend fun main() = coroutineScope {
    val job = launch {
        repeat(1_000) { i ->
            delay(100)
            Thread.sleep(100) // We simulate long operation
            println("Printing $i")
        }
    }
    delay(1000)
    job.cancel()
    println("Cancelled successfully")
}
```

`cancel(exception: CancellationException)` 메소드는 Coroutines 에서 예외를 전파하기 위한 수단이다. 기존 예외를 사용하듯이 사용할 수 있어서 편하다. `CancellationException(message: String?, cause: Throwable?)`. cancel() 메소드는 **동작하게 됬을때 해당 Job 을 취소** 시키게 된다. 그렇다면 위의 코드는 어떻게 될까, 1, 2, 3, 4, Cancelled successfully 라고 나올 것 같지만, 그게 아니다. cancel() 을 호출하고 지나가기 때문에 아래와 같은 결과가 나온다.

```kotlin
Printing 0
Printing 1
Printing 2
Printing 3
Cancelled successfully
Printing 4
```

그래서 Job 이 완전히 끝나는걸 기다리기 위해서는, 아래와 같이 하는게 좋다.

```kotlin
suspend fun main() = coroutineScope {
    val job = launch {
        repeat(1_000) { i ->
            delay(100)
            Thread.sleep(100) // We simulate long operation
            println("Printing $i")
        }
    }
    delay(1000)
    job.cancel()
    job.join()
    println("Cancelled successfully")
}

Printing 0
Printing 1
Printing 2
Printing 3
Printing 4
Cancelled successfully
```

Kotlin 에서는 이를 위해 편의메소드인 `cancelAndJoin()` 메소드를 제공한다.

```kotlin
public suspend fun Job.cancelAndJoin() {
    cancel()
    return join()
}
```

열어보면 매우 단순하게 위와 같이 되어 있다.

## Cancellation 동작 방식

Job 이 Cancel 됬을때, 상태는 **"Cancelling"** 으로 변한다. 그리고 중단된 지점에서 CancellationException 이 발생한다. 명심해야 하는 점은, 코루틴의 Cancellation 은 단순히 멈추는게 아니라, 내부에서 exception 을 통해서 취소시킨 것이다. 그러므로, 우리는 finally block 에서 tear-down 이나 clean-up 행동을 취해줘야 한다.

```kotlin
suspend fun main(): Unit = coroutineScope { 
    val job = Job()
    launch(job) {
        try { 
            val user = database.query("select * from User")
        } finally {
            // clean-up
            database.closeResource()
        } 
    }
    delay(1000)
    job.cancelAndJoin()
}
```

한번 `cancel()` 메소드가 호출되면 다른 coroutine 을 시작할 수 없다. 근데 만약에 정말 R2DB 를 사용하거나 이런 상황에서 사용해야 한다면, withContext(NonCancellable) 을 사용하면 된다.