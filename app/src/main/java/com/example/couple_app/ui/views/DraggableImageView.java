package com.example.couple_app.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Simple ImageView subclass that overrides performClick to satisfy accessibility lint when
 * setOnTouchListener is used on the view.
 */
public class DraggableImageView extends AppCompatImageView {
    public DraggableImageView(Context context) {
        super(context);
    }

    public DraggableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DraggableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Call super to handle the accessibility event and click listeners.
        return super.performClick();
    }
}
