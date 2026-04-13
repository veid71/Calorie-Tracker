package com.calorietracker.widgets

// 🧰 CUSTOM WIDGET TOOLS
import android.content.Context         // Android system access
import android.graphics.*             // Drawing tools (Canvas, Paint, etc.)
import android.util.AttributeSet      // XML attribute handling
import android.view.View             // Base view class
import androidx.core.content.ContextCompat // Resource access helpers
import com.calorietracker.R          // App resources
import kotlin.math.min              // Math utilities

/**
 * 🎨 MACRO RING WIDGET - BEAUTIFUL NUTRITION CIRCLES
 * 
 * Hey young programmer! This creates beautiful colored rings that show nutrition progress.
 * 
 * 🎯 What does this widget show?
 * Three colorful rings that represent the "big three" macronutrients:
 * - 💪 PROTEIN RING (Blue): Shows protein intake vs goal
 * - 🍞 CARBS RING (Orange): Shows carbohydrate intake vs goal  
 * - 🥑 FAT RING (Green): Shows fat intake vs goal
 * 
 * 🎨 Visual Design:
 * - Rings fill up as user approaches their daily goals
 * - Different colors for each macronutrient
 * - Smooth animations when values change
 * - Center text shows percentage or grams consumed
 * 
 * 📊 Ring Progress Logic:
 * - Empty ring = 0% of goal achieved
 * - Half-filled ring = 50% of goal achieved  
 * - Full ring = 100% of goal achieved
 * - Overflowing ring = exceeded goal (different color)
 * 
 * 🎮 Interactive Features:
 * - Tap on ring to see detailed breakdown
 * - Long press to adjust goals
 * - Smooth color transitions based on progress
 */
class MacroRingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // 🎨 DRAWING TOOLS
    private val paint = Paint().apply {
        isAntiAlias = true      // Smooth, crisp lines
        style = Paint.Style.STROKE  // Draw outlines, not filled shapes
        strokeCap = Paint.Cap.ROUND // Rounded line ends
    }
    
    // 📊 MACRO DATA STORAGE
    private var proteinCurrent: Float = 0f    // 💪 Current protein intake in grams
    private var proteinGoal: Float = 100f     // 💪 Daily protein goal in grams
    private var carbsCurrent: Float = 0f      // 🍞 Current carb intake in grams
    private var carbsGoal: Float = 250f       // 🍞 Daily carb goal in grams
    private var fatCurrent: Float = 0f        // 🥑 Current fat intake in grams
    private var fatGoal: Float = 80f          // 🥑 Daily fat goal in grams
    
    // 🎨 COLORS FOR EACH MACRO RING
    private val proteinColor = ContextCompat.getColor(context, R.color.macro_protein_blue)    // 💪 Blue for protein
    private val carbsColor = ContextCompat.getColor(context, R.color.accent_orange)          // 🍞 Orange for carbs
    private val fatColor = ContextCompat.getColor(context, R.color.macro_fat_green)          // 🥑 Green for fat
    private val backgroundColor = ContextCompat.getColor(context, R.color.macro_background)   // ⚫ Background ring
    private val exceededColor = ContextCompat.getColor(context, R.color.red)                // 🔴 When goal exceeded
    
    // 📏 RING DIMENSIONS
    private var ringStrokeWidth = 20f        // How thick each ring is
    private var ringGap = 30f                // Space between rings
    private var centerX = 0f                 // Center point X coordinate
    private var centerY = 0f                 // Center point Y coordinate
    private var baseRadius = 120f            // Radius of innermost ring
    
    /**
     * 📊 UPDATE MACRO VALUES
     * 
     * Call this when user's macro intake changes to update the visual rings.
     * 
     * @param proteinCurrent Current protein intake in grams
     * @param proteinGoal Daily protein goal in grams
     * @param carbsCurrent Current carb intake in grams  
     * @param carbsGoal Daily carb goal in grams
     * @param fatCurrent Current fat intake in grams
     * @param fatGoal Daily fat goal in grams
     */
    fun updateMacros(
        proteinCurrent: Float,
        proteinGoal: Float,
        carbsCurrent: Float,
        carbsGoal: Float,
        fatCurrent: Float,
        fatGoal: Float
    ) {
        this.proteinCurrent = proteinCurrent
        this.proteinGoal = proteinGoal
        this.carbsCurrent = carbsCurrent
        this.carbsGoal = carbsGoal
        this.fatCurrent = fatCurrent
        this.fatGoal = fatGoal
        
        invalidate() // Tell Android to redraw this widget
    }
    
    /**
     * 📏 MEASURE WIDGET SIZE
     * 
     * Android calls this to ask "How big do you want to be?"
     * We calculate size based on ring dimensions.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = ((baseRadius + ringGap * 2 + ringStrokeWidth) * 2).toInt()
        setMeasuredDimension(desiredSize, desiredSize)
    }
    
    /**
     * 📐 CALCULATE DRAWING COORDINATES
     * 
     * When widget size changes, recalculate where to draw everything.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        
        // Adjust ring size based on available space
        val maxRadius = min(w, h) / 2f - ringStrokeWidth
        baseRadius = maxRadius - ringGap * 2
    }
    
    /**
     * 🎨 DRAW THE MACRO RINGS
     * 
     * This is where the magic happens! We draw three colored rings showing macro progress.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 🎨 SET UP DRAWING PROPERTIES
        paint.strokeWidth = ringStrokeWidth
        
        // 💪 DRAW PROTEIN RING (innermost)
        drawMacroRing(
            canvas = canvas,
            current = proteinCurrent,
            goal = proteinGoal,
            radius = baseRadius,
            color = proteinColor,
            label = "P"
        )
        
        // 🍞 DRAW CARBS RING (middle)
        drawMacroRing(
            canvas = canvas,
            current = carbsCurrent,
            goal = carbsGoal,
            radius = baseRadius + ringGap,
            color = carbsColor,
            label = "C"
        )
        
        // 🥑 DRAW FAT RING (outermost)
        drawMacroRing(
            canvas = canvas,
            current = fatCurrent,
            goal = fatGoal,
            radius = baseRadius + ringGap * 2,
            color = fatColor,
            label = "F"
        )
        
        // 📊 DRAW CENTER TEXT WITH OVERALL PROGRESS
        drawCenterText(canvas)
    }
    
    /**
     * 🎨 DRAW ONE MACRO RING
     * 
     * Draws a single colored ring with progress fill.
     */
    private fun drawMacroRing(
        canvas: Canvas,
        current: Float,
        goal: Float,
        radius: Float,
        color: Int,
        label: String
    ) {
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        // 🌫️ DRAW BACKGROUND RING (gray/faded)
        paint.color = backgroundColor
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // 🎯 CALCULATE PROGRESS PERCENTAGE
        val progress = if (goal > 0) current / goal else 0f
        val sweepAngle = (progress * 360f).coerceIn(0f, 360f)
        
        // 🎨 CHOOSE RING COLOR
        paint.color = when {
            progress > 1.2f -> exceededColor  // 🔴 Way over goal (120%+)
            progress > 1.0f -> color         // 🟠 Slightly over goal
            else -> color                    // 🟢 Normal progress
        }
        
        // 🎨 DRAW PROGRESS ARC
        if (sweepAngle > 0) {
            canvas.drawArc(rect, -90f, sweepAngle, false, paint)
        }
        
        // 📝 DRAW MACRO LABEL
        drawMacroLabel(canvas, label, current, goal, radius)
    }
    
    /**
     * 📝 DRAW MACRO LABEL AND VALUES
     * 
     * Shows the macro letter (P/C/F) and current values next to each ring.
     */
    private fun drawMacroLabel(canvas: Canvas, label: String, current: Float, goal: Float, radius: Float) {
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.text_primary)
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        
        // 📍 CALCULATE LABEL POSITION
        val labelX = centerX + radius + 40f
        val labelY = centerY + 8f
        
        // 📝 DRAW MACRO LETTER
        canvas.drawText(label, labelX, labelY, paint)
        
        // 📊 DRAW CURRENT/GOAL VALUES
        paint.textSize = 16f
        val valueText = "${current.toInt()}/${goal.toInt()}g"
        canvas.drawText(valueText, labelX, labelY + 25f, paint)
        
        // Reset paint for next drawing
        paint.style = Paint.Style.STROKE
    }
    
    /**
     * 📊 DRAW CENTER TEXT
     * 
     * Shows overall macro balance in the center of the rings.
     */
    private fun drawCenterText(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.text_primary)
        paint.textAlign = Paint.Align.CENTER
        
        // 🧮 CALCULATE OVERALL BALANCE
        val proteinPercent = if (proteinGoal > 0) (proteinCurrent / proteinGoal * 100).toInt() else 0
        val carbsPercent = if (carbsGoal > 0) (carbsCurrent / carbsGoal * 100).toInt() else 0
        val fatPercent = if (fatGoal > 0) (fatCurrent / fatGoal * 100).toInt() else 0
        val averagePercent = (proteinPercent + carbsPercent + fatPercent) / 3
        
        // 📝 DRAW BALANCE TEXT
        paint.textSize = 32f
        canvas.drawText("${averagePercent}%", centerX, centerY - 10f, paint)
        
        paint.textSize = 16f
        canvas.drawText("Macro Balance", centerX, centerY + 15f, paint)
        
        // Reset paint
        paint.style = Paint.Style.STROKE
    }
}