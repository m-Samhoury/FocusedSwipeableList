package com.focusedswipeablelistitem.core;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.focusedswipeablelistitem.api.ClickListener;
import com.focusedswipeablelistitem.api.FlingListener;

import androidx.annotation.Nullable;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinausaurs might appear!
 * Utility class for implementing
 * <p>
 * <p>
 * Utility class for implementing descent swipable touch listener (In all directions)
 * as well as providing clicks in different parts of the view
 * Kudos to dionysis_lorentzos for allowing me to get a descent head start on this
 *
 * @link { https://github.com/Diolor/Swipecards/blob/master/library/src/main/java/com/lorentzos/flingswipe/BaseFlingAdapterView.java}
 */
class FlingCardListener implements View.OnTouchListener {
    private static final String TAG = FlingCardListener.class.getSimpleName();


    /**
     * Max allowed duration for a "click", in milliseconds.
     */
    private static final int MAX_CLICK_DURATION = 500;


    private static final int INVALID_POINTER_ID = -1;

    private final float objectX;
    private final float objectY;
    private final int objectH;
    private final int objectW;
    private final int parentWidth;
    private final int parentHeight;

    private final FlingListener mFlingListener;
    private ClickListener clickListener;
    private final Object dataObject;
    private final float halfWidth;
    private final float halfHeight;
    private final VelocityTracker mVelocityTracker;

    private final int mTouchSlop;
    private final int mMaximumVelocity;
    private final int mMinimumVelocity;

    private float BASE_ROTATION_DEGREES;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;

    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private View frame = null;


    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private int touchPosition;
    private final Object obj = new Object();
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));
    private boolean mDragging = false;

    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;

    private ViewPropertyAnimator animator;
    private int X_DIRECTION_THRESHOLD;
    private int Y_DIRECTION_THRESHOLD;
    private long pressStartTime;


    private RectF bottomRect;
    private RectF topRect;
    private RectF leftRect;
    private RectF rightRect;


    public FlingCardListener(View frame, Object itemAtPosition, FlingListener flingListener, ClickListener clickListener) {
        this(frame, itemAtPosition, 15f, flingListener, clickListener);
    }

    public FlingCardListener(View frame,
                             Object itemAtPosition,
                             float rotation_degrees,
                             @Nullable FlingListener flingListener,
                             @Nullable ClickListener clickListener) {
        super();
        this.frame = frame;
        this.objectX = frame.getX();
        this.objectY = frame.getY();
        this.objectH = frame.getHeight();
        this.objectW = frame.getWidth();
        this.halfWidth = objectW / 2f;
        this.halfHeight = objectH / 2f;
        this.dataObject = itemAtPosition;
        this.parentWidth = ((ViewGroup) frame.getParent()).getWidth();
        this.parentHeight = ((ViewGroup) frame.getParent()).getHeight();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;
        this.clickListener = clickListener;

        mVelocityTracker = VelocityTracker.obtain();

        Context context = frame.getContext();

        //Get system constants for touch thresholds
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

        X_DIRECTION_THRESHOLD = mTouchSlop * 2;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pressStartTime = System.currentTimeMillis();

                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer

                mActivePointerId = event.getPointerId(0);
                float x = 0;
                float y = 0;
                boolean success = false;
                x = event.getX(mActivePointerId);
                y = event.getY(mActivePointerId);
                success = true;


                if (success) {
                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;

                    int viewX = (int) frame.getX();
                    int viewY = (int) frame.getY();

                    int[] locationOnScreen = new int[2];
                    frame.getLocationOnScreen(locationOnScreen);

                    int onScreenLocationX = locationOnScreen[0];
                    int onScreenLocationY = locationOnScreen[1];

                    topRect = new RectF(onScreenLocationX,
                            onScreenLocationY,
                            onScreenLocationX + objectW,
                            (int) (onScreenLocationY + objectH * 0.25));

                    bottomRect = new RectF(onScreenLocationX,
                            (int) (onScreenLocationY + objectH * 0.75),
                            onScreenLocationX + objectW,
                            (int) (onScreenLocationY + objectH));

                    leftRect = new RectF(onScreenLocationX,
                            onScreenLocationY,
                            (int) (onScreenLocationX + objectW * 0.25),
                            (onScreenLocationY + objectH));

                    rightRect = new RectF((int) (onScreenLocationX + objectW * 0.75),
                            onScreenLocationY,
                            onScreenLocationX + objectW, onScreenLocationY + objectH);

                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier frame
                    if (aPosX == 0) {
                        aPosX = frame.getX();
                    }
                    if (aPosY == 0) {
                        aPosY = frame.getY();
                    }

                    if (y < objectH / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }

                    //Reset the velocity tracker
                    mVelocityTracker.clear();
                    mVelocityTracker.addMovement(event);

                }

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:

                long pressDuration = System.currentTimeMillis() - pressStartTime;


                mDragging = false;

                // Compute the current velocity and start a fling if it is above
                // the minimum threshold.
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                int velocityX = (int) mVelocityTracker.getXVelocity();
                int velocityY = (int) mVelocityTracker.getYVelocity();

                // Find the index of the active pointer and fetch its position
                int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                if (isValidIndex(event.getPointerCount(), pointerIndexMove))
                    return false;

                float xMove = event.getX(pointerIndexMove);
                float yMove = event.getY(pointerIndexMove);

                //Determine the direction of the fling, LEFT = 1, RIGHT = 0
                float traveledDistanceOnX = xMove - aDownTouchX;
                float traveledDistanceOnY = yMove - aDownTouchY;
                if (pressDuration < MAX_CLICK_DURATION && ((Math.abs(traveledDistanceOnX) < mTouchSlop)
                        || (Math.abs(traveledDistanceOnY) < mTouchSlop))) {

                    //Get the raw coordinates of the pointer on the screen
                    float rawPointerX = event.getRawX();
                    float rawPointerY = event.getRawY();

                    if (bottomRect.contains((int) rawPointerX, (int) rawPointerY)) {
                        if (clickListener != null) {
                            clickListener.onClick(dataObject, BOTTOM);
                        }
                    } else if (topRect.contains((int) rawPointerX, (int) rawPointerY)) {
                        if (clickListener != null) {
                            clickListener.onClick(dataObject, TOP);
                        }
                    } else {
                        if (leftRect.contains((int) rawPointerX, (int) rawPointerY)) {
                            if (clickListener != null) {
                                clickListener.onClick(dataObject, LEFT);
                            }
                        } else if (rightRect.contains((int) rawPointerX, (int) rawPointerY)) {
                            if (clickListener != null) {
                                clickListener.onClick(dataObject, RIGHT);
                            }
                        }
                    }
                    resetCardViewOnStackOrRemoveIt();
                } else {
                    if (Math.abs(velocityX) > mMinimumVelocity || Math.abs(velocityY) > mMinimumVelocity) {
                        switch (onFlingDirection(aDownTouchX, xMove, aDownTouchY, yMove)) {
                            case up:
                                if (Math.abs(traveledDistanceOnY) >= Y_DIRECTION_THRESHOLD) {
                                    selectTop();
                                } else {
                                    resetCardViewOnStackOrRemoveIt();
                                }
                                break;
                            case down:
                                if (Math.abs(traveledDistanceOnY) >= Y_DIRECTION_THRESHOLD) {
                                    selectBottom();
                                } else {
                                    resetCardViewOnStackOrRemoveIt();
                                }
                                break;
                            case left:
                                if (Math.abs(traveledDistanceOnX) >= X_DIRECTION_THRESHOLD) {
                                    selectLeftToDirection();
                                } else {
                                    resetCardViewOnStackOrRemoveIt();
                                }
                                break;
                            case right:
                                if (Math.abs(traveledDistanceOnX) >= X_DIRECTION_THRESHOLD) {
                                    selectRightToDirection();
                                } else {
                                    resetCardViewOnStackOrRemoveIt();
                                }
                            default: {
                                resetCardViewOnStackOrRemoveIt();
                            }
                        }
                    } else {
                        resetCardViewOnStackOrRemoveIt();
                    }
                }


                view.getParent().requestDisallowInterceptTouchEvent(false);
                mActivePointerId = INVALID_POINTER_ID;

                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_MOVE:


                mVelocityTracker.addMovement(event);

                // Find the index of the active pointer and fetch its position
                pointerIndexMove = event.findPointerIndex(mActivePointerId);
                if (isValidIndex(event.getPointerCount(), pointerIndexMove))
                    return false;

                xMove = event.getX(pointerIndexMove);
                yMove = event.getY(pointerIndexMove);
                if (Float.isNaN(xMove)
                        || Float.isNaN(yMove)) break;

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;

                //Check for slop on direct events
                if (!mDragging && (Math.abs(dy) > mTouchSlop || Math.abs(dx) > mTouchSlop)) {
                    mDragging = true;
                }


                // Move the frame
                aPosX += dx;
                aPosY += dy;

                // calculate the rotation degrees
                float distobjectX = aPosX - objectX;
                float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                //in this area would be code for doing something with the view as the frame moves.
                frame.setX(aPosX);
                frame.setY(aPosY);

                frame.setRotation(rotation);
                if (mFlingListener != null) {
                    mFlingListener.onScroll(getScrollProgressPercentX(), getScrollProgressPercentY());
                }
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                view.getParent().requestDisallowInterceptTouchEvent(false);

                //Stop any flinging in progress
                if (mDragging) {
                    mDragging = false;
                    abortFlingAnimation();
                }
                resetCardViewOnStackOrRemoveIt();
                break;
            }
        }

        return true;
    }


    /**
     * https://stackoverflow.com/a/26387629/3991044
     *
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @return
     */
    private Direction onFlingDirection(float x1, float x2, float y1, float y2) {
        // Grab two events located on the plane at e1=(x1, y1) and e2=(x2, y2)
        // Let e1 be the initial event
        // e2 can be located at 4 different positions, consider the following diagram
        // (Assume that lines are separated by 90 degrees.)
        //
        //
        //         \ A  /
        //          \  /
        //       D   e1   B
        //          /  \
        //         / C  \
        //
        // So if (x2,y2) falls in region:
        //  A => it's an UP swipe
        //  B => it's a RIGHT swipe
        //  C => it's a DOWN swipe
        //  D => it's a LEFT swipe
        //
        return getDirection(x1, y1, x2, y2);
    }


    /**
     * Given two points in the plane p1=(x1, x2) and p2=(y1, y1), this method
     * returns the direction that an arrow pointing from p1 to p2 would have.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the direction
     */
    public Direction getDirection(float x1, float y1, float x2, float y2) {
        double angle = getAngle(x1, y1, x2, y2);
        return Direction.fromAngle(angle);
    }

    /**
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the right, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    public double getAngle(float x1, float y1, float x2, float y2) {
//        double rad = Math.atan2(y2 - y1, x2 - x1) + Math.PI;
//        return (rad * 180 / Math.PI + 180) % 360;
        float angle = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }


    public enum Direction {
        up,
        down,
        left,
        right;

        /**
         * Returns a direction given an angle.
         * Directions are defined as follows:
         * <p>
         * Up: [45, 135]
         * Right: [0,45] and [315, 360]
         * Down: [225, 315]
         * Left: [135, 225]
         *
         * @param angle an angle from 0 to 360 - e
         * @return the direction of an angle
         */
        public static Direction fromAngle(double angle) {
            if (inRange(angle, 45, 135)) {
                return Direction.up;
            } else if (inRange(angle, 0, 45) || inRange(angle, 315, 360)) {
                return Direction.right;
            } else if (inRange(angle, 225, 315)) {
                return Direction.down;
            } else {
                return Direction.left;
            }

        }

        /**
         * @param angle an angle
         * @param init  the initial bound
         * @param end   the final bound
         * @return returns true if the given angle is in the interval [init, end).
         */
        private static boolean inRange(double angle, float init, float end) {
            return (angle >= init) && (angle < end);
        }
    }

    private boolean isValidIndex(int pointerCount, int pointerIndexMove) {
        return pointerIndexMove < 0 || pointerIndexMove >= pointerCount;
    }

    private void abortFlingAnimation() {
        if (isAnimationRunning && animator != null) {
            animator.cancel();
        }
    }

    private float getScrollProgressPercentX() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }


    private float getScrollProgressPercentY() {
        if (movedBeyondTopBorder()) {
            return -1f;
        } else if (movedBeyondBottomBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosY + halfHeight - topBorder()) / (bottomBorder() - topBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private boolean resetCardViewOnStackOrRemoveIt() {
        if (movedBeyondLeftBorder()) {
            // Left Swipe
            onSelected(true, getExitPoint(-objectW), 100);
            if (mFlingListener != null) {
                mFlingListener.onScroll(getScrollProgressPercentX(), getScrollProgressPercentY());
            }
        } else if (movedBeyondRightBorder()) {
            // Right Swipe
            onSelected(false, getExitPoint(parentWidth), 100);
            if (mFlingListener != null) {
                mFlingListener.onScroll(getScrollProgressPercentX(), getScrollProgressPercentY());
            }
        } else if (movedBeyondTopBorder()) {
            //top swipe
            selectTop();
            if (mFlingListener != null) {
                mFlingListener.onScroll(getScrollProgressPercentX(), getScrollProgressPercentY());
            }
        } else if (movedBeyondBottomBorder()) {
            //bottom swipe
            selectBottom();
            if (mFlingListener != null) {
                mFlingListener.onScroll(getScrollProgressPercentX(), getScrollProgressPercentY());
            }
        } else {
            float abslMoveDistance = Math.abs(aPosX - objectX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            frame.animate()
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(objectX)
                    .y(objectY)
                    .scaleXBy(0)
                    .scaleYBy(0)
                    .rotation(0)
                    .start();
            if (mFlingListener != null) {
                mFlingListener.onScroll(0.0f, 0.0f);
            }

        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }

    private boolean movedBeyondTopBorder() {
        return aPosY + halfHeight < topBorder();
    }

    private boolean movedBeyondBottomBorder() {
        return aPosY + halfHeight > bottomBorder();
    }


    public float leftBorder() {
        return parentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * parentWidth / 4.f;
    }

    public float topBorder() {
        return parentHeight / 4.f;
    }

    public float bottomBorder() {
        return 3 * parentHeight / 4.f;
    }


    private void onSelected(final boolean isLeft,
                            float exitY, long duration) {

        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -objectW - getRotationWidthOffset();
        } else {
            exitX = parentWidth + getRotationWidthOffset();
        }

        animator = this.frame.animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .rotation(getExitRotation(isLeft))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mFlingListener != null) {
                            mFlingListener.onCardExited(frame, objectX, objectY);
                        }
                        if (isLeft) {
                            if (mFlingListener != null) {
                                mFlingListener.leftExit(dataObject);
                            }
                        } else {
                            if (mFlingListener != null) {
                                mFlingListener.rightExit(dataObject);
                            }
                        }
                        isAnimationRunning = false;
                        animator = null;
                    }
                });
    }

    private void onSelectedY(final boolean isTop,
                             float exitX, long duration) {

        isAnimationRunning = true;
        float exitY;
        if (isTop) {
            exitY = -objectH - getRotationWidthOffset();
        } else {
            exitY = parentHeight + getRotationWidthOffset();
        }

        animator = this.frame.animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .rotation(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mFlingListener != null) {
                            mFlingListener.onCardExited(frame, objectX, objectY);
                        }
                        if (isTop) {
                            if (mFlingListener != null) {
                                mFlingListener.topExit(dataObject);
                            }
                        } else {
                            if (mFlingListener != null) {
                                mFlingListener.bottomExit(dataObject);
                            }
                        }
                        isAnimationRunning = false;
                        animator = null;
                    }
                });
    }


    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning)
            onSelected(true, (float) (objectY - frame.getHeight() * 1.5), 250);
    }


    /**
     * Starts a default right exit animation.
     */
    public void selectRightTop() {
        if (!isAnimationRunning)
            onSelected(false, (float) (objectY - frame.getHeight() * 1.5), 250);
    }

    /**
     * Starts a default top exit animation.
     */
    public void selectTop() {
        if (!isAnimationRunning)
            onSelectedY(true, (float) objectX, 100);
    }

    /**
     * Starts a default bottom exit animation.
     */
    public void selectBottom() {
        if (!isAnimationRunning)
            onSelectedY(false, (float) objectX, 100);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRightMiddle() {
        if (!isAnimationRunning)
            onSelected(false, objectY, 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRightBottom() {
        if (!isAnimationRunning)
            onSelected(false, (float) (objectY + frame.getHeight() * 1.5), 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRightToDirection() {
        if (!isAnimationRunning)
            onSelected(false, objectY, 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void dismissInDirection() {
        if (!isAnimationRunning)
            onSelected(false, objectY, 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectLeftTop() {
        if (!isAnimationRunning)
            onSelected(true, (float) (objectY - frame.getHeight() * 1.5), 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectLeftBottom() {
        if (!isAnimationRunning)
            onSelected(true, (float) (objectY + frame.getHeight() * 1.5), 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectLeftMiddle() {
        if (!isAnimationRunning)
            onSelected(true, objectY, 250);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectLeftToDirection() {
        if (!isAnimationRunning)
            onSelected(true, (float) (objectY - frame.getHeight() * 1.5), 250);
    }


    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical y = ax+b linear regression
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }

    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p/>
     * The below method calculates the width offset of the rotation.
     */
    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }


    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.aPosX, this.aPosY);
    }


}
