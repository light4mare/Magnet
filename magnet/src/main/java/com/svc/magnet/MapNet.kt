package com.svc.magnet

open class MapNet<T, V> {
    private var mapNet: MapNet<T, V>? = null

    open fun onNext(t: T, block: (v: V) -> Unit):V? {
        if (mapNet != null) {
            val result = mapNet?.onNext(t){}
            result?.let(block)
        }

        return null
    }
    fun map(block: (t: T) -> V) {
        mapNet = object : MapNet<T, V>(){
            override fun onNext(t: T, block: (v: V) -> Unit): V? {
                return block(t)
            }
        }
    }
}