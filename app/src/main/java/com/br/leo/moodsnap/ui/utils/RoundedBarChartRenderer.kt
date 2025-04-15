package com.br.leo.moodsnap.ui.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    init {
        mHighlightPaint.style = Paint.Style.FILL
        mHighlightPaint.color = Color.WHITE
        mHighlightPaint.alpha = 80
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)
        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = Utils.convertDpToPixel(dataSet.barBorderWidth)

        val drawBorder = dataSet.barBorderWidth > 0f
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY
        val barData = mChart.barData

        val buffer = mBarBuffers.getOrNull(index) ?: return // ✅ Garante que o buffer exista
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(barData.barWidth)
        buffer.feed(dataSet)

        val coordinates = buffer.buffer
        if (coordinates == null || coordinates.isEmpty()) return // ✅ Verifica antes de usar

        trans.pointValuesToPixel(coordinates)

        val radius = 30f

        for (j in 0 until coordinates.size step 4) {
            val left = coordinates[j]
            val top = coordinates[j + 1]
            val right = coordinates[j + 2]
            val bottom = coordinates[j + 3]

            val path = Path().apply {
                addRoundRect(
                    RectF(left, top, right, bottom),
                    floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }

            mRenderPaint.color = dataSet.getColor(j / 4)
            c.drawPath(path, mRenderPaint)

            if (drawBorder) {
                c.drawPath(path, mBarBorderPaint)
            }
        }
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        val barData = mChart.barData

        for (high in indices) {
            val set = barData.getDataSetByIndex(high.dataSetIndex)

            if (set == null || !set.isHighlightEnabled) continue

            val e = set.getEntryForXValue(high.x, high.y)

            if (!isInBoundsX(e, set)) continue

            val trans = mChart.getTransformer(set.axisDependency)

            mHighlightPaint.color = set.highLightColor
            mHighlightPaint.alpha = set.highLightAlpha

            val isStack = if (high.stackIndex >= 0 && e.isStacked) true else false

            val y1: Float
            val y2: Float

            if (isStack) {
                if (mChart.isHighlightFullBarEnabled) {
                    y1 = e.positiveSum
                    y2 = -e.negativeSum
                } else {
                    val range = e.ranges[high.stackIndex]

                    y1 = range.from
                    y2 = range.to
                }
            } else {
                y1 = e.y
                y2 = 0f
            }

            prepareBarHighlight(e.x, y1, y2, barData.barWidth / 2f, trans)

            setHighlightDrawPos(high, mBarRect)

            // Substituir drawRect por addRoundRect para bordas arredondadas
            val radius = 30f // Tamanho do raio para os cantos arredondados

            val path = Path().apply {
                addRoundRect(
                    mBarRect,
                    floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }

            c.drawPath(path, mHighlightPaint) // Desenhar o path com bordas arredondadas
        }
    }



}
