Kotlin Coroutines 에 대한 공부를 최근에 많이 하고 있는데 하면서 느껴지는 점은 Thread 간의 Context Switching 에 관한 내용을 Heap 으로 관리할께 라는 느낌이 강하게 들었다. 예전에는 어떻게 Context Switching 을 적은비용으로 한다는 거지 싶었는데, 이제는 대략적으로 이해가 간다. 언제 글을 정리할까 했다가 오늘 천천히 정리해보려고 한다.

## 기존 Context Switching 의 문제
일단 아주 간단하게 설명하겠다. 어차피 지금 글에서 이 내용이 중요한건 아니니까. 각 Thread 는 Local Stack 등 자신만의 데이터를 가지고 있는데, 다른 Thread 와 Switching 해야 할때 자신의 작업정보를 넘겨주어야 한다. 즉, Process Switching 보다는 싼 비용이지만, 그럼에도 불구하고 Switching 해야 할때 주고 받아야 하는 정보들이 존재한다. 이것이 Context Switching 의 Cost 라고 할 수 있다. 누군가는 아주 당연하게 생각할 수 있다. 당연히 정보를 넘겨 받아야 하는데 당연히 비용이 드는게 아니냐..? 라고 할 수 있다. 하지만 코루틴에서는 조금 다르게 풀었다.

## Kotlin Coroutine 의 해결 방안
코틀린 Coroutine 은 Pointer 방식을 해결방안으로 떠올렸다. 즉, Heap 은 Thread 가 공유하고 있는 영역이니까, **TCB (Thread Context Block) 에 있는 단위를 Heap 까지 끌어올리고, Thread 가 이 메모리 주소만 참조하게 하면 안될까?** 이게 Kotlin Coroutines 에서, Context Switching 비용을 줄인 방법이다. Kotlin 은 이를 위해서 Continuation 이라는 객체를 도입했다.

## Continuation
Kotlin 에서 suspend 된 함수들을 보면 컴파일 시 아래와 같이 변경된다.

```kotlin
suspend fun getUser(): User?
suspend fun setUser(user: User)
suspend fun checkAvailability(flight: Flight): Boolean

// under the hood is
fun getUser(continuation: Continuation<*>): Any?
fun setUser(user: User, continuation: Continuation<*>): Any fun checkAvailability(
   flight: Flight,
continuation: Continuation<*> ): Any
```

보면 **Continuation 이라는 Type 을 Parameter 에서 넘겨 받을 수 있고, Return Type 또한 Any 로 변경**되어 있다. 이 이유는 Kotlin Suspend function 이 Suspend 되었을때, **COROUTINE_SUSPENDED** 라는 특별한 marker 를 리턴하기 때문이다.

```kotlin
suspend fun myFunction() { 
    println("Before") 
    delay(1000) // suspending println("After")
    println("After")
}
```

위의 함수를 Java 로 Decompile 한 코드를 보면 아래와 같다. 사실 실제로 컴파일하면 아래코드가 아닌데 조금 나만의 방식대로 수정했다. 
어찌 됬든 이해하는게 더 중요하다. 이해하고 직접 코드를 찾아보고 싶다면 찾아봐도 좋다.

```kotlin
fun myFunction(continuation: Continuation<Unit>): Any {

    val continuation = continuation

    switch((continuation).label) {
        case 0:
            ResultKt.throwOnFailure($result);
            System.out.println("Before");
            (continuation).label = 1; // 다음에 들어올때는 label 1
            if (DelayKt.delay(1000L, (Continuation)$continuation) == COROUTINE_SUSPENDED) {
                return COROUTINE_SUSPENDED;
            }
            break;
        case 1:
            ResultKt.throwOnFailure($result);
            break;
        default:
            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
    }

    System.out.println("After");
    return Unit.INSTANCE;
}
```

### Label

이 코드를 차근차근 하나씩 분석해보자. `continuation.label` 은 무엇일까? 우리가 PC Register 를 공부하다 보면, `code 0` 과 같은 부분을 마주한다. 
PC Register 는 다음으로 Fetch 될 명령어의 주소를 가지고 있다. 즉, Kotlin Coroutine 에서도 복귀했다가 돌아올때, **어느지점 부터 실행해야 하는지를 알아야 하는데 
이를 Label 로 관리**한다. 보면 Label 이 0 인경우 1인 경우에는 어떤 걸 실행해야해에 관한 Guide 가 Switch 문 안에 적혀있다.

### COROUTINE_SUSPENDED

COROTINE_SUSPENDED 는 무엇일까? 진짜 말 그대로 **Suspend Function 이 suspend 되었다는 뜻**이다. NonBlocking Structure 를 공부해봤다면 왜 Return 을 하는지 알 수 있을 것이다. Thread 에게 함수가 끝난것처럼 속이기 위함이다. 여하튼 suspend 되었다면 COROTINE_SUSPENDED 를 리턴하고, **다시 Resume 될때 label 1 로 진행되게 된다. 왜냐하면 label 을 1로 바꿔줬기 때문**이다. 이렇게 Suspend 에서 Return 을 해주기 때문에 Thread 를 Release 할 수 있는 이유이다. 사실 이건, 다른 언어에서도 많이 차용한다. `-1` 을 리턴한다거나 Exception 를 리턴한다거나 등의 방식을 많이들 이용하는 것으로 알고 있다.

### Store state

그렇다면 상태를 어떻게 저장할까? 예를 들면 하나의 Function 안에서 Thread 라면 Local Stack 에 Variable 의 값을 저장하고 있을텐데, 이를 어떻게 다른 Thread 에게 알려줄 수 있을까? 이것도한 똑같다. `Continuation` 에 저장한다. 아래 코드를 한번 보자.

```kotlin
suspend fun myFunction() {
    println("Before")
    var counter = 0
    delay(1000) // suspending
    counter++
    println("Counter: $counter")
    println("After")
}
```

```kotlin
fun myFunction(continuation: Continuation<Unit>): Any {

    val continuation = continuation

    switch((continuation).label) {
        case 0:
         ResultKt.throwOnFailure($result);
         System.out.println("Before");
         counter = 0;
         (continuation).counter = counter;
         (continuation).label = 1;
         if (DelayKt.delay(1000L, (Continuation)$continuation) == var5) {
            return var5;
         }
         break;
      case 1:
         counter = (continuation).counter;
         ResultKt.throwOnFailure($result);
         break;
      default:
         throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
    }

      ++counter;
      String var2 = "Counter: " + counter;
      System.out.println(var2);
      System.out.println("After");
      return Unit.INSTANCE;
}
```

Decompile 된 코드를 보면 `continuation` 에 counter 값을 저장하고 있음을 알 수 있다. 

## Call Stack Problem

위의 코드를 보면서 한가지 이상한점을 느낄 수 있다. 이렇게 단순하게 Return 해서 Thread 를 Release 해버린다면 Suspend 된 상태에서 Main Thread 가 끝나서 종료되버리는 것 아니야? 만약 `a` 에서 `b` 함수를 호출하고 `b` 의 결과를 이용해야 한다면, `a` 함수는 `b` 함수가 끝나고 나서 실행되야 한다. 보통 언어에서 이를 Call Stack 을 이용해서 처리한다. 하지만 Coroutine 에서는 이를 Continuation 을 통하여 처리한다.

```kotlin
suspend fun a() {
    val user = readUser() // suspend
    b() // suspend
    b() // suspend
    b() // suspend
    println(user)
}

suspend fun b() {
    for (i in 1..10) {
        c(i)
    }
}

suspend fun c(i: Int) {
    delay(i * 100L)
    println("Tick")
}

suspend fun readUser(): String {
    return "user"
}
```

이 코드를 보면 `a()` 에는 `readUser()` 를 포함해서 호출되는 `suspend()` 함수가 많다. 이 모든 부분들이 실행되야 하므로 Decompile 해보면 아래와 같은 코드가 나온다.

```kotlin
fun a(continuation: Continuation<Unit>): Any {
    Object $continuation;

    String user;
    label40: {
        Object COROUTINE_SUSPENDED;
        label39: {
            label38: {
                Object $result = (continuation).result;
                Object readUserResult;
                switch((continuation).label) {
                case 0: // 첫번째 실행
                    ResultKt.throwOnFailure($result);
                    (continuation).label = 1;
                    readUserResult = readUser((Continuation)$continuation);
                    if (readUserResult == COROUTINE_SUSPENDED) {
                        return COROUTINE_SUSPENDED;
                    }
                    break;
                case 1: // 두번째 실행
                    ResultKt.throwOnFailure($result);
                    readUserResult = $result;
                    break;
                case 2: // 세번째 실행
                    user = (String)(continuation).user;
                    ResultKt.throwOnFailure($result);
                    break label38;
                case 3: // 4번째 실행
                    user = (String)(continuation).user;
                    ResultKt.throwOnFailure($result);
                    break label39;
                case 4: // 5번째 실행
                    user = (String)(continuation).user;
                    ResultKt.throwOnFailure($result);
                    break label40;
                default:
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                user = (String)readUserResult;
                (continuation).user = user;
                (continuation).label = 2;
                if (b((Continuation)$continuation) == COROUTINE_SUSPENDED) { // b 실행
                    return COROUTINE_SUSPENDED;
                }
            }

            (continuation).user = user;
            (continuation).label = 3;
            if (b((Continuation)$continuation) == COROUTINE_SUSPENDED) { // b 실행
                return COROUTINE_SUSPENDED;
            }
        }

        (continuation).user = user;
        (continuation).label = 4;
        if (b((Continuation)$continuation) == COROUTINE_SUSPENDED) { // b 실행
            return COROUTINE_SUSPENDED;
        }
    }

    System.out.println(user);
    return Unit.INSTANCE;
}
```

위의 코드를 보면 기존 CallStack 처럼 `a -> b -> c` 순으로 실행함을 확인할 수 있다. 다만 반대로 Suspending 됬다가 올라올때는 `c -> b -> a` 순으로 **resume** 된다.




## 공부해보면서 느낀 점.

Kotlin Coroutine 에 대한 어느정도 청사진이 잡힌것 같다. 하지만 이 책을 읽으면서 느낀건, 번역하기 꽤나 어려워서 이게 한국시장에 늦게 들어올것 같다는 생각도 많이 들었다. 그럼에도 불구하고 읽어보면 좋은 책일 것 같다. 이책을 빨리 다 읽고, 코틀린 동시성 프로그래밍도 한번 읽어봐야겠다.

----
**잡답**

NodeJS 쪽에도 CLS 라는 Library 가 있는데, 이건 Suspend 되는 건 아니지만 Continuos 한 Object 를 계속해서 넘겨 받으면서 
하나의 Function 에서 시작된 Callback Pattern 안에서 주고 받으면서 사용이 가능하다. Kotlin Coroutines 의 Function 에서 Continuation 을 주고 받는걸 보면서 약간 비슷하네 싶긴했다.

