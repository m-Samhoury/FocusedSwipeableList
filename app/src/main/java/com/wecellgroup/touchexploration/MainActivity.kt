package com.wecellgroup.touchexploration

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusedswipeablelistitem.api.ClickListener
import com.focusedswipeablelistitem.api.FlingExitListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_view.view.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val swipeableAdapterImplementation = SwipeableAdapterImplementation(context = this,
                frameLayout = frameLayoutOrderRoot,
                dataList = ArrayList(listOf("1", "2", "3", "4", "5", "6",
                        "7", "8", "9", "10", "11", "12", "13", "14", "15")))

        swipeableAdapterImplementation.clickListener = ClickListener { view, dataObject, direction ->
            Toast.makeText(this@MainActivity, "Clicked $direction", Toast.LENGTH_SHORT).show()
        }

        swipeableAdapterImplementation.flingExitListener = object : FlingExitListener {
            override fun onCardExited(view: View?, objectX: Float, objectY: Float, position: Int) {
                Log.d("listener", "onCardExited X:$objectX  Y:$objectY")
            }

            override fun leftExit(view: View?, dataObject: Any?, position: Int) {
                Log.d("listener", "leftExit")
                Toast.makeText(this@MainActivity, "Left", Toast.LENGTH_SHORT).show()
            }

            override fun rightExit(view: View?, dataObject: Any?, position: Int) {
                Log.d("listener", "rightExit")
                Toast.makeText(this@MainActivity, "Right", Toast.LENGTH_SHORT).show()
            }

            override fun topExit(view: View?, dataObject: Any?, position: Int) {
                Log.d("listener", "topExit")
                Toast.makeText(this@MainActivity,
                        "Top",
                        Toast.LENGTH_SHORT).show()
            }

            override fun bottomExit(view: View?, dataObject: Any?, position: Int) {
                Log.d("listener", "bottomExit")
                Toast.makeText(this@MainActivity,
                        "Bottom",
                        Toast.LENGTH_SHORT).show()
            }

            override fun onScroll(view: View?, scrollProgressPercentX: Float, scrollProgressPercentY: Float, position: Int) {
                Log.d("listener", "onScroll X: $scrollProgressPercentX, Y:$scrollProgressPercentY")
                (view as ViewGroup).textViewNo.alpha = 1 - normalize(-scrollProgressPercentX, 0f, 1f)
                (view as ViewGroup).textViewYes.alpha = 1 - normalize(scrollProgressPercentX, 0f, 1f)
            }

        }
    }

    /**
     * Calculates a value between 0 and 1, given the precondition that value
     * is between min and max. 0 means value = max, and 1 means value = min.
     */
    fun normalize(value: Float, min: Float, max: Float): Float {
        return 1 - (value - min) / (max - min)
    }


}
