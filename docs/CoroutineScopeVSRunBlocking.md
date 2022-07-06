# CoroutineScope VS Runblocking

Kotlin Coroutines 을 학습했다면 위와 같은 고민을 하고 있을 가능성이 높다고 생각한다. 도대체 둘의 차이는 무엇일까? 일단 아래 예시 코드를 한번 보자.

```kotlin
// 1번 코드
fun main() = runBlocking {
    val a = coroutineScope {
        delay(3000)
        10
    }
    println("a is calculated")
    val b = coroutineScope {
        delay(3000)
        20
    }
    println(a) // 10
    println(b) // 20
}

// 2번 코드
fun main() = runBlocking {
    val a = runBlocking {
        delay(5000)
        10
    }
    println("a is calculated")
    val b = runBlocking {
        delay(5000)
        20
    }
    println(a) // 10
    println(b) // 20
}
```

이 포스트를 이해하기 위해서는 최소, 지금 1번 코드와 2번코드가 어떻게 도는지는 알고 있어야 한다. 일단 결과를 말해주자면 **둘다 같은 결과**가 나온다. 그렇다면 CoroutineScope 와 runBlocking 은 무슨 차이가 있는 것일까?

## Runblocking 

일단 RunBlocking Code 의 주석 중 일부를 발췌해보겠다.

> Runs a new coroutine and blocks the current thread interruptibly until its completion.

위의 영어를 해석해보면 현재 Thread 를 작업이 완료될때까지 Blocking 한다는 것을 알 수 있다. 즉, Runblocking 은 Suspend Function 이 아니다. 기존에 Java 에서 우리가 동기 처리를 하기 위해 Thread 를 끝날때까지 Blocking 하는 것과 같은 역할을 하는 것이다. 그럼 반대로 CoroutineScope 는 어떨까?

## CoroutineScope

CoroutineScope 의 코드 일부를 보자.

```kotlin
public suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return suspendCoroutineUninterceptedOrReturn { uCont ->
        val coroutine = ScopeCoroutine(uCont.context, uCont)
        coroutine.startUndispatchedOrReturn(coroutine, block)
    }
}
```

위의 함수명만 봐도 알 수 있듯이 suspend Function 이다. 즉 coroutineScope 야 말로, Coroutine 을 제대로 이용할 수 있는 함수이다. 

## 그렇다면 왜 runBlocking 이 존재?

앞서 말했듯이, 우리가 Thread 를 Blocking 해야 하는 상황이 반드시 생긴다. suspend 함수는 Thread 를 Blocking 시키지 않는다는 큰 장점이 있지만, 그와 반대로 Main Thread 가 그대로 흘러가서 Application 이 죽을 수 있다는 단점 또한 존재한다. 따라서 runblocking 을 사용해야 하는 상황이 나온다. 