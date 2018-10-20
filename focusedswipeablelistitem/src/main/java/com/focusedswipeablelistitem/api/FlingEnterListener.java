package com.focusedswipeablelistitem.api;

import android.view.View;

public interface FlingEnterListener {
    void onCardEntered(View view, float objectX, float objectY, int position);

}
