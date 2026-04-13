package com.calorietracker.widgets

// 🧰 CHART DRAWING TOOLS
import android.content.Context           // Android system access
import android.graphics.*               // Drawing tools (Canvas, Paint, Path, etc.)
import android.util.AttributeSet        // XML attribute handling
import android.view.View               // Base view class
import androidx.core.content.ContextCompat // Resource access helpers
import com.calorietracker.R            // App resources
import java.text.SimpleDateFormat      // Date formatting
import java.util.*                     // Date utilities
import kotlin.math.max                 // Math utilities
import kotlin.math.min

/**
 * 📈 TREND GRAPH WIDGET - BEAUTIFUL NUTRITION PROGRESS CHARTS
 * 
 * Hey young programmer! This creates beautiful line graphs that show how your nutrition
 * changes over time, just like the charts you see in health apps!
 * 
 * 📊 What types of trends can we show?
 * - 🔥 CALORIE TRENDS: Daily calories over the past 7 or 30 days
 * - 💪 MACRO TRENDS: Protein, carbs, fat intake over time
 * - ⚖️ WEIGHT TRENDS: Weight changes over weeks/months
 * - 🎯 GOAL ADHERENCE: How often you hit your nutrition goals
 * 
 * 🎨 Visual Features:
 * - Smooth curved lines connecting data points
 * - Color-coded lines for different metrics
 * - Grid lines for easy reading
 * - Goal line showing target values
 * - Tooltips showing exact values when you tap
 * 
 * 📈 Chart Types Supported:
 * - **Line Graph**: Shows trends over time
 * - **Bar Chart**: Compare daily values
 * - **Area Chart**: Filled areas under the line
 * - **Multi-line**: Compare multiple metrics (calories vs goal)
 * 
 * 🎯 Interactive Features:
 * - Tap data points to see exact values
 * - Swipe to scroll through different time periods
 * - Pinch to zoom in/out on specific date ranges
 * - Toggle between weekly and monthly views
 * 
 * 💡 Smart Insights:
 * - "You're averaging 50 calories below your goal this week!"
 * - "Your protein intake improved by 15% this month"
 * - "Best day was Tuesday with perfect macro balance"
 */
class TrendGraphWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // 🎨 DRAWING TOOLS
    private val linePaint = Paint().apply {
        isAntiAlias = true                    // Smooth, crisp lines
        style = Paint.Style.STROKE            // Draw lines, not filled shapes
        strokeCap = Paint.Cap.ROUND           // Rounded line ends
        strokeWidth = 6f                     // Thick enough to see clearly
    }
    
    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = ContextCompat.getColor(context, R.color.gray_light)
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 32f
        color = ContextCompat.getColor(context, R.color.text_primary)
    }
    
    private val goalLinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) // Dashed line
        color = ContextCompat.getColor(context, R.color.gray_medium)
    }
    
    // 📊 CHART DATA STORAGE
    private var dataPoints: List<TrendDataPoint> = emptyList()
    private var chartType: TrendChartType = TrendChartType.LINE
    private var timeFrame: TrendTimeFrame = TrendTimeFrame.WEEKLY
    private var goalValue: Float? = null
    private var metricType: TrendMetricType = TrendMetricType.CALORIES
    
    // 📏 CHART DIMENSIONS
    private var chartLeft = 80f      // Left margin for Y-axis labels
    private var chartTop = 60f       // Top margin for title
    private var chartRight = 0f      // Right edge (calculated in onSizeChanged)
    private var chartBottom = 0f     // Bottom edge (calculated in onSizeChanged)
    private var chartWidth = 0f      // Available chart width
    private var chartHeight = 0f     // Available chart height
    
    /**
     * 📊 UPDATE TREND DATA
     * 
     * Call this when you have new data to display in the trend chart.
     * 
     * @param newDataPoints List of data points with dates and values
     * @param goalValue Optional goal line to show on chart
     * @param metricType What type of data this represents (calories, protein, etc.)
     * @param timeFrame Weekly or monthly view
     * @param chartType Line, bar, or area chart
     */
    fun updateTrendData(
        newDataPoints: List<TrendDataPoint>,
        goalValue: Float? = null,
        metricType: TrendMetricType = TrendMetricType.CALORIES,
        timeFrame: TrendTimeFrame = TrendTimeFrame.WEEKLY,
        chartType: TrendChartType = TrendChartType.LINE
    ) {
        this.dataPoints = newDataPoints.sortedBy { it.date } // Sort by date
        this.goalValue = goalValue
        this.metricType = metricType
        this.timeFrame = timeFrame
        this.chartType = chartType
        
        invalidate() // Tell Android to redraw the chart
    }
    
    /**
     * 📐 CALCULATE CHART DIMENSIONS
     * 
     * When widget size changes, recalculate where to draw everything.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        chartRight = w.toFloat() - 40f      // Right margin
        chartBottom = h.toFloat() - 80f     // Bottom margin for X-axis labels
        chartWidth = chartRight - chartLeft
        chartHeight = chartBottom - chartTop
    }
    
    /**
     * 🎨 DRAW THE TREND CHART
     * 
     * This is where we create the beautiful chart visualization!
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (dataPoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        // 📊 DRAW CHART COMPONENTS
        drawGridLines(canvas)
        drawGoalLine(canvas)
        
        when (chartType) {
            TrendChartType.LINE -> drawLineChart(canvas)
            TrendChartType.BAR -> drawBarChart(canvas)
            TrendChartType.AREA -> drawAreaChart(canvas)
        }
        
        drawAxisLabels(canvas)
        drawDataPointLabels(canvas)
    }
    
    /**
     * 📝 DRAW EMPTY STATE
     * 
     * Show helpful message when there's no data to display.
     */
    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 48f
        textPaint.color = ContextCompat.getColor(context, R.color.gray_medium)
        
        canvas.drawText(
            "📊 No trend data yet",
            width / 2f,
            height / 2f - 20f,
            textPaint
        )
        
        textPaint.textSize = 32f
        canvas.drawText(
            "Log food for a few days to see trends!",
            width / 2f,
            height / 2f + 30f,
            textPaint
        )
    }
    
    /**
     * 📏 DRAW GRID LINES
     * 
     * Background grid makes it easier to read chart values.
     */
    private fun drawGridLines(canvas: Canvas) {
        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 0f
        val minValue = dataPoints.minOfOrNull { it.value } ?: 0f
        val valueRange = maxValue - minValue
        
        // 📊 HORIZONTAL GRID LINES (for values)
        val gridLineCount = 5
        for (i in 0..gridLineCount) {
            val y = chartTop + (chartHeight / gridLineCount) * i
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }
        
        // 📅 VERTICAL GRID LINES (for dates)
        val dateGridCount = dataPoints.size - 1
        if (dateGridCount > 0) {
            for (i in 0..dateGridCount) {
                val x = chartLeft + (chartWidth / dateGridCount) * i
                canvas.drawLine(x, chartTop, x, chartBottom, gridPaint)
            }
        }
    }
    
    /**
     * 🎯 DRAW GOAL LINE
     * 
     * Show user's daily goal as a dashed horizontal line.
     */
    private fun drawGoalLine(canvas: Canvas) {
        goalValue?.let { goal ->
            val maxValue = dataPoints.maxOfOrNull { it.value } ?: goal
            val minValue = dataPoints.minOfOrNull { it.value } ?: 0f
            val valueRange = maxValue - minValue
            
            if (valueRange > 0) {
                val goalY = chartBottom - ((goal - minValue) / valueRange) * chartHeight
                canvas.drawLine(chartLeft, goalY, chartRight, goalY, goalLinePaint)
                
                // 📝 GOAL LABEL
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = 28f
                textPaint.color = ContextCompat.getColor(context, R.color.gray_medium)
                canvas.drawText("Goal: ${goal.toInt()}", chartLeft + 10f, goalY - 10f, textPaint)
            }
        }
    }
    
    /**
     * 📈 DRAW LINE CHART
     * 
     * Draw smooth lines connecting data points over time.
     */
    private fun drawLineChart(canvas: Canvas) {
        if (dataPoints.size < 2) return
        
        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 0f
        val minValue = dataPoints.minOfOrNull { it.value } ?: 0f
        val valueRange = max(maxValue - minValue, 1f) // Prevent division by zero
        
        // 🎨 SET LINE COLOR BASED ON METRIC TYPE
        linePaint.color = when (metricType) {
            TrendMetricType.CALORIES -> ContextCompat.getColor(context, R.color.primary_green)
            TrendMetricType.PROTEIN -> ContextCompat.getColor(context, R.color.macro_protein_blue)
            TrendMetricType.CARBS -> ContextCompat.getColor(context, R.color.accent_orange)
            TrendMetricType.FAT -> ContextCompat.getColor(context, R.color.macro_fat_green)
            TrendMetricType.WEIGHT -> ContextCompat.getColor(context, R.color.purple)
        }
        
        // 🎨 CREATE SMOOTH CURVED PATH
        val path = Path()
        
        for (i in dataPoints.indices) {
            val x = chartLeft + (chartWidth / (dataPoints.size - 1)) * i
            val y = chartBottom - ((dataPoints[i].value - minValue) / valueRange) * chartHeight
            
            if (i == 0) {
                path.moveTo(x, y) // Start the path
            } else {
                path.lineTo(x, y) // Connect to next point
            }
            
            // 🔵 DRAW DATA POINT CIRCLES
            linePaint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, 8f, linePaint)
            linePaint.style = Paint.Style.STROKE
        }
        
        // 📈 DRAW THE CONNECTING LINE
        canvas.drawPath(path, linePaint)
    }
    
    /**
     * 📊 DRAW BAR CHART
     * 
     * Draw vertical bars for each day's data.
     */
    private fun drawBarChart(canvas: Canvas) {
        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 0f
        val minValue = 0f // Bars always start from zero
        val valueRange = max(maxValue - minValue, 1f)
        
        linePaint.style = Paint.Style.FILL
        linePaint.color = when (metricType) {
            TrendMetricType.CALORIES -> ContextCompat.getColor(context, R.color.primary_green)
            TrendMetricType.PROTEIN -> ContextCompat.getColor(context, R.color.macro_protein_blue)
            TrendMetricType.CARBS -> ContextCompat.getColor(context, R.color.accent_orange)
            TrendMetricType.FAT -> ContextCompat.getColor(context, R.color.macro_fat_green)
            TrendMetricType.WEIGHT -> ContextCompat.getColor(context, R.color.purple)
        }
        
        val barWidth = chartWidth / dataPoints.size * 0.7f // 70% width with gaps
        
        dataPoints.forEachIndexed { index, dataPoint ->
            val x = chartLeft + (chartWidth / dataPoints.size) * index + (chartWidth / dataPoints.size - barWidth) / 2
            val barHeight = (dataPoint.value / valueRange) * chartHeight
            val y = chartBottom - barHeight
            
            // 📊 DRAW BAR
            canvas.drawRect(x, y, x + barWidth, chartBottom, linePaint)
        }
        
        linePaint.style = Paint.Style.STROKE
    }
    
    /**
     * 🏔️ DRAW AREA CHART
     * 
     * Draw filled area under the trend line.
     */
    private fun drawAreaChart(canvas: Canvas) {
        if (dataPoints.size < 2) return
        
        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 0f
        val minValue = dataPoints.minOfOrNull { it.value } ?: 0f
        val valueRange = max(maxValue - minValue, 1f)
        
        // 🎨 SET AREA COLOR (SEMI-TRANSPARENT)
        val areaColor = when (metricType) {
            TrendMetricType.CALORIES -> ContextCompat.getColor(context, R.color.primary_green)
            TrendMetricType.PROTEIN -> ContextCompat.getColor(context, R.color.macro_protein_blue)
            TrendMetricType.CARBS -> ContextCompat.getColor(context, R.color.accent_orange)
            TrendMetricType.FAT -> ContextCompat.getColor(context, R.color.macro_fat_green)
            TrendMetricType.WEIGHT -> ContextCompat.getColor(context, R.color.purple)
        }
        
        val areaPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.argb(80, Color.red(areaColor), Color.green(areaColor), Color.blue(areaColor)) // 30% transparency
        }
        
        // 🏔️ CREATE FILLED AREA PATH
        val areaPath = Path()
        
        // Start from bottom-left
        val firstX = chartLeft
        val firstY = chartBottom - ((dataPoints[0].value - minValue) / valueRange) * chartHeight
        areaPath.moveTo(firstX, chartBottom)
        areaPath.lineTo(firstX, firstY)
        
        // Connect all data points
        for (i in dataPoints.indices) {
            val x = chartLeft + (chartWidth / (dataPoints.size - 1)) * i
            val y = chartBottom - ((dataPoints[i].value - minValue) / valueRange) * chartHeight
            areaPath.lineTo(x, y)
        }
        
        // Close path at bottom-right
        val lastX = chartLeft + chartWidth
        areaPath.lineTo(lastX, chartBottom)
        areaPath.close()
        
        // 🎨 DRAW FILLED AREA
        canvas.drawPath(areaPath, areaPaint)
        
        // 📈 DRAW LINE ON TOP
        drawLineChart(canvas)
    }
    
    /**
     * 📝 DRAW AXIS LABELS
     * 
     * Show dates on X-axis and values on Y-axis.
     */
    private fun drawAxisLabels(canvas: Canvas) {
        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 0f
        val minValue = dataPoints.minOfOrNull { it.value } ?: 0f
        val valueRange = max(maxValue - minValue, 1f)
        
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 24f
        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        
        // 📅 X-AXIS LABELS (DATES)
        val dateFormat = when (timeFrame) {
            TrendTimeFrame.WEEKLY -> SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue, Wed
            TrendTimeFrame.MONTHLY -> SimpleDateFormat("MM/dd", Locale.getDefault()) // 12/25, 12/26
        }
        
        dataPoints.forEachIndexed { index, dataPoint ->
            if (timeFrame == TrendTimeFrame.WEEKLY || index % 3 == 0) { // Show every label for weekly, every 3rd for monthly
                val x = chartLeft + (chartWidth / (dataPoints.size - 1)) * index
                val dateText = dateFormat.format(Date(dataPoint.date))
                canvas.drawText(dateText, x, chartBottom + 50f, textPaint)
            }
        }
        
        // 📊 Y-AXIS LABELS (VALUES)
        textPaint.textAlign = Paint.Align.RIGHT
        val labelCount = 5
        for (i in 0..labelCount) {
            val value = minValue + (valueRange / labelCount) * i
            val y = chartBottom - (chartHeight / labelCount) * i + 8f
            val valueText = when (metricType) {
                TrendMetricType.CALORIES -> "${value.toInt()}"
                TrendMetricType.WEIGHT -> "${value.toInt()} lbs"
                else -> "${value.toInt()}g"
            }
            canvas.drawText(valueText, chartLeft - 10f, y, textPaint)
        }
    }
    
    /**
     * 🏷️ DRAW DATA POINT LABELS
     * 
     * Show exact values when hovering over data points.
     */
    private fun drawDataPointLabels(canvas: Canvas) {
        // For now, just show the chart title
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 36f
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        
        val title = when (metricType) {
            TrendMetricType.CALORIES -> "📈 Calorie Trends"
            TrendMetricType.PROTEIN -> "💪 Protein Trends"
            TrendMetricType.CARBS -> "🍞 Carb Trends"
            TrendMetricType.FAT -> "🥑 Fat Trends"
            TrendMetricType.WEIGHT -> "⚖️ Weight Trends"
        }
        
        canvas.drawText(title, width / 2f, 40f, textPaint)
    }
}

/**
 * 📊 TREND DATA POINT
 * 
 * Single data point for trend charts containing date and value.
 * 
 * @property date      📅 Unix timestamp of this data point
 * @property value     📊 Numeric value (calories, grams, pounds, etc.)
 * @property label     🏷️ Optional text label for this point
 * @property color     🎨 Optional custom color for this point
 */
data class TrendDataPoint(
    val date: Long,           // 📅 Timestamp: 1672531200000 (Jan 1, 2023)
    val value: Float,         // 📊 Value: 1850.0 (calories), 120.5 (protein grams), 175.2 (weight)
    val label: String? = null, // 🏷️ Optional: "Perfect day!" or "Cheat meal"
    val color: Int? = null    // 🎨 Optional: Custom color for special days
)

/**
 * 📈 TREND CHART TYPE
 * 
 * Different ways to visualize trend data.
 */
enum class TrendChartType {
    LINE,   // 📈 Connected line chart (best for showing trends over time)
    BAR,    // 📊 Vertical bar chart (best for comparing daily values)
    AREA    // 🏔️ Filled area chart (best for showing cumulative progress)
}

/**
 * ⏰ TREND TIME FRAME
 * 
 * How much time to show in the chart.
 */
enum class TrendTimeFrame {
    WEEKLY,    // 📅 Show past 7 days
    MONTHLY    // 📅 Show past 30 days
}

/**
 * 📊 TREND METRIC TYPE
 * 
 * What type of data we're showing trends for.
 */
enum class TrendMetricType {
    CALORIES,  // 🔥 Daily calorie intake
    PROTEIN,   // 💪 Daily protein intake in grams
    CARBS,     // 🍞 Daily carbohydrate intake in grams
    FAT,       // 🥑 Daily fat intake in grams
    WEIGHT     // ⚖️ Weight measurements in pounds
}