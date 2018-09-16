package com.svc.magnet

abstract class ProxySource<T: Any> {
    // 防止回环
    protected var isSourceChange = false

    fun onNext(t: T) {
        if (isSourceChange) {
            isSourceChange = false
        } else {
            onObserve(t)
        }
    }

    abstract fun bindMagnet(magnet: Magnet<T>)
    abstract fun onObserve(t: T)
}