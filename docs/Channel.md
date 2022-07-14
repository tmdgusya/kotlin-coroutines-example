# Channel

채널 API 는 Coroutine 간의 상호 통신용으로 사용된다. Coroutine 과 Coroutine 사이에서 Channel 을 이용하여 어떠한 데이터든 주고 받을 수 있다. 

## 특징

- 채널의 경우 Producer 와 Consumer 갯수에 제한이 없다.
- Channel 은 아래 두개 interface 를 구현하고 있다.
    - SendChannel
    - ReceivceChannel

## Receive 가 또는 Send 가 Suspend 되는 경우

- Recevice 는 Channel 에서 Element 를 받아오는 함수이다. 근데 만약에 Channel 에 Element 가 없다면 어떻게 될까? 
해당 Coroutine 은 가져올 수 있는 Element 가 있을때 까지 suspend 된다.

- Send 가 Suspend 되는 경우는 Channel 의 capacity threshold 에 도달했을때 보낼수 없으므로 suspend 된다. 

만약 non-suspending function 을 사용하고 싶다면 `trySend` 와 `tryReceive` 를 이용하면 된다.

## 예시 코드

```kotlin
suspend fun main(): Unit = coroutineScope {
    // create Channel
    val channel = Channel<Int>()

    launch {
        repeat(5) { index ->
            delay(1000)
            println("Producing Index")
            channel.send(index) // send data to channel
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index") // receive data from channel
        }
    }
}
```

위의 코드를 보면 Int Channel 을 열고, 거기에 하나의 Coroutine 이 Data 를 Send 하고, 하나의 코루틴은 Data 를 받아옴을 알 수 있다. 
Channel 을 사용할때 한가지 위험한 점이 있는데 **예기치 못하게 Channel 이 닫히는 경우**이다. 아래 코드를 한번 보자

```kotlin
suspend fun main(): Unit = coroutineScope {
    val channel = Channel<Int>()

    launch(SupervisorJob()) {
        repeat(5) { index ->
            delay(1000)
            println("Producing Index")
            if (index == 3) {
                throw IllegalArgumentException("")
            }
            channel.send(index)
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index")
        }
    }
}
```

위 코드 처럼 index 가 3이 됬을때 produce coroutine 은 exception 을 발생시키고 죽는다. receive channel 은 produce channel 의 Data 를 기다리며 suspend 된다. 이 상태로 영원히 종료되지 않는다. 우리가 이 코드를 종료시키기 위해선 어떻게 해야할까? 바로 channel 을 close 해줘야 한다. 일단은 아주 간단하게 아래처럼 코드를 작성해 볼 수 있을 것이다.

```kotlin
suspend fun main(): Unit = coroutineScope {
    val channel = Channel<Int>()

    launch(SupervisorJob()) {
        repeat(5) { index ->
            try {
                delay(1000)
                println("Producing Index")
                if (index == 3) {
                    throw IllegalArgumentException("")
                }
                channel.send(index)
            } catch (e: Exception) {
                channel.close()
                throw e
            }
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index")
        }
    }
}
```

하지만 이렇게 **try..catch 를 계속 써줘야 하는건 불편하기도 할뿐더러, 사용자가 까먹기에도 쉽다.** 그래소 코틀린에서는 좀 더 간편한 기능을 제공한다. 바로 Builder 를 제공하는 것인데, CoroutineScope.produce 를 제공해준다.

```kotlin
suspend fun main(): Unit = coroutineScope {
    val channel = produce {
        repeat(5) { index ->
            delay(1000)
            println("Producing Index")
            if (index == 3) {
                throw IllegalArgumentException("")
            }
            channel.send(index)
        }
    }

    launch {
        repeat(5) {
            val index = channel.receive()
            println("Received Index : $index")
        }
    }
}
```

위와 같이 Code 를 작성하면 알아서 Close 를 해주게 된다. 이렇게 해줄수 있는 이유는 Exception 이 발생하게 되면 CompletedExceptionally 메소드를 호출하고, 결국 해당 메소드가 이어져 아래 메소드를 호출하게 된다. 더 궁금하면 직접 코드를 까보는게 좋다.

```kotlin
override fun onCancelled(cause: Throwable, handled: Boolean) {
    val processed = _channel.close(cause)
    if (!processed && !handled) handleCoroutineException(context, cause)
}
```

## Channel Types

코루틴에는 capacity size 별로 4 가지의 Channel Type 이 있다.

- **Unlimited** : 채널의 capacity. 즉, buffer 가 limit 이 없다는 뜻이다. Unlimited 를 사용하게 되면 sender 는 절대 suspend 되지 않는다.
- **Buffured** : 채널의 capacity 가 64로 결정되거나 kotlinx.coroutines.channels.defaultBuffer 라는 JVM 환경변수 값으로 설정될수도 있다.
- **Rendezvous(default)** : channel 의 capacity 가 0 또는 Rendezvous 라고 설명되어 있는데, 쉽게 얘기하면 sender 가 data 를 보내면 receivce 가 받기 전까지 sender 도 data 를 보내지 않는다고 생각하면 편하다. 아래 순서같다고 생가갛면 된다.
    -   ```
        Producing Index
        Received Index : 0
        Producing Index
        Received Index : 1
        Producing Index
        Received Index : 2
        ```    
- **Conflated** : Channel 의 BufferSize 가 1 로 설정되어 있는 것이다. 근데, 이 Conflated 는 Receiver() 의 속도가 느리면 큰일 날 수 있는데, **이전의 데이터를 날리고 새로운 데이터가 들어가버리는 구조이다. 즉, 넣는 순간 그 데이터가 Channel 의 유일한 데이터**가 된다. 

## Fan-out

<img width="869" alt="image" src="https://user-images.githubusercontent.com/57784077/179018783-6338676d-2eb0-497f-ba9e-56015c6218b5.png">

위 사진 처럼 하나의 Producer 에 여러 Receiver 를 두는 경우, 똑같은 데이터를 중복으로 또 받는 것이 아닌 같은 큐에 있는 데이터를 서로 Consume 하는 구조이다. Kafka Producer 1대에 Consumer 2 대라고 생각하면 편하다. 근데 여기서 신기한건 **Fairly 하게 분배된다는 것이다. 그 이유는 Channel 이 FIFO 구조**로 되어 있기 때문이다.

## Fan-in

<img width="886" alt="image" src="https://user-images.githubusercontent.com/57784077/179019487-895c52b8-159e-43c9-92a1-f9dfc2d047b5.png">

이건 위와 반대로 여러명의 Producer 에 Consumer 가 1대인 경우이다.

## 후기 

간단하게 개념정리만 했다. 전 회사에서는 고루틴으로 간단하게 소켓 테스트하는걸 만들었었다. Go 에도 Channel 개념이 있어서 성능테스트 루틴이 Report Consumer 에게 Channel 을 통해서 주기적으로 Report 를 갱신해주는 코드를 작성했었다. 코루틴으로도 한번 짜볼까 고민됬다. DSL 을 통해서 성능 테스트 규모를 작성하고 하면 참 편할텐데.