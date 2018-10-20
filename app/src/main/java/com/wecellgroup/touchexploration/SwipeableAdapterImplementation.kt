package com.wecellgroup.touchexploration


import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.focusedswipeablelistitem.core.SwipeableAdapter
import kotlinx.android.synthetic.main.card_view.view.*

class SwipeableAdapterImplementation(val context: Context, val frameLayout: FrameLayout,
                                     private val dataList: ArrayList<String>)
    : SwipeableAdapter<SwipeableAdapterImplementation.MyViewHolder>(context, frameLayout) {


    override fun createViewHolder(parentView: ViewGroup, position: Int): MyViewHolder =
            MyViewHolder(createView(R.layout.card_view, parent = parentView))


    override fun getItemCount(): Int = dataList.size


    init {
        setupAdapter()

    }

    override fun onBindView(viewHolder: MyViewHolder, position: Int) {
        viewHolder.itemView.textViewDescription.text = dataList[position]
    }

    override fun getViewPoolSize(): Int = if (3 > getItemCount()) getItemCount() else 3


    public class MyViewHolder(itemView: View) : SwipeableAdapter.ViewHolder(itemView) {

    }
}