package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropOverlayView extends View {
    private Paint borderPaint;
    private Rect cropRect;
    private int minCropSize, maxCropSize = Integer.MAX_VALUE;
    private int touchAreaSize;
    private int lastX, lastY;

    private enum DragMode {NONE, MOVE, RESIZE_LEFT, RESIZE_TOP, RESIZE_RIGHT, RESIZE_BOTTOM}

    private DragMode dragMode = DragMode.NONE;

    public interface OnOutsideCropClickListener {
        void onOutsideClick(boolean flag);
    }

    private OnOutsideCropClickListener outsideCropClickListener;

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3 * density);

        minCropSize = (int) (50 * density + 0.5f);
        touchAreaSize = (int) (20 * density + 0.5f);
        cropRect = new Rect(100, 100, 100 + minCropSize, 100 + minCropSize);
    }

    public void setMaxCropSize(int max) {
        this.maxCropSize = max;
    }

    public void centerInitialCrop(int w, int h) {
        int size = Math.min(maxCropSize, Math.min(w, h) - 2 * touchAreaSize);
        size = Math.max(size, minCropSize);
        int cx = w / 2, cy = h / 2;
        cropRect.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        int size = Math.min(w, h);
        int left = (w - size) / 2, top = (h - size) / 2;
        cropRect.set(left, top, left + size, top + size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#88000000"));
        int save = canvas.save();
        canvas.clipRect(cropRect, Region.Op.DIFFERENCE);
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
        canvas.restoreToCount(save);
        canvas.drawRect(cropRect, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX(), y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                dragMode = detectDragMode(x, y);
                outsideCropClickListener.onOutsideClick(dragMode == DragMode.NONE);
                return dragMode != DragMode.NONE;
            case MotionEvent.ACTION_MOVE:
                int dx = x - lastX, dy = y - lastY;
                switch (dragMode) {
                    case MOVE:
                        cropRect.offset(dx, dy);
                        clampInside();
                        break;
                    case RESIZE_RIGHT:
                        resizeFromEdge(dx, true);
                        break;
                    case RESIZE_LEFT:
                        resizeFromEdge(-dx, false);
                        break;
                    case RESIZE_BOTTOM:
                        resizeFromEdge(dy, true);
                        break;
                    case RESIZE_TOP:
                        resizeFromEdge(-dy, false);
                        break;
                }
                invalidate();
                lastX = x;
                lastY = y;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                outsideCropClickListener.onOutsideClick(true);
                dragMode = DragMode.NONE;
                return true;
        }
        return super.onTouchEvent(ev);
    }

    private DragMode detectDragMode(int x, int y) {
        if (isNear(cropRect.left, x) && y >= cropRect.top && y <= cropRect.bottom)
            return DragMode.RESIZE_LEFT;
        if (isNear(cropRect.right, x) && y >= cropRect.top && y <= cropRect.bottom)
            return DragMode.RESIZE_RIGHT;
        if (isNear(cropRect.top, y) && x >= cropRect.left && x <= cropRect.right)
            return DragMode.RESIZE_TOP;
        if (isNear(cropRect.bottom, y) && x >= cropRect.left && x <= cropRect.right)
            return DragMode.RESIZE_BOTTOM;
        if (cropRect.contains(x, y)) return DragMode.MOVE;
        return DragMode.NONE;
    }

    private boolean isNear(int edge, int touch) {
        return Math.abs(edge - touch) <= touchAreaSize;
    }

    private void resizeFromEdge(int delta, boolean expandRightOrBottom) {
        int oldSize = cropRect.width();
        int newSize = Math.max(minCropSize, Math.min(oldSize + delta, maxCropSize));
        if (expandRightOrBottom) {
            cropRect.right = cropRect.left + newSize;
            cropRect.bottom = cropRect.top + newSize;
        } else {
            cropRect.left = cropRect.right - newSize;
            cropRect.top = cropRect.bottom - newSize;
        }
        clampInside();
    }

    private void clampInside() {
        int w = getWidth(), h = getHeight();
        if (cropRect.left < 0) cropRect.offset(-cropRect.left, 0);
        if (cropRect.top < 0) cropRect.offset(0, -cropRect.top);
        if (cropRect.right > w) cropRect.offset(w - cropRect.right, 0);
        if (cropRect.bottom > h) cropRect.offset(0, h - cropRect.bottom);
    }

    public Rect getCropRect() {
        return new Rect(cropRect);
    }

    public void setOnOutsideCropClickListener(OnOutsideCropClickListener l) {
        outsideCropClickListener = l;
    }
}
