package com.wecellgroup.touchexploration

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusedswipeablelistitem.api.ClickListener
import com.focusedswipeablelistitem.api.FlingListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val swipeableAdapterImplementation = SwipeableAdapterImplementation(context = this,
                frameLayout = frameLayoutOrderRoot,
                dataList = ArrayList(listOf("1", "2", "3", "4", "5", "6", "7")))

        swipeableAdapterImplementation.clickListener = ClickListener { dataObject, direction ->
            Toast.makeText(this@MainActivity, "Clicked $direction", Toast.LENGTH_SHORT).show()
        }

        swipeableAdapterImplementation.flingListener = object : FlingListener {
            override fun topExit(dataObject: Any?) {
                Log.d("listener", "topExit")
            }

            override fun bottomExit(dataObject: Any?) {
                Log.d("listener", "bottomExit")
            }

            override fun leftExit(dataObject: Any?) {
                Log.d("listener", "leftExit")
            }

            override fun rightExit(dataObject: Any?) {
                Log.d("listener", "rightExit")
            }

            override fun onScroll(scrollProgressPercentX: Float, scrollProgressPercentY: Float) {
                Log.d("listener", "onScroll X: $scrollProgressPercentX, Y:$scrollProgressPercentY")
            }

            override fun onCardExited(view: View?, objectX: Float, objectY: Float) {
                Log.d("listener", "onCardExited X:$objectX  Y:$objectY")
            }

        }
    }


}
