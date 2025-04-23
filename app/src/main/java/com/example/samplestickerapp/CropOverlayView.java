package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropOverlayView extends View {

    private Paint borderPaint;
    private Rect cropRect;

    // Flags de modo de interação
    private enum DragMode { NONE, MOVE, RESIZE_LEFT, RESIZE_TOP, RESIZE_RIGHT, RESIZE_BOTTOM }
    private DragMode dragMode = DragMode.NONE;

    // Tamanhos mínimo e máximo (em pixels)
    private int minCropSize;
    private int maxCropSize = Integer.MAX_VALUE;

    // Área de toque para detecção de bordas
    private int touchAreaSize;

    // Última posição de toque
    private int lastX, lastY;

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        // Inicializa paint
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3 * density);

        // Tamanho mínimo: 50dp
        minCropSize = (int)(50 * density + 0.5f);
        // Área de detecção de borda: 20dp
        touchAreaSize = (int)(20 * density + 0.5f);

        // Inicializa um quadrado central padrão (será reposicionado pelo Activity)
        cropRect = new Rect(100, 100, 100 + minCropSize, 100 + minCropSize);
    }

    /** Define o tamanho máximo permitido (p.ex. largura do VideoView) */
    public void setMaxCropSize(int max) {
        this.maxCropSize = max;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Define um quadrado centralizado com tamanho igual ao menor lado da view
        int size = Math.min(w, h);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        cropRect.set(left, top, left + size, top + size);
    }

    /** Centraliza o crop com um tamanho inicial */
    public void centerInitialCrop(int w, int h) {
        int size = Math.min(maxCropSize, Math.min(w, h) - 2 * touchAreaSize);
        size = Math.max(size, minCropSize);

        int cx = w / 2;
        int cy = h / 2;
        cropRect.left = cx - size/2;
        cropRect.top  = cy - size/2;
        cropRect.right= cx + size/2;
        cropRect.bottom=cy + size/2;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Cria uma camada semitransparente cinza sobre toda a tela
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#88000000")); // Cinza com 50% de opacidade

        // Salva o estado atual do canvas
        int saveCount = canvas.save();

        // Define a área de recorte para excluir o retângulo de corte
        canvas.clipRect(cropRect, Region.Op.DIFFERENCE);

        // Desenha a sobreposição fora do retângulo de corte
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Restaura o estado do canvas
        canvas.restoreToCount(saveCount);

        // Desenha a borda branca ao redor do retângulo de corte
        canvas.drawRect(cropRect, borderPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int x = (int)ev.getX();
        int y = (int)ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                dragMode = detectDragMode(x, y);

                outsideCropClickListener.onOutsideClick(false);
                if (dragMode == DragMode.NONE) {
                    outsideCropClickListener.onOutsideClick(true);
                }

                return dragMode != DragMode.NONE;

            case MotionEvent.ACTION_MOVE:
                int dx = x - lastX;
                int dy = y - lastY;

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
                    default:
                        break;
                }

                invalidate();
                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_UP:
                outsideCropClickListener.onOutsideClick(true);
            case MotionEvent.ACTION_CANCEL:
                dragMode = DragMode.NONE;
                return true;
        }
        return super.onTouchEvent(ev);
    }

    /** Decide se o usuário quer mover ou redimensionar e por qual borda */
    private DragMode detectDragMode(int x, int y) {
        if (isNear(cropRect.left, x) && y >= cropRect.top && y <= cropRect.bottom) {
            return DragMode.RESIZE_LEFT;
        }
        if (isNear(cropRect.right, x) && y >= cropRect.top && y <= cropRect.bottom) {
            return DragMode.RESIZE_RIGHT;
        }
        if (isNear(cropRect.top, y) && x >= cropRect.left && x <= cropRect.right) {
            return DragMode.RESIZE_TOP;
        }
        if (isNear(cropRect.bottom, y) && x >= cropRect.left && x <= cropRect.right) {
            return DragMode.RESIZE_BOTTOM;
        }
        if (cropRect.contains(x, y)) {
            return DragMode.MOVE;
        }
        return DragMode.NONE;
    }

    private boolean isNear(int edgeCoord, int touchCoord) {
        return Math.abs(edgeCoord - touchCoord) <= touchAreaSize;
    }

    /**
     * Redimensiona o quadrado mantendo-o proporcional (1:1).
     * @param delta   Quanto deseja aumentar (positivo) ou diminuir (negativo).
     * @param expandRightOrBottom  Se true, ancoramos na borda esquerda/topo; senão, na borda direita/fundo.
     */
    private void resizeFromEdge(int delta, boolean expandRightOrBottom) {
        int oldSize = cropRect.width();
        int newSize = oldSize + delta;
        newSize = Math.max(newSize, minCropSize);
        newSize = Math.min(newSize, maxCropSize);

        // Calcula deslocamento para manter quadrado
        int diff = newSize - oldSize;

        if (expandRightOrBottom) {
            // ancoragem em left/top
            cropRect.right = cropRect.left + newSize;
            cropRect.bottom= cropRect.top  + newSize;
        } else {
            // ancoragem em right/bottom
            cropRect.left  = cropRect.right - newSize;
            cropRect.top   = cropRect.bottom- newSize;
        }
        clampInside();
    }

    /** Garante que o crop permaneça dentro dos limites da view */
    private void clampInside() {
        int w = getWidth();
        int h = getHeight();
        // Left/Top não saírem de 0
        if (cropRect.left < 0) {
            cropRect.offset(-cropRect.left, 0);
        }
        if (cropRect.top < 0) {
            cropRect.offset(0, -cropRect.top);
        }
        // Right/Bottom não saírem de w/h
        if (cropRect.right > w) {
            cropRect.offset(w - cropRect.right, 0);
        }
        if (cropRect.bottom > h) {
            cropRect.offset(0, h - cropRect.bottom);
        }
    }

    /** Para aplicação externa: obtenha o retângulo de crop atual */
    public Rect getCropRect() {
        return new Rect(cropRect);
    }

    public interface OnOutsideCropClickListener {
        void onOutsideClick(boolean flag);
    }

    private OnOutsideCropClickListener outsideCropClickListener;

    public void setOnOutsideCropClickListener(OnOutsideCropClickListener listener) {
        this.outsideCropClickListener = listener;
    }

}
