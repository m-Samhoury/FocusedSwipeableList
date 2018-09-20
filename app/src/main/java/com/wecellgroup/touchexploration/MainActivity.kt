package com.wecellgroup.touchexploration

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val swipeableAdapterImplementation = SwipeableAdapterImplementation(context = this,
                frameLayout = frameLayoutOrderRoot,
                dataList = ArrayList(listOf("1", "2", "3", "4", "5", "6", "7")))


    }


}
