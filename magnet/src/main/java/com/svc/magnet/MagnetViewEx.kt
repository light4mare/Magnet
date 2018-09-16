package com.svc.magnet

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import svc.magnet.annotation.Block
import svc.magnet.annotation.MagnetNode
import java.lang.reflect.Field

fun View.bindClick(magnet: Magnet<Int>): View {
    this.setOnClickListener {
        magnet.value(MagnetKey.NORMAL_CLICK, false)
    }

    this.setOnLongClickListener {
        magnet.value(MagnetKey.LONG_CLICK, false)
        true
    }

    return this
}

fun View.bindVisible(magnet: Magnet<Int>): View {
    magnet.observe {
        visibility = it
    }
    return this
}

fun View.bindSelect(magnet: Magnet<Boolean>): View {
    magnet.value(false)
    magnet.observe {
        this.isSelected = it
    }

    return this
}

fun EditText.bind(magnet: Magnet<String>): EditText {
    val editTextProxy = object : ProxySource<String>() {
        override fun bindMagnet(magnet: Magnet<String>) {
            this@bind.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    isSourceChange = true
                    magnet.value(s.toString())
                }
            })
        }

        override fun onObserve(t: String) {
            this@bind.setText(t)
        }
    }

    editTextProxy.bindMagnet(magnet)

    return this
}

/**
 * 这种非代码生成的方式无法将反射相关对象缓存
 * 但是反射的调用量在百万级以下时不用考虑性能问题
 */
fun <T : Any> TextView.bind(magnet: Magnet<T>, key: String): TextView {
    magnet.observe {
        var field: Field
        var clazz: Class<*> = it.javaClass
        // 无法做缓存
        val propertyChain = key.split(".")
        val fieldArray = arrayOfNulls<Field>(propertyChain.size)

        for (index in propertyChain.indices) {
            val property = propertyChain[index]
            field = clazz.getDeclaredField(property)
            field.let { propertyField ->
                propertyField.isAccessible = true
                fieldArray[index] = propertyField
                if (index < propertyChain.size) {
                    clazz = propertyField.genericType as Class<*>
                }
            }
        }

        var any: Any? = it
        for (propertyField in fieldArray) {
            any = propertyField?.get(any)
        }
        text = any as String
    }

    return this
}

fun <T : MagnetNode<T>> TextView.bind(magnet: NodeMagnet<T>, vararg key: String) {
    magnet.observe<String>(*key) {
        text = it
    }
}

fun <T : MagnetNode<T>, V: Any> TextView.bind(magnet: NodeMagnet<T>, vararg key: String, block: (t: V) -> String) {
    magnet.observe<V>(*key) {
        text = block(it)
    }
}

fun ProgressBar.bind(magnet: Magnet<Int>): ProgressBar {
    magnet.observe {
        this.progress = it
    }

    return this
}