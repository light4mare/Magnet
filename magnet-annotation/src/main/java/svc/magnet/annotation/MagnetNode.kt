package svc.magnet.annotation

interface MagnetNode<T> {
    fun onReplace(source: T)
    fun <V: Any> observe(vararg key: String, block: Block<V>)
}