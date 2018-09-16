package com.svc.magnet

import io.reactivex.subjects.PublishSubject
import svc.magnet.annotation.Block
import svc.magnet.annotation.MagnetNode

/**
 * 通过内置监听器实现双向绑定
 */
@Suppress("UNCHECKED_CAST")
class NodeMagnet<T : MagnetNode<T>> : Magnet<T>() {
    private var value: T? = null

    private val waitingMap by lazy { mutableMapOf<List<String>, Block<out Any>>() }

    override fun value(t: T?, check: Boolean) {
        if (check && value == t) return

        t?.let {
            if (value == null) {
                value = it
                for (entry in waitingMap) {
                    value?.observe(key = *entry.key.toTypedArray(), block = entry.value)
                }
            }
            value?.onReplace(it)
            subject.onNext(it)
        }
    }

    fun <V : Any> observe(vararg key: String, block: (v: V) -> Unit): NodeMagnet<T> {
        return observe(*key, block = object : Block<V> {
            override fun onNext(v: V) {
                block(v)
            }
        })
    }

    fun <V : Any> observe(vararg key: String, block: Block<V>): NodeMagnet<T> {
        value?.apply {
            this.observe<V>(*key, block = block)
        } ?: waitingMap.put(key.toList(), block)
        return this
    }

    fun <V> observe(block: Block<V>) {
        subject.subscribe {
            block.onNext(it as V)
        }
    }
}