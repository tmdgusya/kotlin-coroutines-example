# Kotlin Collection 과 Hot Data Stream And Cold data stream

Kotlin Collection 은 Java Collection 과 무엇이 다를까? 일단 Java 를 쓰다가 Kotlin 으로 넘어온 사람이 이 Collection Class Diagram 을 봤을때는 사뭇 Java 와 약간 차이가 있음을 느낄 것이다. 일단 구체적인 설명은 Diagram 을 보고 하는게 편하니, 아래 Diagram 을 보도록 하자. PlantUML 로 직접 작성했다.

![image](https://user-images.githubusercontent.com/57784077/179342179-bd0f3f63-2fa0-4ab1-a1d8-14917541d857.png)

## Iterator

일단 위의 Collection Class Diagram 을 살펴보면 기본적으로 **최상위 Interface 가 Iterator 임**을 알 수 있다. 왜 Kotlin 은 이런 구조를 채택했을까? 일단 Iterator 를 상속하게 되면 아래와 같은 이점을 얻을 수 있다.

- **향상된 for 문 문법 지원**
- **forEach 이용 가능**

이것 말고도 다른 이점을 알기 위해선 우리는 **Sequence(Cold) 의 Class Diagram** 또한 알아야 한다.

## Sequence

![image](https://user-images.githubusercontent.com/57784077/179342481-159dd9ed-c968-4764-8947-12f2d44cbbe4.png)

Sequence 또한 Diagram 에 추가했다. Sequence 안에서 **Iterator 를 Composition 하여 사용**하고 있다. Sequence Inline 함수 하나를 살펴보자

```kotlin
public inline fun <T> Sequence(crossinline iterator: () -> Iterator<T>): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = iterator()
}
```

Sequence Function 에 iterator 를 넣어주면 Seqence 를 생성하여 사용할 수 있다. 우리는 위의 Class Diagram 을 보면 Collection 또한 `iterator()` 를 가지고 있음을 알 수 있다. 즉, 우리가 List 를 이용해서 Seqeunce 를 생성할 수 있다는 것이다. Kotlin 에서는 이를 `asSequence()` 를 통해 지원한다.

```kotlin
public inline fun <T> Sequence(crossinline iterator: () -> Iterator<T>): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = iterator()
}
```

이게 어떤 장점이 있을까? 바로 List 에서 Sequence 로 전환이 가능하고, Sequence 에서 연산을 통한걸 다시 List 로 전환할 수도 있다는 것이다. 그래서 **Kotlin 에서는 Hot data Structure 를 써도, 중간에 연산을 해야한다면 Cold Stream 으로 계산하기 쉽다는 것**이다.

## Hot vs Cold

일단 Hot 과 Cold 에 관한 내용은 자료구조나 연산에 관한 내용을 읽게되면 꼭 나오는 단어이므로 한번 쯤 정리하고 가겠다. 보통 Java/Kotlin 진영에서 자주 쓰이는 Data Structure 로 구분해보자면 아래와 같다.

|Hot|Cold|
|:---:|:---:|
|Collection(List, Set, Map)|Sequence, Stream|
|Channel|Flow|

보통 **Hot data stream** 의 경우 쉽게 얘기해서 필요하지 않을때도 메모리에 올라가 있는다고 생각하면 편하다. 그래서 Hot Data Stream 을 통해서 **연산하게 되면 연산하는 그 즉시 연산이 일어난다.** 이게 한국말로 하려니 참어려운데.. Eager loading 을 생각하면 된다. **Cold Data Stream 은 그와 반대로 보통 Terminal Operation 이 일어나는 순간에 연산을 시작**한다. 즉, Lazy 하다는 것이다.

근데 이 두개의 연산의 차이를 모른다면 어떤 차이가 있길래 이것이 유의미한 것인가? 라는 생각을 할 수도 있다. 일단 아래  코드를 보자.

```kotlin
fun main() {
    val collection = listOf(1, 2, 3)

    collection
        .map { it * 2 }
        .filter { it <= 10 }
        .map { it * it }
        .filter { it % 2 == 0 }
        .forEach { println("List = $it") }

    collection
        .asSequence()
        .map { it * 2 }
        .filter { it <= 10 }
        .map { it * it }
        .filter { it % 2 == 0 }
        .forEach { println("Sequence = $it") }
}
```

하나는 Collection Method 로 그대로 연산을 진행하는 경우이고, 하나는 Sequence 로 변환후에 연산을 진행하는 경우이다. 

## Hot Data Stream

일단 **Hot data Stream** 부터 살펴보자. `map` 과 `filter` 함수를 보자.

```kotlin
public inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)
}

public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}
```

**map 과 filter function** 은 보면 알겠지만 intermediate 연산을 위해서 ArrayList() 를 생성한다. 공간 복잡도 측면으로 생각했을때 Map Function 의 경우 **N 개의 Data가 들어오면 N 개의 메모리를 한번 더 차지해야 하는 구조**라고 볼수도 있는 것이다. Filter 의 경우도 ArrayList 를 하나더 생산하게 된다. 즉, intermediate 연산이 많으면 많을수록 N Size 의 ArrayList 를 생산하게 된다. 즉, 공간복잡도 측면에서 유리한가? 라는 생각을 할 수 있는 것이다.

## Cold Data Stream

Cold Data Stream 은 어떨까? 한번 살펴보자. Cold Data Stream 을 시작하기 위해 Collection 에서 호출하는 `asSequence` 메소드를 보자.

```kotlin
public fun <T> Iterator<T>.asSequence(): Sequence<T> = Sequence { this }.constrainOnce()
```

일단 Sequence 를 만드는 과정에서 기존의 Sequence 를 받아서 Sequence 를 만들게 된다. 이 과정에서 O(n) 정도의 공간복잡도가 들 수 있을 것이다. (정확하지 않다.. 정확하게 아시는 분들은 댓글로 부탁드립니다 ㅎㅎ..) 그리고, 중요한 `map` 과 `filter` function 을 한번 살펴보자.

```kotlin
public fun <T, R> Sequence<T>.map(transform: (T) -> R): Sequence<R> {
    return TransformingSequence(this, transform)
}

internal class TransformingSequence<T, R>
constructor(private val sequence: Sequence<T>, private val transformer: (T) -> R) : Sequence<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = sequence.iterator()
        override fun next(): R {
            return transformer(iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }

    internal fun <E> flatten(iterator: (R) -> Iterator<E>): Sequence<E> {
        return FlatteningSequence<T, R, E>(sequence, transformer, iterator)
    }
}
```

Sequence 객체를 받아서 `next()` 를 호출할때마다, 우리가 `map(transform(ele: T))` 로 넘겨준걸 실행하게 된다. 즉, 위의 연산을 살펴보았을때 map 에서의 공간복잡도 적으로 늘어나는 측면은 딱히 보이지는 않는다. 그리고 또한 `next()` 가 호출될때 `transform()` 연산이 일어난 다는 것도 알수 있을 것이다. 이러한 이유로 lazy 연산이 가능한 것이다. 그럼 Filter 연산을 한번 살펴보자.

```kotlin
internal class FilteringSequence<T>(
    private val sequence: Sequence<T>,
    private val sendWhen: Boolean = true,
    private val predicate: (T) -> Boolean
) : Sequence<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator()
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item) == sendWhen) {
                    nextItem = item
                    nextState = 1
                    return
                }
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem
            nextItem = null
            nextState = -1
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}
```

Filter 연산 또한 next 를 호출할때 쯤에야 `calcNext()` 를 통해서 우리가 넘긴 `predicate()` 연산을 시행한다는 점을 알 수 있다. 그럼 마지막으로 수행되는 forEach 연산을 보자.

```kotlin
public inline fun <T> Sequence<T>.forEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}
```

forEach 연산에서는 향상된 for 문을 사용하고 있다. 즉, **Iterator 의 next() 를 호출하고 있다는 것**이다. 아까 위에서 봤던 map 과 filter 는 next() 가 호출될때 연산된다고 했다. 즉, **forEach 는 여태까지 계산되고 있지 않았던 intermediate 연산을 수행시키는 terminal operation** 이다. 이 개념을 몰랐다면 잘 알아두는게 좋다. 이렇기 때문에 **Sequence 는 하나하나씩 next() 로 element 를 넘겨가면서 그게 필요한(on-demand) 순간 연산을 진행**한다. 그렇기 때문에 중간에 새로운 자료구조를 만들 필요도 없는 것이다. 이러한 이유로 Sequence 는 infinite 한 Stream 을 생성할 수 있다.

## 메모리 비교

위에서 봤을때 당연하게도 Sequence 가 메모리를 덜 차지할 것 같다. 한번 이를 실험으로 비교해보자.

```kotlin
fun main() {
    val collection = mutableListOf<Int>()

    for (i in 0 until 1000000000) collection.add(i)

    collection
        .map { it * 2 }
        .filter { it <= 10 }
        .map { it * it }
        .filter { it % 2 == 0 }
        .forEach { println("List = $it") }

}
```

Data 가 1000만건 있을때 Hot Data Stream 으로 연산을 진행해보겠다.

<img width="1644" alt="image" src="https://user-images.githubusercontent.com/57784077/179344529-11f507c8-a4b1-4d54-9cd9-7bb414da9ec9.png">


Data 가 1000만건 있을때 Cold Data Stream 으로 연산을 진행해보겠다.

<img width="1678" alt="image" src="https://user-images.githubusercontent.com/57784077/179344490-bf3e5bab-1958-4ed0-be8d-c955ef01aed8.png">

보면 사용하는 Heap 의 용량차이가 거의 2배 정도 차이가 나는 것을 눈으로 확인할 수 있다. 예전에 와탭의 JVM 메모리 강의를 본적이 있는데 결국 메모리를 잘쓰기 위해서는 메모리를 적게 쓰는 것이 중요하다. 라고 하셨는데 이렇게 List 에 중간연산이 많다면 Seqeunce 를 이용하는 코드로 바꿔보면 엄청은 아니지만 눈에 약간은 보일정도로 메모리를 아끼는 방법으로 코딩을 할 수 있을 것이다.

## Github

