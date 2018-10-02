package svc.magnet.model

import svc.magnet.annotation.Ignore
import svc.magnet.annotation.Init
import svc.magnet.annotation.Source

@Source
class Inner {
    var name: String? = ""
    var age: Int = 0
    var mInt: Int = 0
    var mFloat: Float = 0f
    var mDouble: Double = 0.0
    var mLong: Long = 0L
    var mByte: Byte = 0
    var mChar: Char = '\u0000'
    var mBoolean: Boolean = false
}

@Source()
class Outer {
    @Init("initialized")
    var account: String = ""
    var password: String = ""
    var inner: Inner? = null
    @Ignore
    var ignore: String = ""
}