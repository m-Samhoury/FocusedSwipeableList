package com.focusedswipeablelistitem.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.collection.SparseArrayCompat
import androidx.core.util.Pools
import androidx.core.view.ViewCompat
import com.focusedswipeablelistitem.api.ClickListener
import com.focusedswipeablelistitem.api.FlingEnterListener
import com.focusedswipeablelistitem.api.FlingExitListener
import com.focusedswipeablelistitem.dip

/**
 *Base adapter that manages the creation and recycling of swiped away views
 */
abstract class SwipeableAdapter<T : SwipeableAdapter.ViewHolder>(
        private val context: Context,
        private val rootFrameLayout: FrameLayout,
        private val initialRotation: Float = 15f,
        private val enableLeftSwipe: Boolean = true,
        private val enableRightSwipe: Boolean = true,
        private val enableTopSwipe: Boolean = true,
        private val enableBottomSwipe: Boolean = true) {

    private lateinit var attachedViewsByPosition: SparseArrayCompat<T>

    private var currentViewsPosition = 0
    private var currentVisiblePosition = 0
    private var isSetup: Boolean = false

    private lateinit var recycledViewsPool: RecycledViewsPool<T>
    private lateinit var recyclerViewPool: Pools.SimplePool<T>

    var flingEnterListener: FlingEnterListener? = null
    var flingExitListener: FlingExitListener? = null
    var clickListener: ClickListener? = null


    private var mainFlingListener = object : FlingExitListener {
        override fun bottomExit(view: View, dataObject: Any?, position: Int) {
            flingExitListener?.bottomExit(view, dataObject, currentVisiblePosition)
        }

        override fun topExit(view: View, dataObject: Any?, position: Int) {
            flingExitListener?.topExit(view, dataObject, currentVisiblePosition)
        }


        override fun onCardExited(view: View, objectX: Float, objectY: Float, position: Int) {
            currentVisiblePosition--

            for (i in 0 until attachedViewsByPosition.size()) {
                val key = attachedViewsByPosition.keyAt(i)
                // get the object by the key.

                attachedViewsByPosition.get(key)?.let {
                    if (it.itemId == view.id) {
                        removeAndRecycleView(it)
                    }
                }
            }

            if (rootFrameLayout.childCount == 0) {
                flingExitListener?.onCardExited(view, objectX, objectY, getExitingPosition())
                onAllItemsSwiped()
                return
            }

            if (currentViewsPosition <= 0) {
                attachTouchListenerToLastView()
                flingExitListener?.onCardExited(view, objectX, objectY, getExitingPosition())
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
            Log.d("swipe", "currentViewsPosition: $currentViewsPosition")

            flingExitListener?.onCardExited(view, objectX, objectY, getExitingPosition())

        }

        override fun leftExit(view: View, dataObject: Any?, position: Int) {
            flingExitListener?.leftExit(view, dataObject, currentVisiblePosition)
        }

        override fun rightExit(view: View, dataObject: Any?, position: Int) {
            flingExitListener?.rightExit(view, dataObject, currentVisiblePosition)
        }


        override fun onScroll(view: View, scrollProgressPercentX: Float, scrollProgressPercentY: Float, position: Int) {
            flingExitListener?.onScroll(view, scrollProgressPercentX, scrollProgressPercentY, getRealCurrentPosition())
        }
    }
    private val mainClickListener =
            ClickListener { view, dataObject, direction ->
                clickListener?.onClick(view,
                        dataObject, direction)
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
    protected open fun onAllItemsSwiped() {}

    /**
     * Called when new view is needed to be created
     *
     * it is advised to call the helper method @link{createView()}
     */
    protected abstract fun createViewHolder(parentView: ViewGroup, position: Int = currentViewsPosition): T

    private fun adjustViewMargins(view: View, topMargin: Int = 0) {
        (view.layoutParams as FrameLayout.LayoutParams).topMargin = topMargin
    }

    private fun getLastView(): View = rootFrameLayout.getChildAt(rootFrameLayout.childCount - 1)


    private fun removeAndRecycleView(viewHolder: T) {
        checkSetup()
        viewHolder.itemView.visibility = View.GONE
        try {
            if (viewHolder.itemView.parent != null) {
                viewHolder.itemView.setOnTouchListener(null)
                rootFrameLayout.removeView(viewHolder.itemView)
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        attachedViewsByPosition.remove(viewHolder.itemView.id)
//        recycledViewsPool.recycleView(viewHolder)
        onViewRecycled(viewHolder)
    }


    /**
     * This should be called on the initialization of the child class
     */
    protected fun setupAdapter() {
        isSetup = true
        currentViewsPosition = getItemCount() - 1
        currentVisiblePosition = getItemCount() - 1
        recycledViewsPool = RecycledViewsPool(getViewPoolSize())
        recyclerViewPool = Pools.SimplePool(getViewPoolSize())

        attachedViewsByPosition = SparseArrayCompat(getViewPoolSize())

        for (i in 0 until getViewPoolSize()) {
            val createdViewHolder = createViewHolder(rootFrameLayout)
            createdViewHolder.itemId = createdViewHolder.itemView.id
            attachedViewsByPosition.append(createdViewHolder.itemId, createdViewHolder)
            onBindView(createdViewHolder)
            rootFrameLayout.addView(createdViewHolder.itemView, 0)
        }
        attachTouchListenerPost(rootFrameLayout.getChildAt(rootFrameLayout.childCount - 1))
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


//        attachTouchListenerPost(touchableView)
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
                    initialRotation, mainFlingListener, mainClickListener
            )

            flingCardListener.setAllowLeftSwipe(enableLeftSwipe)
            flingCardListener.setAllowRightSwipe(enableRightSwipe)
            flingCardListener.setAllowTopSwipe(enableTopSwipe)
            flingCardListener.setAllowBottomSwipe(enableBottomSwipe)

            touchableView.setOnTouchListener(flingCardListener)
            touchableView.tag = flingCardListener
        }
    }

    private fun attachTouchListener(touchableView: View) {

        val allViewsCount = rootFrameLayout.childCount
        for (i in 0 until allViewsCount) {
            rootFrameLayout.getChildAt(i)?.setOnTouchListener(null)
        }


        val flingCardListener = FlingCardListener(touchableView,
                touchableView,
                initialRotation, mainFlingListener, mainClickListener
        )
        flingCardListener.setAllowLeftSwipe(enableLeftSwipe)
        flingCardListener.setAllowRightSwipe(enableRightSwipe)
        flingCardListener.setAllowTopSwipe(enableTopSwipe)
        flingCardListener.setAllowBottomSwipe(enableBottomSwipe)

        touchableView.setOnTouchListener(flingCardListener)
        touchableView.tag = flingCardListener
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
    protected abstract fun onBindView(viewHolder: T, position: Int = currentViewsPosition--)

    companion object {

        private class RecycledViewsPool<T : ViewHolder>(val size: Int) {
            private val viewsPool = Pools.SynchronizedPool<T>(size)


            public fun recycleView(toBeRecycled: T) {
                viewsPool.release(toBeRecycled)
            }

            public fun obtainView() = viewsPool.acquire()

        }

        const val DIRECTION_TOP = 1
        const val DIRECTION_BOTTOM = 2
        const val DIRECTION_LEFT = 3
        const val DIRECTION_RIGHT = 4
        const val DIRECTION_NONE = 5


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


    public fun selectTop() {
        (getLastView().tag as? FlingCardListener)?.selectTop()
    }

    public fun selectBottom() {
        (getLastView().tag as? FlingCardListener)?.selectBottom()
    }

    public fun selectRight() {
        (getLastView().tag as? FlingCardListener)?.selectRightMiddle()
    }

    public fun selectLeft() {
        (getLastView().tag as? FlingCardListener)?.selectLeftMiddle()
    }


    private fun getRealCurrentPosition(): Int = currentVisiblePosition
    private fun getExitingPosition(): Int = currentVisiblePosition + 1

    public fun notifyItemInserted(insertedIndex: Int, direction: Int = DIRECTION_NONE) {

        if (rootFrameLayout.childCount >= getViewPoolSize()) {
            val viewToBeRemoved = rootFrameLayout.getChildAt(0)

            if (viewToBeRemoved != null) {
                adjustViewMargins(viewToBeRemoved)
                for (i in 0 until attachedViewsByPosition.size()) {
                    val key = attachedViewsByPosition.keyAt(i)
                    // get the object by the key.
                    attachedViewsByPosition.get(key)?.let {
                        if (it.itemId == viewToBeRemoved.id) {
                            removeAndRecycleView(it)
                        }
                    }
                }
            }
        }

        val createdViewHolder = createViewHolder(rootFrameLayout)
        createdViewHolder.itemView.layoutParams = createDefaultLayoutParams()

        createdViewHolder.itemView.post {
            createdViewHolder.itemId = createdViewHolder.itemView.id
            attachedViewsByPosition.append(createdViewHolder.itemId, createdViewHolder)
            currentViewsPosition = getItemCount() - rootFrameLayout.childCount
            currentVisiblePosition++
            onBindView(createdViewHolder, getRealCurrentPosition())
            currentViewsPosition--
            attachTouchListenerToLastView()


            when (direction) {
                DIRECTION_TOP -> {
                    animateBackFromTop(createdViewHolder)
                }
                DIRECTION_BOTTOM -> {
                    animateBackFromBottom(createdViewHolder)
                }
                DIRECTION_LEFT -> {
                    animateBackFromLeft(createdViewHolder)
                }
                DIRECTION_RIGHT -> {
                    animateBackFromRight(createdViewHolder)
                }
                DIRECTION_NONE -> {
                }
                else -> {
                }
            }

            flingEnterListener?.onCardEntered(createdViewHolder.itemView,
                    createdViewHolder.initialX,
                    createdViewHolder.initialY,
                    getExitingPosition())
        }
        createdViewHolder.itemView.visibility = View.INVISIBLE
        rootFrameLayout.addView(createdViewHolder.itemView)

    }


    private fun animateBackFromTop(createdViewHolder: T) {
        createdViewHolder.itemView.y =
                createdViewHolder.itemView.y - createdViewHolder.itemView.dip(60)
        createdViewHolder.itemView.animate()
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator(0.5f))
                .x(createdViewHolder.initialX)
                .y(createdViewHolder.initialY)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {

                    }
                });
    }


    private fun animateBackFromBottom(createdViewHolder: T) {

    }

    private fun animateBackFromLeft(createdViewHolder: T) {
        createdViewHolder.itemView.x =
                createdViewHolder.itemView.x - (createdViewHolder.itemView.width * 1.5).toInt()

        createdViewHolder.itemView.rotation = -45f

        createdViewHolder.itemView.animate()
                .setDuration(0)
                .xBy(-createdViewHolder.itemView.width * 1.5f)
                .rotation(-45f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        createdViewHolder.itemView.requestLayout()
                        createdViewHolder.itemView.visibility = View.VISIBLE
                    }
                })
                .start()

        createdViewHolder.itemView.animate()
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator(0.5f))
                .x(createdViewHolder.initialX)
                .y(createdViewHolder.initialY)
                .rotation(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        createdViewHolder.itemView.requestLayout()
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        createdViewHolder.itemView.requestLayout()
                    }
                })
                .start()

    }

    private fun animateBackFromRight(createdViewHolder: T) {

    }

}
