package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPlannerScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val activeDeg by viewModel.activeDegree.collectAsStateWithLifecycle()
    val completedCredits = viewModel.getCompletedCredits()
    val currentCgpa = viewModel.getCurrentCGPA()
    val scale = viewModel.getActiveDegreeScale()

    val totalNeeded = activeDeg?.totalCreditsNeeded ?: 120.0
    val targetGpa = activeDeg?.targetGpa ?: 3.5
    val maxScalePoints = scale?.maxPoints ?: 4.0

    // Manage slider states locally for responsive, zero-lag rendering
    var localTargetGpa by remember(targetGpa) { mutableStateOf(targetGpa.toFloat()) }
    var localTotalNeeded by remember(totalNeeded) { mutableStateOf(totalNeeded.toFloat()) }

    val localTargetGpaDouble = localTargetGpa.toDouble()
    val localTotalNeededDouble = localTotalNeeded.toDouble()

    val remainingCredits = (localTotalNeededDouble - completedCredits).coerceAtLeast(0.0)

    val remainingSemesters by viewModel.remainingSemestersInput.collectAsStateWithLifecycle()

    // Calculate planning values using local slider state
    val currentPoints = currentCgpa * completedCredits
    val totalRequiredPointsForTarget = localTargetGpaDouble * localTotalNeededDouble
    val remainingPointsNeeded = (totalRequiredPointsForTarget - currentPoints).coerceAtLeast(0.0)

    val requiredFutureAverage = if (remainingCredits > 0.0) {
        remainingPointsNeeded / remainingCredits
    } else 0.0

    // Feasibility status evaluation using local slider state
    val feasibility = remember(localTargetGpaDouble, localTotalNeededDouble, completedCredits, currentCgpa, maxScalePoints) {
        if (completedCredits >= localTotalNeededDouble) {
            if (currentCgpa >= localTargetGpaDouble) "Goal Reached!" else "Completed (Missed Target)"
        } else {
            val remaining = localTotalNeededDouble - completedCredits
            if (remaining <= 0.0) {
                "Goal Reached!"
            } else {
                val requiredPoints = (localTargetGpaDouble * localTotalNeededDouble) - (currentCgpa * completedCredits)
                val requiredAvg = requiredPoints / remaining
                if (requiredAvg <= 0.0) "Very Realistic"
                else if (requiredAvg > maxScalePoints) "Impossible"
                else {
                    val pct = requiredAvg / maxScalePoints
                    when {
                        pct < 0.5 -> "Very Realistic"
                        pct < 0.8 -> "Realistic / Achievable"
                        pct < 0.92 -> "Challenging"
                        else -> "Very Difficult"
                    }
                }
            }
        }
    }
    val (statusColor, description) = when (feasibility) {
        "Very Realistic" -> Pair(
            Color(0xFF2E7D32), // Deep Green
            "Highly achievable! Keep performing at your current level and you will sail through."
        )
        "Realistic / Achievable" -> Pair(
            Color(0xFF4CAF50), // Green
            "Very achievable. Requires consistent efforts and staying close to your current average."
        )
        "Challenging" -> Pair(
            Color(0xFFE65100), // Dark Orange
            "Challenging. You need a significant upgrade in your upcoming quarters to lift your CGPA."
        )
        "Very Difficult" -> Pair(
            Color(0xFFC62828), // Deep Red
            "Extremely difficult. Requires nearly perfect scores (Straight A's) in every remaining course."
        )
        "Impossible" -> Pair(
            Color(0xFF37474F), // Slate Grey
            "Mathematically impossible within the current credit structure. You may need to increase your degree credit limits or revise your target GPA."
        )
        "Goal Reached!" -> Pair(
            Color(0xFF1565C0), // Nice Blue
            "Congratulations! You've already completed the graduation credit threshold and achieved your Target GPA!"
        )
        else -> Pair(
            MaterialTheme.colorScheme.primary,
            "Enter academic semesters & grades to calculate your roadmap."
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("target_planner_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Target GPA Roadmap Planner",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        )

        // Planner Configuration Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Goal Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Target Final GPA Slider / Editor
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target GPA",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = String.format("%.2f", localTargetGpa),
                            style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = localTargetGpa,
                        onValueChange = { newVal ->
                            localTargetGpa = newVal
                        },
                        onValueChangeFinished = {
                            activeDeg?.let {
                                viewModel.updateDegreeInfo(it.copy(targetGpa = Math.round(localTargetGpa * 100.0) / 100.0))
                            }
                        },
                        valueRange = 0f..maxScalePoints.toFloat(),
                        steps = (maxScalePoints * 20).toInt(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Graduation Credits Needed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Graduation Credits Required",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${localTotalNeeded.toInt()} Credits",
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = localTotalNeeded,
                        onValueChange = { newVal ->
                            localTotalNeeded = newVal
                        },
                        onValueChangeFinished = {
                            activeDeg?.let {
                                viewModel.updateDegreeInfo(it.copy(totalCreditsNeeded = Math.round(localTotalNeeded.toDouble()).toDouble()))
                            }
                        },
                        valueRange = 30f..240f,
                        steps = 210,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Remaining Semesters Tracker
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Semesters Remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$remainingSemesters Semesters",
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = remainingSemesters.toFloat(),
                        onValueChange = { viewModel.updateRemainingSemesters(it.toInt()) },
                        valueRange = 1f..12f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Output Status Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, statusColor.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FEASIBILITY / STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )

                Text(
                    text = feasibility.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = statusColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Required GPA Equation Metrics Card
        if (remainingCredits > 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Calculated Target Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Average GPA needed in remaining credits
                    Column {
                        Text(
                            text = "Required Average Future GPA (Y Remaining Credits)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (requiredFutureAverage > maxScalePoints) {
                                "${String.format("%.2f", requiredFutureAverage)} (EXCEEDS SCALE MAX: ${String.format("%.1f", maxScalePoints)})"
                            } else {
                                String.format("%.2f", requiredFutureAverage)
                            },
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (requiredFutureAverage > maxScalePoints) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "To graduate with $targetGpa target CGPA, you must earn an average of ${String.format("%.2f", requiredFutureAverage)} GPA points across your remaining ${String.format("%.1f", remainingCredits)} credits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    // Average required per semester
                    val requiredCreditsPerSemester = remainingCredits / remainingSemesters
                    val totalPointsNeededPerSemester = (requiredFutureAverage * remainingCredits) / remainingSemesters
                    val requiredGpaPerSemester = if (requiredCreditsPerSemester > 0.0) {
                        totalPointsNeededPerSemester / requiredCreditsPerSemester
                    } else 0.0

                    Column {
                        Text(
                            text = "Estimated Target GPA Per Remaining Semester",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = String.format("%.2f SGPA", requiredGpaPerSemester),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Assuming you take an average load of ${String.format("%.1f", requiredCreditsPerSemester)} credits per semester, you need a steady SGPA of ${String.format("%.2f", requiredGpaPerSemester)} in each of the next $remainingSemesters semesters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = "You've already reached your targeted graduation credits. Adjust graduation settings above to plan for higher semesters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
