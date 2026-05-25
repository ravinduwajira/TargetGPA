package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.example.data.Course
import com.example.data.Semester

@Composable
fun GpaLineChart(
    semesters: List<Semester>,
    allCourses: List<Course>,
    calculateGpa: (List<Course>) -> Double,
    maxGpa: Double,
    modifier: Modifier = Modifier
) {
    if (semesters.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No semester data available for trend chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    // Calculate SGPAs & CGPAs sequentially
    val dataPoints = mutableListOf<Double>()
    val semesterNames = mutableListOf<String>()
    
    val completedSems = semesters.filter { !it.isHypothetical }.sortedBy { it.id }
    
    val cumulativeCourses = mutableListOf<Course>()
    completedSems.forEach { sem ->
        val semCourses = allCourses.filter { it.semesterId == sem.id }
        cumulativeCourses.addAll(semCourses)
        val cgpa = calculateGpa(cumulativeCourses)
        dataPoints.add(cgpa)
        semesterNames.add(sem.name)
    }

    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No completed semesters found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = modifier) {
        Text(
            text = "CGPA Growth Trend Across Semesters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 40f
            val paddingBottom = 40f
            val graphWidth = width - paddingLeft
            val graphHeight = height - paddingBottom

            // Draw Y-axis grid lines (0, 1, 2, 3, maxPoints)
            val maxGpaCoerced = maxGpa.coerceAtLeast(0.1)
            val gridStep = maxGpaCoerced / 4.0
            for (i in 0..4) {
                val gpaVal = i * gridStep
                val y = (graphHeight - (gpaVal / maxGpaCoerced * graphHeight)).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", gpaVal),
                    10f,
                    y + 10f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 24f
                    }
                )
            }

            // Map dataPoints to X & Y
            val stepsCount = dataPoints.size
            if (stepsCount == 1) {
                // Just a single point, let's draw a dot in the center
                val x = paddingLeft + graphWidth / 2f
                val y = (graphHeight - ((dataPoints[0] / maxGpaCoerced) * graphHeight)).toFloat()
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
            } else {
                val dx = graphWidth / (stepsCount - 1)
                val path = Path()
                
                for (i in 0 until stepsCount) {
                    val x = paddingLeft + (i * dx)
                    val y = (graphHeight - ((dataPoints[i] / maxGpaCoerced) * graphHeight)).toFloat()
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    // Draw Point dots
                    drawCircle(
                        color = secondaryColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }

                // Draw connecting path line
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 4f)
                )
            }

            // Draw X-axis labels (semester names)
            if (stepsCount > 0) {
                val dx = if (stepsCount > 1) graphWidth / (stepsCount - 1) else graphWidth / 2f
                for (i in 0 until stepsCount) {
                    val x = paddingLeft + (i * dx)
                    val label = semesterNames[i]
                    val truncatedLabel = if (label.length > 8) label.take(6) + ".." else label
                    drawContext.canvas.nativeCanvas.drawText(
                        truncatedLabel,
                        x - 30f,
                        height - 10f,
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 24f
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GradePieChart(
    courses: List<Course>,
    modifier: Modifier = Modifier
) {
    val completedCourses = courses.filter { it.grade.isNotBlank() }
    if (completedCourses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No completed courses found for grade analysis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Group credits by grade
    val gradeCreditsMap = completedCourses.groupBy { it.grade }
        .mapValues { entry -> entry.value.sumOf { it.credits } }

    val totalCredits = gradeCreditsMap.values.sum()
    if (totalCredits <= 0.0) return

    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFFFFEB3B), Color(0xFF795548), Color(0xFF9E9E9E)
    )

    Column(modifier = modifier) {
        Text(
            text = "Grade Distribution (By Completed Credits)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .weight(1f)
            ) {
                var currentAngle = 0f
                gradeCreditsMap.entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / totalCredits) * 360f).toFloat()
                    val color = colors[index % colors.size]

                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    currentAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Legend
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                gradeCreditsMap.entries.toList().take(6).forEachIndexed { index, entry ->
                    val color = colors[index % colors.size]
                    val percentage = (entry.value / totalCredits) * 100.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${entry.key}: ${String.format("%.1f", entry.value)} Cr (${String.format("%.1f", percentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (gradeCreditsMap.size > 6) {
                    Text(
                        text = "+ ${gradeCreditsMap.size - 6} other grade(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreditsBarChart(
    courses: List<Course>,
    modifier: Modifier = Modifier
) {
    val completedCourses = courses.filter { it.grade.isNotBlank() }
    if (completedCourses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No completed courses found for credit analysis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Sum credits per grade level & sort by grade letter alphabetically or standard ranks
    val gradeCreditsMap = completedCourses.groupBy { it.grade }
        .mapValues { entry -> entry.value.sumOf { it.credits } }
        .entries.sortedByDescending { it.value } // Order by credits descending for elegant listing

    val maxCreditsInBar = (gradeCreditsMap.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(0.1)
    val barColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val textOnSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Text(
            text = "Total Credits Earned per Grade Letter",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 40f
            val paddingBottom = 40f
            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom

            val barCount = gradeCreditsMap.size
            if (barCount == 0) return@Canvas

            val spacing = 20f
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (chartWidth - totalSpacing) / barCount

            // Draw background grid lines corresponding to credit increments
            val creditSteps = (maxCreditsInBar / 4.0).coerceAtLeast(1.0)
            for (i in 0..4) {
                val creditVal = i * creditSteps
                val y = (chartHeight - (creditVal.toFloat() / maxCreditsInBar.toFloat() * chartHeight)).toFloat()
                drawLine(
                    color = gridLineColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${creditVal.toInt()} Cr",
                    10f,
                    y + 10f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 24f
                    }
                )
            }

            // Draw individual Bars
            gradeCreditsMap.forEachIndexed { index, entry ->
                val x = paddingLeft + spacing + (index * (barWidth + spacing))
                val barHeightVal = (entry.value / maxCreditsInBar) * chartHeight
                val y = (chartHeight - barHeightVal.toFloat()).toFloat()

                // Draw Bar Rectangle
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeightVal.toFloat())
                )

                // Draw outline for contrast
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeightVal.toFloat()),
                    style = Stroke(width = 2f)
                )

                // Label on top of Bar (credit count)
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", entry.value),
                    x + (barWidth / 2f) - 20f,
                    (y - 10f).coerceAtLeast(30f),
                    android.graphics.Paint().apply {
                        color = textOnSurfaceColor.toArgb()
                        textSize = 22f
                    }
                )

                // Label below Bar (Grade name)
                drawContext.canvas.nativeCanvas.drawText(
                    entry.key,
                    x + (barWidth / 2f) - 15f,
                    height - 10f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 24f
                    }
                )
            }
        }
    }
}
