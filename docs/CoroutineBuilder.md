# CoroutineBuilder

Kotlin 에서 Suspend function 은 normal function 에서 호출할 수 없다. Suspend Function 은 오로지 Suspend Function 에서만 호출되어야 한다. 하지만 우리가 Suspend 를 사용해야 하는데, 모든 함수가 suspend 일 수 있을까? 적어도 main(프로그램 진입점) 은 suspend 하지 않을 수 있다. 그래서 코틀린은 suspend scope 를 만들 수 있도록 corutineBuilder 를 제공한다. 대표적으로 제공하는 세가지 **corutineBuilder (launch, runBlocking, async)** 에 대해 공부해보자.

## launch builder

launch builder 는 concept 적으로 새로운 daemon thread 를 생성하는 것과 비슷하다. 아래 코드를 한번 살펴보자. **(다만 아래 예시처럼 GlobalScope 를 쓸일이 많지는 않을 것이다.)**

```kotlin
fun main() {
    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    GlobalScope.launch {
        delay(1000)
        println("Roach!")
    }

    println("Hello, ")
    Thread.sleep(2000)
}
```

```kotlin
fun main() {
    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    thread(isDaemon = true) {
        Thread.sleep(1000)
        println("Roach!")
    }

    println("Hello, ")
    Thread.sleep(2000)
}
```

이 두개의 코드 모두다 동작결과는 같다. 왜냐하면 Daemon Thread 도 BackGround Thread 로 BackGround 에서 Job 을 수행하기 때문이다. 다만 Cost 의 차이가 있다. Daemon Thread 를 사용할때 Delay 를 걸기 위해서는 Thread 를 Blocking 해야 한다. **항상 Thread Blocking 은 엄청 비싼 비용 중 하나라는 걸 명심해야 한다. CPU 를 효율적으로 쓰지 못하게 될 것이기 때문이다.**, 하지만 Kotlin Coroutines 의 **Suspend Function 은 함수가 일시중지 될뿐 Thread 는 Blocking 되지 않는다.** 이것이 Kotlin Suspend Function 의 장점이다.

## runBlocking Builder

기본적으로 Coroutine 은 Thread 를 Block 시키지 않는게 원칙이지만, 때론 Thread 의 Block 이 필요할때가 있다. (Main Thread 의 종료를 방지해야 해야하는 상황 등등..). 이럴때 runBlocking 을 사용할 수 있다. 아래 코드를 한번 보자.

```kotlin
fun main() { 
    runBlocking {
       delay(1000L)
       println("World!")
    }
    runBlocking {
       delay(1000L)
       println("World!")
    }
    runBlocking {
       delay(1000L)
       println("World!")
    }
    println("Hello,")
}

// (1 sec)
// World!
// (1 sec)
// World!
// (1 sec)
// World!
// Hello,
```

결과를 보면 결국 Thread 를 Blocking 하는 것과 똑같다. 그렇다면 여기서 우리는 이걸 사용하는 이유가 있나? 라는 의문을 가지게 될 것이다.