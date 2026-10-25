package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Fork (Thomas): a small ring filled clockwise to a fraction, for the RAM and storage pills. */
final class RingDrawable extends Drawable {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final int sizePx;
    private float fraction;

    RingDrawable(int sizePx, float strokePx) {
        this.sizePx = sizePx;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(strokePx);
        track.setColor(0x40FFFFFF);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(strokePx);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(0xF2FFFFFF);
        setBounds(0, 0, sizePx, sizePx);
    }

    void setFraction(float f) {
        fraction = Math.max(0f, Math.min(1f, f));
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        float inset = track.getStrokeWidth() / 2f + 0.5f;
        box.set(getBounds().left + inset, getBounds().top + inset, getBounds().right - inset, getBounds().bottom - inset);
        canvas.drawOval(box, track);
        if (fraction > 0f) canvas.drawArc(box, -90f, 360f * fraction, false, arc);
    }

    @Override
    public int getIntrinsicWidth() { return sizePx; }

    @Override
    public int getIntrinsicHeight() { return sizePx; }

    @Override
    public void setAlpha(int alpha) { track.setAlpha(alpha / 4); arc.setAlpha(alpha); }

    @Override
    public void setColorFilter(ColorFilter cf) { arc.setColorFilter(cf); }

    @Override
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
