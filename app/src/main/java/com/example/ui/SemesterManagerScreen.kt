package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Course
import com.example.data.Semester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterManagerScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val activeScaleEntries = viewModel.getActiveScaleEntries()

    var searchQuery by remember { mutableStateOf("") }
    val searchedCourses by viewModel.searchedCourses.collectAsStateWithLifecycle()

    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var showAddCourseDialogForSemesterId by remember { mutableStateOf<Int?>(null) }
    var expandedSemesterIds by remember { mutableStateOf(setOf<Int>()) }

    // Dialog state holders
    var newSemName by remember { mutableStateOf("") }
    var newSemGoalGpa by remember { mutableStateOf("") }

    var newCourseCode by remember { mutableStateOf("") }
    var newCourseName by remember { mutableStateOf("") }
    var newCourseCredits by remember { mutableStateOf("") }
    var newCourseGradeIndex by remember { mutableStateOf(0) }
    var newCourseNotes by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("semester_manager_screen")
    ) {
        // Search bar & Add Semester FAB Top Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("Search Courses...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = { showAddSemesterDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Semester")
            }
        }

        // Search Results Mode vs Active Tree View Mode
        if (searchQuery.isNotBlank()) {
            Text(
                text = "Search Results matching \"$searchQuery\"",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (searchedCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching courses found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchedCourses) { course ->
                        CourseRow(
                            course = course,
                            onEdit = { viewModel.updateCourseInfo(it) },
                            onDelete = { viewModel.deleteCourse(it) },
                            activeScaleEntries = activeScaleEntries
                        )
                    }
                }
            }
        } else {
            // Standard expandable Tree View of Semesters & Courses
            if (semesters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "No Semester",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Add a Semester to Begin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddSemesterDialog = true }) {
                            Text("Create Semester")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    val completedSems = semesters.filter { !it.isHypothetical }
                    items(completedSems) { semester ->
                        val semCourses = courses.filter { it.semesterId == semester.id }
                        val sgpa = viewModel.calculateGPAForCourses(semCourses)
                        val totalCredits = semCourses.sumOf { it.credits }
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
                                // Semester Header Row (Clickable)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = semester.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${semCourses.size} Courses | ${String.format("%.1f", totalCredits)} Credits | SGPA: ${String.format("%.2f", sgpa)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = {
                                            viewModel.deleteSemester(semester)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Semester",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand courses list"
                                        )
                                    }
                                }

                                // Expanded Course Details
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        if (semCourses.isEmpty()) {
                                            Text(
                                                text = "No courses added yet. Touch '+' below to add your courses and letters.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        } else {
                                            semCourses.forEach { course ->
                                                CourseRow(
                                                    course = course,
                                                    onEdit = { viewModel.updateCourseInfo(it) },
                                                    onDelete = { viewModel.deleteCourse(it) },
                                                    activeScaleEntries = activeScaleEntries
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }

                                        // Add Course Quick Button inside Expanded Drawer
                                        Button(
                                            onClick = {
                                                showAddCourseDialogForSemesterId = semester.id
                                                newCourseCode = ""
                                                newCourseName = ""
                                                newCourseCredits = "3.0"
                                                newCourseGradeIndex = 0
                                                newCourseNotes = ""
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Course")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Semester Dialog
        if (showAddSemesterDialog) {
            AlertDialog(
                onDismissRequest = { showAddSemesterDialog = false },
                title = { Text("Add Semester") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newSemName,
                            onValueChange = { newSemName = it },
                            label = { Text("Semester Name (e.g., Fall 2026)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newSemGoalGpa,
                            onValueChange = { newSemGoalGpa = it },
                            label = { Text("Goal SGPA (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSemName.isNotBlank()) {
                                val goal = newSemGoalGpa.toDoubleOrNull()
                                viewModel.addSemester(newSemName, goal)
                                newSemName = ""
                                newSemGoalGpa = ""
                                showAddSemesterDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSemesterDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Course Dialog
        showAddCourseDialogForSemesterId?.let { semesterId ->
            var expandedDropdown by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddCourseDialogForSemesterId = null },
                title = { Text("Add New Course") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newCourseCode,
                            onValueChange = { newCourseCode = it },
                            label = { Text("Course Code (e.g., COMP101)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCourseName,
                            onValueChange = { newCourseName = it },
                            label = { Text("Course Name (e.g., Intro to Algorithms)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCourseCredits,
                            onValueChange = { newCourseCredits = it },
                            label = { Text("Credits / Hours (e.g., 3.0)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Custom Dropdown Menu for Letter Grade Selection
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedText = if (activeScaleEntries.isNotEmpty() && newCourseGradeIndex in activeScaleEntries.indices) {
                                "${activeScaleEntries[newCourseGradeIndex].letter} (${activeScaleEntries[newCourseGradeIndex].points} pts)"
                            } else {
                                "Pick Grade Letter"
                            }

                            OutlinedTextField(
                                value = selectedText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Grade Letter") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select grade letter",
                                        modifier = Modifier.clickable { expandedDropdown = !expandedDropdown }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDropdown = !expandedDropdown }
                            )

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                activeScaleEntries.forEachIndexed { idx, entry ->
                                    DropdownMenuItem(
                                        text = { Text("${entry.letter}  —  ${entry.points} GPA Points") },
                                        onClick = {
                                            newCourseGradeIndex = idx
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newCourseNotes,
                            onValueChange = { newCourseNotes = it },
                            label = { Text("Notes / Info (Optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val creditsVal = newCourseCredits.toDoubleOrNull() ?: 3.0
                            if (newCourseCode.isNotBlank() && activeScaleEntries.isNotEmpty()) {
                                val targetScaleEntry = activeScaleEntries[newCourseGradeIndex]
                                viewModel.addCourse(
                                    semesterId = semesterId,
                                    code = newCourseCode.uppercase(),
                                    name = newCourseName,
                                    credits = creditsVal,
                                    grade = targetScaleEntry.letter,
                                    points = targetScaleEntry.points,
                                    notes = newCourseNotes
                                )
                                showAddCourseDialogForSemesterId = null
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCourseDialogForSemesterId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CourseRow(
    course: Course,
    onEdit: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    activeScaleEntries: List<com.example.data.GradeEntry>
) {
    var showDialogEditCourse by remember { mutableStateOf(false) }

    var codeInput by remember { mutableStateOf(course.code) }
    var nameInput by remember { mutableStateOf(course.name) }
    var creditsInput by remember { mutableStateOf(course.credits.toString()) }
    var selectGradeIndex by remember { 
        mutableStateOf(activeScaleEntries.indexOfFirst { it.letter == course.grade }.coerceAtLeast(0)) 
    }
    var notesInput by remember { mutableStateOf(course.notes) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_row_${course.code}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = course.grade,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${course.code}: ${course.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", course.credits)} Credits | Earned Points: ${course.points}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { showDialogEditCourse = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Course", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onDelete(course) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Course", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (course.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = course.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDialogEditCourse) {
        var dropdownExpandedEdit by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialogEditCourse = false },
            title = { Text("Edit Course Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("Course Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Course Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = creditsInput,
                        onValueChange = { creditsInput = it },
                        label = { Text("Credits") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Grade dropdown list
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentText = if (activeScaleEntries.isNotEmpty() && selectGradeIndex in activeScaleEntries.indices) {
                            "${activeScaleEntries[selectGradeIndex].letter} (${activeScaleEntries[selectGradeIndex].points} pts)"
                        } else {
                            "Select Grade"
                        }

                        OutlinedTextField(
                            value = currentText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Grades",
                                    modifier = Modifier.clickable { dropdownExpandedEdit = !dropdownExpandedEdit }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpandedEdit = !dropdownExpandedEdit }
                        )

                        DropdownMenu(
                            expanded = dropdownExpandedEdit,
                            onDismissRequest = { dropdownExpandedEdit = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            activeScaleEntries.forEachIndexed { i, entry ->
                                DropdownMenuItem(
                                    text = { Text("${entry.letter}  —  ${entry.points} XP") },
                                    onClick = {
                                        selectGradeIndex = i
                                        dropdownExpandedEdit = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes / Comments") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val creds = creditsInput.toDoubleOrNull() ?: course.credits
                        if (codeInput.isNotBlank() && activeScaleEntries.isNotEmpty() && selectGradeIndex in activeScaleEntries.indices) {
                            val selectedEntry = activeScaleEntries[selectGradeIndex]
                            onEdit(
                                course.copy(
                                    code = codeInput.uppercase(),
                                    name = nameInput,
                                    credits = creds,
                                    grade = selectedEntry.letter,
                                    points = selectedEntry.points,
                                    notes = notesInput
                                )
                            )
                            showDialogEditCourse = false
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogEditCourse = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
