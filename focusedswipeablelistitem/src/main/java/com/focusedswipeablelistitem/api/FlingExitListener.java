package com.focusedswipeablelistitem.api;

import android.view.View;

public interface FlingExitListener {
    void onCardExited(View view, float objectX, float objectY, int position);

    void leftExit(View view, Object dataObject, int position);

    void rightExit(View view, Object dataObject, int position);

    void topExit(View view, Object dataObject, int position);

    void bottomExit(View view, Object dataObject, int position);

    void onScroll(View view, float scrollProgressPercentX, float scrollProgressPercentY, int position);


}


