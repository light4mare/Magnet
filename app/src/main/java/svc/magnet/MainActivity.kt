package svc.magnet

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.svc.magnet.NodeMagnet
import kotlinx.android.synthetic.main.activity_main.*
import svc.magnet.model.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val nodeMagnet = NodeMagnet<OuterSource>()
    private val random by lazy { Random() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bind()
    }

    private fun bind() {
        nodeMagnet.observe<OUTERSOURCE_INNER>(OuterSource.INNER) {
            textObject.text = "Inner: $it"
        }
        nodeMagnet.observe<OUTERSOURCE_ACCOUNT>(OuterSource.ACCOUNT) {
            textAccount.text = "account: $it"
        }
        nodeMagnet.observe<INNERSOURCE_NAME>(OuterSource.INNER, InnerSource.NAME) {
            textName.text = "name: $it"
        }.map {
            it.account?.apply { } ?: ""
        }.observe {
            Log.e("map", "map: $it")
        }
    }

    public fun onClick(view: View) {
        val outer = OuterSource()
        outer.inner = InnerSource()
        outer.account = random.nextInt(100).toString()
        outer.inner?.name = random.nextInt(100).toString()
        nodeMagnet.value(outer)
    }
}
