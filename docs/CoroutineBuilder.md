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
그래서 책에서는 자주 쓰이는 두가지 Case 를 알려주는데, 첫번째로는 main Thread 의 종료를 막기 위해 사용하고, 두번째도 비슷하게 테스트 케이스의 종료를 막기 위해 사용된다.

## async builder

async builder 는 launch 랑 비슷한데 값을 produce 하게 설계되어있다. 즉, 값을 Consume 만 하는거라면 보통의 상황에서 launch 를 택하고, `Deffered<Type>` 을 리턴하려면 async 를 고르면 된다. Deffered 는 await 을 사용해서 realize 해주어야 한다. 하지만, 값을 실체화 해야 하는 만큼, 실체화 하는 시점에서 값이 계산 중이라면 그때는 약간의 Blocking 이 발생할 수 있다.

```kotlin
fun main() = runBlocking {
val res1 = GlobalScope.async {
        delay(1000L)
        "Text 1"
}
val res2 = GlobalScope.async {
        delay(3000L)
        "Text 2"
}
val res3 = GlobalScope.async {
        delay(2000L)
        "Text 3"
   }
   println(res1.await()) // blocking 이 걸릴수도 있음.
   println(res2.await())
   println(res3.await())
}
```

## GlobalScope

우리는 예제에서 GlobalScope 를 많이 사용했는데, 현실에서는 거의 사용하지 않는다. 일단, **GlobalScope 는 SingleTon 으로 관리되며 Application 과 Life-Cycle 을 같이 하는 것**으로 알고 있다. 정확히는 GlobalScope 는 **top-level** 에서 실행되기 때문이다. 그래서 Deamon Thread 처럼 BackGround 에서 게속해서 돌아야 하는게 아니라면, 굳이 GlobalScope 를 사용할 이유는 없다고 생각한다. 아래 async, launch, runBlocking 코드를 한번 보자.

```kotlin
fun <T> runBlocking(
    context: CoroutineContext = EmptyCoroutineContext, 
    block: suspend CoroutineScope.() -> T
): T
fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext, 
    start: CoroutineStart = CoroutineStart.DEFAULT, 
    block: suspend CoroutineScope.() -> Unit
): Job
fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext, 
    start: CoroutineStart = CoroutineStart.DEFAULT, 
    block: suspend CoroutineScope.() -> T
): Deferred<T>
```

launch, runBlocking, async 의 코드를 보면 **CoroutineScope 의 Extension Function** 임을 확인할 수 있다. 따라서 굳이 GlobalScope 를 사용할 이유도 없으며 보통 GlobalScope 와 같은 Boundary 로 작업되야 하는 Job 이 없을 것 이다. Corotine Scope 를 만들어야 하는 상황이라면 아래와 같이 코드를 작성하는게 더 좋다.

```kotlin
suspend fun getArticlesForUser( 
    userToken: String?,
): List<ArticleJson> = coroutineScope {
    val articles = async { articleRepository.getArticles() } 
    val user = userService.getUser(userToken) 

    articles.await()
            .filter { canSeeOnList(user, it) }
            .map { toArticleJson(it) }
}
```

## 후기

지금 하고 있는 신규 프로젝트가 곧 마무리 되는데, 마무리 되면 팀내 코드에 코루틴을 조금씩 넣어보고 싶은 욕심이 생긴다. JavaScript 처럼 간단하게 사용 가능해서 참 편리하다는 생각이 든다.