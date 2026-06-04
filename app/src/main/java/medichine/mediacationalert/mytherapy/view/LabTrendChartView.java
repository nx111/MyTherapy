package medichine.mediacationalert.mytherapy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.model.LabResult;
import medichine.mediacationalert.mytherapy.model.LabTestItem;

public class LabTrendChartView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rangeLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF plot = new RectF();
    private LabTestItem item;
    private List<LabResult> results = new ArrayList<>();
    private OnResultClickListener resultClickListener;

    public LabTrendChartView(Context context) {
        super(context);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(getResources().getColor(R.color.history_row_divider));

        textPaint.setTextSize(sp(11));
        textPaint.setColor(getResources().getColor(R.color.text_secondary));

        rangePaint.setStyle(Paint.Style.FILL);
        rangePaint.setColor(Color.argb(42, 0, 122, 77));

        rangeLinePaint.setStyle(Paint.Style.STROKE);
        rangeLinePaint.setStrokeWidth(dp(1));
        rangeLinePaint.setColor(getResources().getColor(R.color.history_taken));
        rangeLinePaint.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(4)}, 0));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(getResources().getColor(R.color.nav_selected));

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(getResources().getColor(R.color.nav_selected));

        pointStrokePaint.setStyle(Paint.Style.STROKE);
        pointStrokePaint.setStrokeWidth(dp(2));
        pointStrokePaint.setColor(getResources().getColor(R.color.surface));
    }

    public void setData(LabTestItem item, List<LabResult> results) {
        this.item = item;
        this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
        invalidate();
    }

    public void setOnResultClickListener(OnResultClickListener listener) {
        resultClickListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results.isEmpty()) {
            drawNoData(canvas);
            return;
        }

        plot.set(dp(52), dp(18), getWidth() - dp(16), getHeight() - dp(36));
        if (plot.width() <= 0 || plot.height() <= 0) {
            return;
        }

        double min = chartMin();
        double max = chartMax();
        double span = max - min;
        if (span <= 0.000001) {
            min -= 1;
            max += 1;
            span = max - min;
        }

        drawReferenceRange(canvas, min, span);
        drawGrid(canvas, min, max);
        drawTrend(canvas, min, span);
        drawDateLabels(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP || results.isEmpty() || plot.width() <= 0 || plot.height() <= 0) {
            return true;
        }
        int index = nearestResultIndex(event.getX(), event.getY());
        if (index >= 0 && resultClickListener != null) {
            resultClickListener.onResultClick(results.get(index));
        }
        return true;
    }

    private void drawNoData(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(getResources().getString(R.string.no_data),
                getWidth() / 2f,
                getHeight() / 2f,
                textPaint);
    }

    private void drawReferenceRange(Canvas canvas, double min, double span) {
        if (item == null || item.mReferenceMin == null || item.mReferenceMax == null
                || item.mReferenceMax < item.mReferenceMin) {
            return;
        }
        float top = yFor(item.mReferenceMax, min, span);
        float bottom = yFor(item.mReferenceMin, min, span);
        RectF band = new RectF(plot.left, Math.min(top, bottom), plot.right, Math.max(top, bottom));
        canvas.drawRect(band, rangePaint);
        canvas.drawLine(plot.left, top, plot.right, top, rangeLinePaint);
        canvas.drawLine(plot.left, bottom, plot.right, bottom, rangeLinePaint);
    }

    private void drawGrid(Canvas canvas, double min, double max) {
        textPaint.setTextAlign(Paint.Align.RIGHT);
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float y = plot.top + plot.height() * i / steps;
            canvas.drawLine(plot.left, y, plot.right, y, gridPaint);
            double value = max - (max - min) * i / steps;
            canvas.drawText(formatQuantity(value), plot.left - dp(6), y + dp(4), textPaint);
        }
    }

    private void drawTrend(Canvas canvas, double min, double span) {
        Path path = new Path();
        for (int i = 0; i < results.size(); i++) {
            float x = xFor(i);
            float y = yFor(results.get(i).mValue, min, span);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);
        for (int i = 0; i < results.size(); i++) {
            float x = xFor(i);
            float y = yFor(results.get(i).mValue, min, span);
            canvas.drawCircle(x, y, dp(5), pointPaint);
            canvas.drawCircle(x, y, dp(5), pointStrokePaint);
        }
    }

    private void drawDateLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(shortDate(results.get(0).mCreatedAt), plot.left, getHeight() - dp(10), textPaint);
        if (results.size() > 1) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(shortDate(results.get(results.size() - 1).mCreatedAt),
                    plot.right,
                    getHeight() - dp(10),
                    textPaint);
        }
    }

    private float xFor(int index) {
        if (results.size() <= 1) {
            return plot.centerX();
        }
        return plot.left + plot.width() * index / (results.size() - 1);
    }

    private float yFor(double value, double min, double span) {
        float ratio = (float) ((value - min) / span);
        return plot.bottom - plot.height() * ratio;
    }

    private int nearestResultIndex(float touchX, float touchY) {
        double min = chartMin();
        double max = chartMax();
        double span = max - min;
        if (span <= 0.000001) {
            min -= 1;
            span = 2;
        }

        int nearest = -1;
        float bestDistance = dp(28);
        for (int i = 0; i < results.size(); i++) {
            float dx = touchX - xFor(i);
            float dy = touchY - yFor(results.get(i).mValue, min, span);
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }

    private double chartMin() {
        double min = results.get(0).mValue;
        for (LabResult result : results) {
            min = Math.min(min, result.mValue);
        }
        if (item != null) {
            if (item.mReferenceMin != null) {
                min = Math.min(min, item.mReferenceMin);
            }
            if (item.mReferenceMax != null) {
                min = Math.min(min, item.mReferenceMax);
            }
        }
        return min - paddingForRange();
    }

    private double chartMax() {
        double max = results.get(0).mValue;
        for (LabResult result : results) {
            max = Math.max(max, result.mValue);
        }
        if (item != null) {
            if (item.mReferenceMin != null) {
                max = Math.max(max, item.mReferenceMin);
            }
            if (item.mReferenceMax != null) {
                max = Math.max(max, item.mReferenceMax);
            }
        }
        return max + paddingForRange();
    }

    private double paddingForRange() {
        double min = results.get(0).mValue;
        double max = results.get(0).mValue;
        for (LabResult result : results) {
            min = Math.min(min, result.mValue);
            max = Math.max(max, result.mValue);
        }
        if (item != null) {
            if (item.mReferenceMin != null) {
                min = Math.min(min, item.mReferenceMin);
                max = Math.max(max, item.mReferenceMin);
            }
            if (item.mReferenceMax != null) {
                min = Math.min(min, item.mReferenceMax);
                max = Math.max(max, item.mReferenceMax);
            }
        }
        double span = max - min;
        return span <= 0.000001 ? 1 : span * 0.12;
    }

    private String shortDate(String createdAt) {
        if (createdAt == null) {
            return "";
        }
        String value = createdAt.trim();
        return value.length() >= 10 ? value.substring(5, 10) : value;
    }

    private String formatQuantity(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    public interface OnResultClickListener {
        void onResultClick(LabResult result);
    }
}
