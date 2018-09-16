package svc.magnet.annotation

interface Block<V>{
    fun onNext(v: V)
}