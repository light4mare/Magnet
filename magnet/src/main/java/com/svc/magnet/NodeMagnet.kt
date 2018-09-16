package com.svc.magnet

import io.reactivex.subjects.PublishSubject
import svc.magnet.annotation.Block
import svc.magnet.annotation.MagnetNode

/**
 * 通过内置监听器实现双向绑定
 */
class NodeMagnet<T : MagnetNode<T>> {
    private var value: T? = null

    private val subject = PublishSubject.create<T>()
    private val waitingMap by lazy { mutableMapOf<List<String>, Block<out Any>>() }

    fun value(t: T?, check: Boolean = false) {
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

        //==============================================================================================

        //==============================================================================================
    }

    fun getValue(): T? {
        return value
    }

    fun observe(block: (t: T) -> Unit) {
        subject.subscribe {
            block(it)
        }
    }

    fun <V> map(block: (v: V) -> Unit) {
        val map = object : MapNet<T, V>() {

        }

        subject.subscribe {
            map.onNext(it, block)
        }
    }

    //==============================================================================================
    fun <V : Any> observe(vararg key: String, block: (v: V) -> Unit) {
        observe<V>(*key, block = object: Block<V>{
            override fun onNext(v: V) {
                block(v)
            }
        })
    }

    fun <V : Any> observe(vararg key: String, block: Block<V>) {
        value?.apply {
            this.observe<V>(*key, block = block)
        } ?: waitingMap.put(key.toList(), block)
    }

    fun <V> observe(block: Block<V>) {
        subject.subscribe {
            block.onNext(it as V)
        }
    }

    //==============================================================================================
}