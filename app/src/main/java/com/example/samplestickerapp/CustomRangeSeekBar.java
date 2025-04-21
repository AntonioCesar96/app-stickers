package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CustomRangeSeekBar extends View {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int thumbRadius = 30;
    private int thumbStartX = 0;
    private int thumbEndX = 500;
    private int heightCenter;
    private int videoDuration = 10000;

    private OnRangeChangedListener rangeChangedListener;
    private OnRangeReleasedListener rangeReleasedListener;

    private int maxRangeMs = 8000;

    public CustomRangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeWidth(4);
    }

    public void setVideoDuration(int durationMs) {
        this.videoDuration = durationMs;

        int visibleDuration = Math.min(videoDuration, maxRangeMs);

        // Calcular a nova posição dos thumbs com base na largura da View
        post(() -> {
            int totalWidth = getWidth() - thumbRadius * 2;

            thumbStartX = thumbRadius;
            thumbEndX = thumbStartX + (int) ((visibleDuration * 1.0f / videoDuration) * totalWidth);

            invalidate();

            // Notificar listeners imediatamente após o ajuste, se necessário
            if (rangeChangedListener != null) {
                rangeChangedListener.onRangeChanged(getStartMs(), getEndMs());
            }
        });
    }

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        this.rangeChangedListener = listener;
    }

    public void setOnRangeReleasedListener(OnRangeReleasedListener listener) {
        this.rangeReleasedListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        heightCenter = getHeight() / 2;

        int startMs = getStartMs();
        int endMs = getEndMs();

        // Linha de fundo
        paint.setColor(Color.GRAY);
        canvas.drawLine(thumbRadius, heightCenter, getWidth() - thumbRadius, heightCenter, paint);

        // Linha azul de seleção
        paint.setColor(Color.BLUE);
        canvas.drawLine(thumbStartX, heightCenter, thumbEndX, heightCenter, paint);

        // Desenhar círculos (pinos)
        paint.setColor(Color.WHITE);
        canvas.drawCircle(thumbStartX, heightCenter, thumbRadius, paint);
        canvas.drawCircle(thumbEndX, heightCenter, thumbRadius, paint);

        // Desenhar textos dos valores
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);
        paint.setTextAlign(Paint.Align.CENTER);

        // Converter para segundos com 1 casa decimal
        String startSeconds = String.format("%.1fs", startMs / 1000f);
        String endSeconds = String.format("%.1fs", endMs / 1000f);

        // Texto abaixo do thumb inicial
        canvas.drawText(startSeconds, thumbStartX, heightCenter + thumbRadius + 40, paint);

        // Texto acima do thumb final
        canvas.drawText(endSeconds, thumbEndX, heightCenter + thumbRadius + 40, paint);

        // Duração total entre os thumbs
        int durationMs = endMs - startMs;
        String durationSeconds = String.format("%.1fs", durationMs / 1000f);

        // Posição central entre os thumbs
        int centerX = (thumbStartX + thumbEndX) / 2;

        // Texto da duração no centro abaixo da linha azul
        canvas.drawText(durationSeconds, centerX, heightCenter + thumbRadius + 80, paint);

    }



    private boolean movingStart = false;
    private boolean movingEnd = false;
    private boolean movingBoth = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Calcula a margem de 10% antes do pino inicial e depois do pino final
                int rangeWidth = thumbEndX - thumbStartX;
                int margin = (int) (rangeWidth * 0.1);  // 10% da largura do intervalo

                // Verifica se o toque está dentro das margens para mover ambos os pinos
                if (x > (thumbStartX + margin) && x < (thumbEndX - margin)) {
                    movingBoth = true;
                } else {
                    movingStart = Math.abs(x - thumbStartX) < thumbRadius * 2;
                    movingEnd = Math.abs(x - thumbEndX) < thumbRadius * 2;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                int totalWidth = getWidth() - thumbRadius * 2;

                if (movingBoth) {
                    // Move ambos os pinos para a posição do toque
                    int offsetX = x - (thumbEndX + thumbStartX) / 2;
                    int newStartX = Math.max(thumbRadius, Math.min(thumbStartX + offsetX, thumbEndX - thumbRadius * 2));
                    int newEndX = newStartX + (thumbEndX - thumbStartX);

                    // Garantir que o final não ultrapasse o limite da view
                    newEndX = Math.min(newEndX, getWidth() - thumbRadius);

                    thumbStartX = newStartX;
                    thumbEndX = newEndX;
                } else if (movingStart) {
                    int proposedStartX = Math.max(thumbRadius, Math.min(x, thumbEndX - thumbRadius * 2));
                    int proposedStartMs = (int) ((proposedStartX - thumbRadius) * 1.0f / totalWidth * videoDuration);
                    int endMs = (int) ((thumbEndX - thumbRadius) * 1.0f / totalWidth * videoDuration);

                    if (endMs - proposedStartMs > maxRangeMs) {
                        int newEndMs = proposedStartMs + maxRangeMs;
                        thumbStartX = proposedStartX;
                        thumbEndX = thumbRadius + (int) ((newEndMs * 1.0f / videoDuration) * totalWidth);
                    } else {
                        thumbStartX = proposedStartX;
                    }

                } else if (movingEnd) {
                    int proposedEndX = Math.min(getWidth() - thumbRadius, Math.max(x, thumbStartX + thumbRadius * 2));
                    int startMs = (int) ((thumbStartX - thumbRadius) * 1.0f / totalWidth * videoDuration);
                    int proposedEndMs = (int) ((proposedEndX - thumbRadius) * 1.0f / totalWidth * videoDuration);

                    if (proposedEndMs - startMs > maxRangeMs) {
                        int newStartMs = proposedEndMs - maxRangeMs;
                        thumbEndX = proposedEndX;
                        thumbStartX = thumbRadius + (int) ((newStartMs * 1.0f / videoDuration) * totalWidth);
                    } else {
                        thumbEndX = proposedEndX;
                    }
                }

                invalidate();

                if (rangeChangedListener != null) {
                    rangeChangedListener.onRangeChanged(getStartMs(), getEndMs());
                }
                return true;

            case MotionEvent.ACTION_UP:
                movingStart = false;
                movingEnd = false;
                movingBoth = false;

                if (rangeReleasedListener != null) {
                    rangeReleasedListener.onRangeReleased(getStartMs(), getEndMs());
                }
                return true;
        }
        return super.onTouchEvent(event);
    }


    public int getStartMs() {
        return (int) ((thumbStartX - thumbRadius) * 1.0f / (getWidth() - thumbRadius * 2) * videoDuration);
    }

    public int getEndMs() {
        return (int) ((thumbEndX - thumbRadius) * 1.0f / (getWidth() - thumbRadius * 2) * videoDuration);
    }

    public interface OnRangeChangedListener {
        void onRangeChanged(int startMs, int endMs);
    }

    public interface OnRangeReleasedListener {
        void onRangeReleased(int startMs, int endMs);
    }
}
