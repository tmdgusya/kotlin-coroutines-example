@startuml

title Kotlin Collection Diagram

interface Iterator<out T> {
    +operator next(): T
    +operator hasNext(): Boolean
}

interface MutableIterator<out T> {
    +remove(): Unit
}

interface ListIterator<out T> {
    +next(): T
    +hasNext(): Boolean
    +hasPrevious(): Boolean
    +previous(): T
    +nextIndex(): Int
    +previousIndex(): Int
}

interface MutableListIterator<T> {
    +next(): T
    +hasNext(): Boolean
    +remove(): Unit
    +set(element: T): Unit
    +add(element: T): Unit
}

interface Iterable<out T> {
    +operator iterator(): Iterator<T>
}

interface MutableIterable<out T> {
    +iterator(): MutableIterator<T>
}

interface Sequence<out T> {
    +operator iterator(): Iterator<T>
}

interface Collection<out E> {
    +size: number
    +isEmpty(): boolean
    +iterator(): Iterator<E>
    +contains(element: @UnsafeVariance E): Boolean
    +containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

interface MutableCollection<E> {
    +iterator(): MutableIterator<E>
    +add(element: E): Boolean
    +addAll(elements: Collection<E>): Boolean
    +remove(element: E): Boolean
    +removeAll(elements: Collection<E>): Boolean
    +retainAll(elements: Collection<E>): Boolean
    +containsAll()
    +clear(): Unit
}

interface List<out E> {
    +size: number
    +isEmpty(): Boolean
    +contains(element: @UnsafeVariance E): Boolean
    +iterator(): Iterator<E>
    +containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    +get(index: Int): E
}

Iterator *-- Iterable
Iterator *-- Sequence
Iterator <|-- MutableIterator
MutableIterator *-- MutableIterable
Iterator <|-- ListIterator
Iterator <|-- MutableListIterator
MutableIterator <|-- MutableListIterator

Iterable <|-- Collection
Iterable <|-- MutableIterable
Collection <|-- MutableCollection
MutableIterable <|-- MutableCollection 

Collection <|-- List
@enduml

