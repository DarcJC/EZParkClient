package pro.darc.park.utils

import java.util.concurrent.CopyOnWriteArraySet

// inherit it from any custom data class
interface EventArgs

typealias Observer<T> = (sender: Any, eventArgs: T) -> Unit

class EventHandler<T: EventArgs> {
    private val subscribers = CopyOnWriteArraySet<Observer<T>>()

    fun add(subscriber: Observer<T>) = subscribers.add(subscriber)

    operator fun plusAssign(subscriber: Observer<T>) {
        add(subscriber)
    }

    fun remove(observer: Observer<T>) = subscribers.remove(observer)

    operator fun minusAssign(observer: Observer<T>) {
        remove(observer)
    }

    operator fun invoke(publisher: Any, args: T) {
        subscribers.forEach { it.invoke(publisher, args) }
    }
}
