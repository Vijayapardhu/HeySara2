package com.mvp.sara;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class VoiceBarsView extends View {
    private static final int BAR_COUNT = 5;
    private static final int ANIMATION_DURATION = 600;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] barHeights = new float[BAR_COUNT];
    private final ValueAnimator[] animators = new ValueAnimator[BAR_COUNT];
    private final float[] minHeights = {0.3f, 0.5f, 0.7f, 0.5f, 0.3f};
    private final float[] maxHeights = {1f, 0.8f, 1f, 0.8f, 1f};

    public VoiceBarsView(Context context) { super(context); init(); }
    public VoiceBarsView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public VoiceBarsView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        barPaint.setColor(0xFFFFFFFF);
        barPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < BAR_COUNT; i++) {
            final int idx = i;
            barHeights[i] = minHeights[i];
            animators[i] = ValueAnimator.ofFloat(minHeights[i], maxHeights[i]);
            animators[i].setDuration(ANIMATION_DURATION);
            animators[i].setRepeatCount(ValueAnimator.INFINITE);
            animators[i].setRepeatMode(ValueAnimator.REVERSE);
            animators[i].setInterpolator(new LinearInterpolator());
            animators[i].setStartDelay(i * 100);
            animators[i].addUpdateListener(animation -> {
                barHeights[idx] = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
    }

    public void startBarsAnimation() {
        for (ValueAnimator animator : animators) {
            if (!animator.isStarted()) animator.start();
        }
    }

    public void stopBarsAnimation() {
        for (ValueAnimator animator : animators) {
            animator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float barWidth = width / (BAR_COUNT * 2f);
        float gap = barWidth;
        float maxBarHeight = height * 0.8f;
        float baseY = (height - maxBarHeight) / 2f;
        for (int i = 0; i < BAR_COUNT; i++) {
            float x = (i * 2 + 1) * barWidth;
            float barH = barHeights[i] * maxBarHeight;
            canvas.drawRoundRect(x, baseY + (maxBarHeight - barH), x + barWidth, baseY + maxBarHeight, barWidth/2, barWidth/2, barPaint);
        }
    }
} 