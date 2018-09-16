package com.svc.magnet

import io.reactivex.subjects.PublishSubject

/**
 * 通过内置监听器实现双向绑定
 */
open class Magnet<T : Any> {
    private var value: T? = null

    protected val subject = PublishSubject.create<T>()

    open fun value(t: T?, check: Boolean = false) {
        if (check && value == t) return

        t?.let {
            value = t
            subject.onNext(it)
        }
    }

    fun getValue(): T? {
        return value
    }

    fun observe(block: (t: T) -> Unit) {
        subject.subscribe {
            block(it)
        }
    }

    fun <V: Any> map(block: (t: T) -> V): Magnet<V> {
        val magnet = Magnet<V>()
        subject.subscribe {
            magnet.value(block(it))
        }
        return magnet
    }

    fun filter(block: (t: T) -> Boolean): Magnet<T>{
        val magnet = Magnet<T>()
        subject.subscribe {
            if (block(it)) {
                magnet.value(it)
            }
        }
        return magnet
    }
}