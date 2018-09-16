package svc.magnet.model

import svc.magnet.annotation.Source

@Source
class Inner{
    var name: String = ""
    var age: Int = 0
}

@Source
class Outer{
    var account: String = ""
    var password: String = ""
    var inner: Inner? = null
}