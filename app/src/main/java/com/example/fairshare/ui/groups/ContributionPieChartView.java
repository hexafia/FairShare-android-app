package com.example.fairshare.ui.groups;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.fairshare.ContributionSummary;

import java.util.ArrayList;
import java.util.List;

public class ContributionPieChartView extends View {

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF chartBounds = new RectF();
    private final List<ContributionSummary> contributions = new ArrayList<>();

    private final int[] palette = {
            Color.parseColor("#2CB4A5"),
            Color.parseColor("#FF9F1C"),
            Color.parseColor("#54A0FF"),
            Color.parseColor("#FF6B81"),
            Color.parseColor("#1DD1A1"),
            Color.parseColor("#7E57C2")
    };

    public ContributionPieChartView(Context context) {
        super(context);
        init();
    }

    public ContributionPieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContributionPieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        holePaint.setColor(Color.WHITE);
        centerTextPaint.setColor(Color.parseColor("#1A1A2E"));
        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setTextSize(42f);
        centerTextPaint.setFakeBoldText(true);
    }

    public void setContributions(List<ContributionSummary> items) {
        contributions.clear();
        if (items != null) {
            contributions.addAll(items);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height) * 0.82f;
        float left = (width - size) / 2f;
        float top = (height - size) / 2f;
        chartBounds.set(left, top, left + size, top + size);

        if (contributions.isEmpty()) {
            canvas.drawText("No data", width / 2f, height / 2f, centerTextPaint);
            return;
        }

        float startAngle = -90f;
        for (int i = 0; i < contributions.size(); i++) {
            ContributionSummary summary = contributions.get(i);
            float sweepAngle = (float) ((summary.getPercentage() / 100d) * 360d);
            slicePaint.setColor(palette[i % palette.length]);
            canvas.drawArc(chartBounds, startAngle, sweepAngle, true, slicePaint);
            startAngle += sweepAngle;
        }

        float holeRadius = size * 0.23f;
        canvas.drawCircle(width / 2f, height / 2f, holeRadius, holePaint);
        canvas.drawText("Equity", width / 2f, (height / 2f) + 14f, centerTextPaint);
    }
}
