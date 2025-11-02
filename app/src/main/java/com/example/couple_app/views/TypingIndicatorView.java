package com.example.couple_app.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.couple_app.R;

/**
 * Custom view for typing indicator with animated dots
 * Shows "..." animation when bot is typing
 */
public class TypingIndicatorView extends LinearLayout {

    private View dot1;
    private View dot2;
    private View dot3;
    private AnimatorSet animatorSet;

    public TypingIndicatorView(Context context) {
        super(context);
        init(context);
    }

    public TypingIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TypingIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_typing_indicator, this, true);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    /**
     * Start the typing animation
     */
    public void startAnimation() {
        if (animatorSet != null && animatorSet.isRunning()) {
            return;
        }

        // Create bounce animation for each dot with delay
        ObjectAnimator anim1 = createDotAnimation(dot1, 0);
        ObjectAnimator anim2 = createDotAnimation(dot2, 200);
        ObjectAnimator anim3 = createDotAnimation(dot3, 400);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(anim1, anim2, anim3);
        animatorSet.start();
    }

    /**
     * Stop the typing animation
     */
    public void stopAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        // Reset dots to original position
        if (dot1 != null) dot1.setTranslationY(0);
        if (dot2 != null) dot2.setTranslationY(0);
        if (dot3 != null) dot3.setTranslationY(0);
    }

    /**
     * Create bounce animation for a single dot
     */
    private ObjectAnimator createDotAnimation(View dot, long startDelay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -15f, 0f);
        animator.setDuration(600);
        animator.setStartDelay(startDelay);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        return animator;
    }
}

