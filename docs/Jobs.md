# Jobs

코루틴에서 Job 이란 무엇일까? 컨셉적으로는 LifeCycle 에서 취소될 수 있는 것을 뜻한다. Job 을 알아야 하는 이유는 내가 알기론, 모든 ㄴKotlin Coroutines Library 를 이용해 만든 CoroutineBuilders 는 Job 을 만든다. Deffered 또한 Job Interface 를 상속하고 있다.

## Structured Concurrency

Job 또한 코틀린의 기본원칙인 **Structured Concurrency** 를 따른다. 만약, Structured Concurrency 를 모른다면, 다시 이전포스트를 공부하고 오는게 좋다. 하여튼, 그래서 아래 Job 이 취소되거나, 부모 Job 이 취소될 경우 그 Scope 는 Cancel 될 것이다.

## Job LifeCycle

<img width="428" alt="image" src="https://user-images.githubusercontent.com/57784077/176878164-6c7b99e2-99d9-48e7-be94-8f379a062857.png">

Job 의 LifeCycle 은 위와 같다. 위의 사진은 [Kotlin Coroutines 책](https://www.google.com/search?q=kotlin+coroutine+book&rlz=1C5CHFA_enKR995KR995&oq=Kotlin+Corou&aqs=chrome.2.69i57j0i512j69i59j0i512l2j69i65j69i60l2.4088j0j7&sourceid=chrome&ie=UTF-8) 에서 가져온 사진이다. 대부분의 Coroutine 들은 **"Active"** 상태로 시작하지만, Job 은 특이하게도 **"NEW"** 라는 상태로 시작한다. 그래서 **Lazily 하게 실행**될 수 있다. Job 이 실행되게 되면 상태는 Active 로 바뀌게 된다. 여기서 **Job 이 성공적으로 성공했다면 Completing 상태**로 돌아가고, Children 이 끝나는 걸 대기한다. 모든 **Children 들이 종료되면, Job 은 Completed 상태**가 된다. 만약 **Active 상태 이후로 실패되거나 취소 되면, Cancelling 상태로 변경**된다.

보통 Network I/O 로직에 Coroutine 을 사용하는데 이때 Cacelling 상태가 되었을때, **Session 을 끊는 다던가, Resource 를 반환**한다거나 하는 일들을 수행하면 좋다고 한다. 이것마져 끝나면 **"Cancelled"** 상태가 된다.

## Lazily Start

아까 위에서 설명했듯이 **New 라는 State 가 존재하기에 Job 은 Lazily 하게 실행**할 수 있다.

```kotlin
// launch started lazily is in New state
val lazyJob = launch(start = CoroutineStart.LAZY) { delay(1000) }
println(lazyJob) // LazyStandaloneCoroutine{New}@ADD
// we need to start it, to make it active
lazyJob.start()
println(lazyJob) // LazyStandaloneCoroutine{Active}@ADD
lazyJob.join() // (1 sec)
println(lazyJob) //LazyStandaloneCoroutine{Completed}@ADD
```

## The properties(isActive, isCompleted, isCancelled) have difference value each State

아래 표를 보면 알 수 있듯이, 각 상태마다 속성이 나타내는 값이 다르다. 코루틴을 사용하려면 잘 알고 있는게 좋은 값들이다.

<img width="549" alt="image" src="https://user-images.githubusercontent.com/57784077/176879849-472424e1-329c-41ed-81d3-a6f10e90ec0c.png">

## Parent 의 잡을 상속할까?

우리가 앞에서 배워왔던 것을 생각하면 하나의 CoroutineContext 안의 자식 객체들은 모두 같은 Job 을 가질 것 같기도 하다. 왜냐하면, 모든 CoroutineBuilders 들은 Job 을 만드니까, 하지만 아니다. 그 이유는 CoroutineContext 는 자신만의 Job을 소유하기 때문이다.

```kotlin
fun main(): Unit = runBlocking {
    val name = CoroutineName("Some name") val job = Job()
    launch(name + job) {
        val childName = coroutineContext[CoroutineName] println(childName == name) // true
        val childJob = coroutineContext[Job] println(childJob == job) // false println(childJob == job.children.first()) // true
    } 
}
```

위의 코드를 보면 한가지 사실을 알 수 있는데, Parent 는 Childrens 의 Job 을 모두 소유하고 있다. 그래서 아래와 같은 코드로도 사용이 가능하다.

```kotlin
fun main(): Unit = runBlocking {
    launch {
        delay(1000)
        println("Test1")
    }
    launch {
        delay(2000)
        println("Test2")
    }
    println("All tests are done")

    val children = this.coroutineContext[Job]?.children
    val job = this.coroutineContext[Job]
    val NumbersOfChildren = children?.count()

    println("The Job($job) have $NumbersOfChildren children")
}
```

## Parent 의 Job 을 왜 상속받지 못할까?

근데 왜 Parent Job 을 상속받지 못할까? 그 이유는, Structured Concurrency 가 허용될 수 없기 때문이다. 아래 코드를 한번보자.

```kotlin
fun main(): Unit = runBlocking {
    launch(Job()) { // the new job replaces one from parent
        delay(1000)
        println("Will not be printed")
   }
}
```

이 함수는 어떻게 실행될까? `println` 이 호출될까? 아니 호출되지 않는다. `Job()` 을 통해 **새롭게 생성한 CoroutineContext 는 Parent 와 아무런 연관이 없기 때문에, Parent 는 자식의 실행이 끝마치기를 기다리지 않는다.**

## Job Factory Function

우리는 계속해서 Coroutine Builder 로만 Job 을 생성하는 걸 봐왔었는데, Job 은 사실 다른 방식으로도 생성이 가능하다. 우리가 위에서 사용했던 `Job()` 이라는 Factory Function 을 이용해서도 가능하다. 하지만 이렇게 생성한 Job 은 위에서 예제로 설명했듯이, 어떠한 CoroutineContext 에 속해있던 그것들과 관계가 맺어져있지 않다. **즉, Structured Concurrency 가 되지 않을 수 있다는 것**이다. 그래서 Job 을 Factory Function 으로 만들게 되면 실수하거나, 코드를 제대로 예측하지 못할 수 있다.

```kotlin
suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) { 
        delay(1000)
        println("Text 1")
    }
    launch(job) { 
        delay(2000)
        println("Text 2")
    }
    job.join() 
    println("Will not be printed")
}
```

이 코드의 실행은 어떻게 될까? 이 코드는 끝나지 않는다. 왜냐면, job() 은 일단 Cancellable 이나 Completed 상태가 되지 않는다면 계속해서 Active 상태이다. 그래서 현재 상태에서는 Job 에 주어진 body 가 없으므로 계속해서 Active 상태로 존재한다. 이 Code 를 끝내기 위해서는 어떻게 해야 할까?

```kotlin
suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) {
        delay(1000)
        println("Job 1 = $job")
        println("Text 1")
    }
    launch(job) {
        delay(2000)
        println("Job 2 = $job")
        println("Text 2")
    }
    job.children.forEach {
        it.join()
    }
    println("Will be printed")
}
```

이렇게 작성하면 끝난다. 근데 하나 짚고 넘어가야 할게 있다. Structured Concurrency 를 완전히 안준수하는 것일까? 과연 아래코드는 어떻게 될까?

```kotlin
suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) {
        delay(1000)
        throw IllegalArgumentException("")
    }
    launch(job) {
        delay(2000)
        println("Job 2 = $job")
        println("Text 2")
    }
    job.join()
    println("Will not be printed")
}
```

아래 코드는 코드가 종료된다. 그 이유는 Job 의 children 에서 Exception 이 전파됬기 때문이다. 따라서 위의 코드를 보면 완전히 Structured Concurrency 를 준수하지 않는 것은 아니다.

### Job.completed()

이 메소드를 사용하게 되면, job 의 children 들은 completed 가 될때까지 동작하며, 호출한 이후로는 다른 코루틴은 참가할 수 없게 된다. completed 의 result 가 true 면 이 job 이 끝난것이고, false 면 이미 끝난 것이다. 일단 예제를 보면 훨씬 더 이해가 잘가니 아래 예제를 보자.

일단 정상적인 케이스는 아래와 같다.

```kotlin
suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) {
        delay(1000)
        println("Text 1")
    }
    launch(job) {
        delay(2000)
        println("Job 2 = $job")
        println("Text 2")
    }
    println("Is Complete? ${job.complete()}")
    job.join()
    println("Will be printed")
}

// true
// Text 1
// Text 2
// Will be printed
```

그럼 completed 호출 후에 다른 Children 하나를 attach 해보자.

```kotlin
suspend fun main(): Unit = coroutineScope {
    val job = Job()
    launch(job) {
        delay(1000)
        println("Text 1")
    }
    launch(job) {
        delay(2000)
        println("Job 2 = $job")
        println("Text 2")
    }
    println("Is Complete? ${job.complete()}")

    job.join()

    launch(job) {
        println("Text 3")
    }

    println(job.children.count()) // 0

    job.join() // not executed
    
    println("Will not be printed")
}

//Is Complete? true
//Text 1
//Job 2 = JobImpl{Completing}@3a71b63e
//Text 2
//0
//Will not be printed
```

위의 실행결과를 봐서 알겠지만 전혀 실행되지 않는다.