package com.focusedswipeablelistitem.api;

import android.view.View;

public interface FlingListener {
    void onCardExited(View view, float objectX, float objectY);

    void leftExit(View view, Object dataObject);

    void rightExit(View view, Object dataObject);

    void topExit(View view, Object dataObject);

    void bottomExit(View view, Object dataObject);

    void onScroll(View view, float scrollProgressPercentX, float scrollProgressPercentY);


}


