package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Course
import com.example.data.Semester

@Composable
fun WhatIfSimulatorScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val activeScaleEntries = viewModel.getActiveScaleEntries()

    val officialCgpa = viewModel.getCurrentCGPA()
    val officialCredits = viewModel.getCompletedCredits()

    // Filter hypothetical semesters
    val simulatedSemesters = semesters.filter { it.isHypothetical }
    val simulatedCoursesCount = courses.filter { c -> simulatedSemesters.any { s -> s.id == c.semesterId } }.size
    val totalSimulatedCredits = courses.filter { c -> simulatedSemesters.any { s -> s.id == c.semesterId } }.sumOf { it.credits }

    // Computes overall projected CGPA including hypothetical courses
    val projectedCgpa = viewModel.getEstimatedHypotheticalCgpa()

    // Preset testing overrides for dynamic UI sandbox previews
    var sandboxScenarioOverride by remember { mutableStateOf<String?>(null) }
    val overriddenProjectedCgpa = sandboxScenarioOverride?.let {
        viewModel.getwhatIfCgpaAndPreview(it)
    } ?: projectedCgpa

    var showAddHypoSemesterDialog by remember { mutableStateOf(false) }
    var showAddHypoCourseDialogForSemId by remember { mutableStateOf<Int?>(null) }
    var expandedSemesterIds by remember { mutableStateOf(setOf<Int>()) }

    // Temporary inputs
    var semName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var courseName by remember { mutableStateOf("") }
    var courseCredits by remember { mutableStateOf("3.0") }
    var courseGradeIdx by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("what_if_simulator_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Applet Header
        item {
            Column {
                Text(
                    text = "What-If Projection Simulator",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Build hypothetical futures. Simulate upcoming grades & explore cumulative projections without altering official academic records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Comparative Projection Board Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Projected Cumulative Impact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Official CGPA column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Current CGPA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.2f", officialCgpa),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${officialCredits.toInt()} Cr Earned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Simulated/Projected CGPA column
                        val isOverridden = sandboxScenarioOverride != null
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .background(
                                    if (isOverridden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isOverridden) "$sandboxScenarioOverride Preview" else "Projected CGPA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isOverridden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = String.format("%.2f", overriddenProjectedCgpa),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isOverridden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${(officialCredits + totalSimulatedCredits).toInt()} Credits Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (overriddenProjectedCgpa > officialCgpa) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This path will successfully raise your GPA by +${String.format("%.2f", overriddenProjectedCgpa - officialCgpa)}!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }

        // One-Click Presets Sandbox Picker Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "One-Tap Quick Scenarios",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Optimistic", "Realistic", "Minimum").forEach { scenario ->
                        val isSelected = sandboxScenarioOverride == scenario
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                sandboxScenarioOverride = if (isSelected) null else scenario
                            },
                            label = { Text(scenario) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                if (sandboxScenarioOverride != null) {
                    TextButton(
                        onClick = { sandboxScenarioOverride = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset Scenario Overrides", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Simulated Semesters header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hypothetical Semesters Manager",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showAddHypoSemesterDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Planner Semester", fontSize = 12.sp)
                }
            }
        }

        // List of hypothetical Semesters
        if (simulatedSemesters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hypothetical semesters.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add a simulated term (e.g. 'Next Semester') to test hypothetical credits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(simulatedSemesters) { semester ->
                val semCourses = courses.filter { it.semesterId == semester.id }
                val sgpa = viewModel.calculateGPAForCourses(semCourses)
                val totalSemCredits = semCourses.sumOf { it.credits }
                val isExpanded = expandedSemesterIds.contains(semester.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSemesterIds = if (isExpanded) {
                                        expandedSemesterIds - semester.id
                                    } else {
                                        expandedSemesterIds + semester.id
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Vertical Indicator Bar (Amber representing simulated / what-if scenarios)
                                val indicatorColor = Color(0xFFF59E0B)
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(indicatorColor)
                                )

                                Column {
                                    Text(
                                        text = semester.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    )
                                    Text(
                                        text = "${semCourses.size} hypothetical courses | ${totalSemCredits.toInt()} Credits | Simulated SGPA: ${String.format("%.2f", sgpa)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.deleteSemester(semester) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (semCourses.isEmpty()) {
                                    Text(
                                        text = "Add hypothetical courses (e.g. MATH 301, 4 Credits, Grade A) underneath this simulated semester to study credit weights dynamically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    semCourses.forEach { course ->
                                        // Simple card displaying hypothetical course
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${course.code}: ${course.name}",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = "${String.format("%.1f", course.credits)} Credits | Target: Grade ${course.grade} (${course.points} pts)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(onClick = { viewModel.deleteCourse(course) }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        showAddHypoCourseDialogForSemId = semester.id
                                        courseCode = ""
                                        courseName = ""
                                        courseCredits = "3.0"
                                        courseGradeIdx = 0
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Hypothetical Course")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddHypoSemesterDialog) {
        AlertDialog(
            onDismissRequest = { showAddHypoSemesterDialog = false },
            title = { Text("New Planner Sandbox Semester") },
            text = {
                OutlinedTextField(
                    value = semName,
                    onValueChange = { semName = it },
                    label = { Text("Sandbox Semester Name (e.g., Year 4 Sem 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (semName.isNotBlank()) {
                            viewModel.addSemester(semName, null, isHypothetical = true)
                            semName = ""
                            showAddHypoSemesterDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHypoSemesterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showAddHypoCourseDialogForSemId?.let { semesterId ->
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddHypoCourseDialogForSemId = null },
            title = { Text("Add Hypothetical Course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code (e.g. PHYS202)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Course Name (e.g. Physics)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = courseCredits,
                        onValueChange = { courseCredits = it },
                        label = { Text("Credit Hours (e.g. 4.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown for target grades
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedText = if (activeScaleEntries.isNotEmpty() && courseGradeIdx in activeScaleEntries.indices) {
                            "${activeScaleEntries[courseGradeIdx].letter} (${activeScaleEntries[courseGradeIdx].points} pts)"
                        } else {
                            "Pick Targeted Grade"
                        }

                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Grade Letter") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { dropdownExpanded = !dropdownExpanded }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = !dropdownExpanded }
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            activeScaleEntries.forEachIndexed { idx, entry ->
                                DropdownMenuItem(
                                    text = { Text("${entry.letter}  —  ${entry.points} pts") },
                                    onClick = {
                                        courseGradeIdx = idx
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val creditsVal = courseCredits.toDoubleOrNull() ?: 3.0
                        if (courseCode.isNotBlank() && activeScaleEntries.isNotEmpty() && courseGradeIdx in activeScaleEntries.indices) {
                            val targetGrade = activeScaleEntries[courseGradeIdx]
                            viewModel.addCourse(
                                semesterId = semesterId,
                                code = courseCode.uppercase(),
                                name = courseName,
                                credits = creditsVal,
                                grade = targetGrade.letter,
                                points = targetGrade.points,
                                isCompleted = false, // mark as hypothetical/sandbox course!
                                notes = "Simulated / Planned Course"
                            )
                            showAddHypoCourseDialogForSemId = null
                        }
                    }
                ) {
                    Text("Add Simulated")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHypoCourseDialogForSemId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
