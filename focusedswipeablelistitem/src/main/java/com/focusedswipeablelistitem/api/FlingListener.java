package com.focusedswipeablelistitem.api;

import android.view.View;

public interface FlingListener {
    void onCardExited(View view, float objectX, float objectY);

    void leftExit(Object dataObject);

    void rightExit(Object dataObject);

    void topExit(Object dataObject);

    void bottomExit(Object dataObject);

    void onScroll(float scrollProgressPercentX,float scrollProgressPercentY);


}


