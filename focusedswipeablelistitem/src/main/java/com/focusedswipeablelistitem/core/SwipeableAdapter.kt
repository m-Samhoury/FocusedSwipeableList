package com.focusedswipeablelistitem.core

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.collection.SparseArrayCompat
import androidx.core.util.Pools
import androidx.core.view.ViewCompat
import com.focusedswipeablelistitem.dip

/**
 *Base adapter that manages the creation and recycling of swiped away views
 */
abstract class SwipeableAdapter<T : SwipeableAdapter.ViewHolder>(private val context: Context,
                                                                 private val rootFrameLayout: FrameLayout,
                                                                 private val initialRotation: Float = 15f) {

    private lateinit var attachedViewsByPosition: SparseArrayCompat<T>

    private var currentPosition = 0
    private var isSetup: Boolean = false

    private lateinit var recycledViewsPool: RecycledViewsPool<T>
    private lateinit var recyclerViewPool: Pools.SimplePool<T>

    private val flingListener: FlingCardListener.FlingListener? = null


    private var mainFlingListener = object : FlingCardListener.FlingListener {
        override fun onClick(dataObject: Any?, direction: Int) {
            flingListener?.onClick(dataObject, direction)
        }

        override fun onCardExited(view: View, objectX: Float, objectY: Float) {
            if (currentPosition < -1) {
                onAllItemsSwiped()
                return
            }
            for (i in 0 until attachedViewsByPosition.size()) {
                val key = attachedViewsByPosition.keyAt(i)
                // get the object by the key.

                attachedViewsByPosition.get(key)?.let {
                    if (it.itemId == view.id) {
                        removeAndRecycleView(it)
                    }
                }
            }

            if (currentPosition < 0) {
                attachTouchListenerToLastView()

                return
            }

            adjustViewMargins(view)

            val newView = createViewHolder(rootFrameLayout)
            adjustViewMargins(newView.itemView, view.dip(16))
            newView.itemId = newView.itemView.id
            newView.itemView.layoutParams = createDefaultLayoutParams()


            attachedViewsByPosition.append(newView.itemId, newView)


            newView.reAdjustView()

            rootFrameLayout.addView(newView.itemView, 0)




            attachTouchListenerToLastView()
            onBindView(newView)
            Log.d("swipe", "currentPosition: $currentPosition")

            flingListener?.onCardExited(view, objectX, objectY)

        }

        override fun leftExit(dataObject: Any?) {
            flingListener?.leftExit(dataObject)
        }

        override fun rightExit(dataObject: Any?) {
            flingListener?.rightExit(dataObject)
        }


        override fun onScroll(scrollProgressPercent: Float) {
            flingListener?.onScroll(scrollProgressPercent)
        }
    }

    /**
     * Make sure that the top most view only gets the touch listener
     */
    private fun attachTouchListenerToLastView() {
        val indexOfLastChild = rootFrameLayout.childCount - 1
        val childAt = rootFrameLayout.getChildAt(indexOfLastChild)
        if (childAt != null) {
            attachTouchListener(childAt)
        }
    }


    /**
     * Called when all items provided by the adapter has been swiped away
     */
    protected fun onAllItemsSwiped() {}

    /**
     * Called when new view is needed to be created
     *
     * it is advised to call the helper method @link{createView()}
     */
    protected abstract fun createViewHolder(parentView: ViewGroup, position: Int = currentPosition): T

    private fun adjustViewMargins(view: View, topMargin: Int = 0) {
        (view.layoutParams as FrameLayout.LayoutParams).topMargin = topMargin
    }

    private fun removeAndRecycleView(viewHolder: T) {
        checkSetup()
        viewHolder.itemView.visibility = View.GONE
        rootFrameLayout.removeView(viewHolder.itemView)
        attachedViewsByPosition.remove(viewHolder.itemView.id)
        recycledViewsPool.recycleView(viewHolder)
        onViewRecycled(viewHolder)
    }


    /**
     * This should be called on the initialization of the child class
     */
    protected fun setupAdapter() {
        isSetup = true
        currentPosition = getItemCount() - 1
        recycledViewsPool = RecycledViewsPool(getViewPoolSize())
        recyclerViewPool = Pools.SimplePool(getViewPoolSize())

        attachedViewsByPosition = SparseArrayCompat(getViewPoolSize())

        for (i in 0 until getViewPoolSize()) {
            val createdViewHolder = createViewHolder(rootFrameLayout)
            createdViewHolder.itemId = createdViewHolder.itemView.id
            attachedViewsByPosition.append(createdViewHolder.itemId, createdViewHolder)
            onBindView(createdViewHolder)
            rootFrameLayout.addView(createdViewHolder.itemView, 0)
            Log.d("swipe", "currentPosition: $currentPosition")
        }
    }

    /**
     * helper method to remove the hassle of creating the view yourself
     * @param layoutRes the layout to be inflated
     * @param parent the viewgroup that this view belongs to
     *
     * @return the created view
     */
    protected open fun createView(layoutRes: Int, parent: ViewGroup): View {
        checkSetup()

        val touchableView = LayoutInflater.from(context).inflate(layoutRes, parent, false)

        touchableView.id = ViewCompat.generateViewId()
        val layoutParams = createDefaultLayoutParams()
        touchableView.layoutParams = layoutParams


        attachTouchListenerPost(touchableView)
        return touchableView
    }

    /**
     * This touch listener needs to know the coordinates of its view, thus we need to wait until the view has
     * been drawn then assign it thus the suffix Post
     */
    private fun attachTouchListenerPost(touchableView: View) {
        val viewGroup = touchableView.parent as? ViewGroup

        viewGroup?.let {
            val allViewsCount = it.childCount
            for (i in 0 until allViewsCount) {
                it.getChildAt(i)?.setOnTouchListener(null)
            }
        }


        touchableView.post {
            val flingCardListener = FlingCardListener(touchableView,
                    touchableView,
                    initialRotation, mainFlingListener
            )
            touchableView.setOnTouchListener(flingCardListener)
        }
    }

    private fun attachTouchListener(touchableView: View) {

        val allViewsCount = rootFrameLayout.childCount
        for (i in 0 until allViewsCount) {
            rootFrameLayout.getChildAt(i)?.setOnTouchListener(null)
        }


        val flingCardListener = FlingCardListener(touchableView,
                touchableView,
                initialRotation, mainFlingListener
        )
        touchableView.setOnTouchListener(flingCardListener)
    }

    private fun createDefaultLayoutParams(): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    private fun checkSetup() =
            if (!isSetup)
                throw IllegalStateException("You need to call the setup method in your constructor")
            else Any()


    protected open fun onViewRecycled(viewHolder: T) {
        checkSetup()

    }

    /**
     * Determine the number of stacked views at the same time
     * Default is 2
     */
    protected abstract fun getViewPoolSize(): Int


    public abstract fun getItemCount(): Int

    /**
     * Called when the view is ready to get bound to new data
     */
    protected abstract fun onBindView(viewHolder: T, position: Int = currentPosition--)

    companion object {

        private class RecycledViewsPool<T : ViewHolder>(val size: Int) {
            private val viewsPool = Pools.SynchronizedPool<T>(size)


            public fun recycleView(toBeRecycled: T) {
                viewsPool.release(toBeRecycled)
            }

            public fun obtainView() = viewsPool.acquire()

        }


    }

    abstract class ViewHolder(val itemView: View) {
        companion object {
            val NO_POSITION = -1
            val NO_ID: Int = -1
            val INVALID_TYPE = -1
        }

        internal var initialX: Float = -1f
        internal var initialY: Float = -1f

        var position = NO_POSITION
            internal set
        var itemId = NO_ID
            internal set
        var itemViewType = INVALID_TYPE
            internal set
        internal var mIsDirty = true

        init {
            itemView.post {
                initialX = itemView.x
                initialY = itemView.y
            }

        }


        internal fun reAdjustView() {
            itemView.clearAnimation()

            itemView.x = initialX
            itemView.y = initialY
            itemView.rotation = 0f
            itemView.visibility = View.VISIBLE
            itemView.tag = 0
            itemView.requestLayout()


        }
    }

}
