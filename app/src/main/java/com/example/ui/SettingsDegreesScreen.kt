package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Degree
import com.example.data.GradingScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsDegreesScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val activeDeg by viewModel.activeDegree.collectAsStateWithLifecycle()
    val allDegs by viewModel.allDegrees.collectAsStateWithLifecycle()
    val allScales by viewModel.allGradingScales.collectAsStateWithLifecycle()
    val scaleEntries by viewModel.gradeEntries.collectAsStateWithLifecycle()

    var showAddDegreeDialog by remember { mutableStateOf(false) }
    var showAddCustomScaleDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var showBackupPanel by remember { mutableStateOf(false) }

    // Add degree state
    var degName by remember { mutableStateOf("") }
    var degTargetGpa by remember { mutableStateOf("3.5") }
    var degTotalCredits by remember { mutableStateOf("120.0") }
    var selectedScaleIdx by remember { mutableStateOf(0) }

    // Add scale state
    var scaleName by remember { mutableStateOf("") }
    var scaleMaxPoints by remember { mutableStateOf("4.0") }
    var scaleGradesInput by remember { mutableStateOf("A+:4.0, A:4.0, B:3.0, C:2.0, D:1.0, F:0.0") }

    // Import state
    var jsonImportText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_degrees_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core header
        Column {
            Text(
                text = "Global App Setup & Degrees",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "Add dual programs, design modular scores, back up your student profile, or extract academic print PDFs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Multiple Degree Programs Manager Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Degree Programs / Profile Paths",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showAddDegreeDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Program", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                allDegs.forEach { degree ->
                    val isActive = degree.id == activeDeg?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.switchActiveDegree(degree.id) }
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(
                                selected = isActive,
                                onClick = { viewModel.switchActiveDegree(degree.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = degree.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
                                )
                                Text(
                                    text = "Target GPA: ${String.format("%.2f", degree.targetGpa)} | Graduation Requirement: ${degree.totalCreditsNeeded.toInt()} Cr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (allDegs.size > 1) {
                            IconButton(onClick = { viewModel.deleteDegree(degree) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Program", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 2. Custom Grading Scales Manager Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grading Scales & Scores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showAddCustomScaleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom scale", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                allScales.forEach { scale ->
                    val isActiveScale = scale.id == activeDeg?.activeScaleId
                    var showEntriesList by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActiveScale) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEntriesList = !showEntriesList }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isActiveScale) Icons.Default.Verified else Icons.Default.Grade,
                                    contentDescription = null,
                                    tint = if (isActiveScale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${scale.name} (Max: ${scale.maxPoints})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (scale.isSystemDefault) {
                                        Text(
                                            text = "System Default Scale",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activeDeg?.activeScaleId != scale.id) {
                                    IconButton(
                                        onClick = {
                                            activeDeg?.let {
                                                viewModel.updateDegreeInfo(it.copy(activeScaleId = scale.id))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Switch to scale", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (!scale.isSystemDefault && allScales.size > 1) {
                                    IconButton(onClick = { viewModel.deleteCustomGradingScale(scale.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove scale", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Icon(
                                    imageVector = if (showEntriesList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showEntriesList) {
                            val entries = scaleEntries[scale.id] ?: emptyList()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, end = 16.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Letter mappings:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    entries.forEach { entry ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${entry.letter}: ${entry.points}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        // 3. Academic PDF Printing & Backup Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Backup, Import & Academic Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // PDF printing offline
                Button(
                    onClick = {
                        val file = viewModel.exportToPdf(context)
                        if (file != null) {
                            Toast.makeText(context, "Academic record PDF saved to Documents!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to compile offline PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Official Progress PDF Report")
                }

                // JSON Backup Drawer trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showBackupPanel = !showBackupPanel },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Backup Text")
                    }

                    Button(
                        onClick = { showImportJsonDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }

                AnimatedVisibility(visible = showBackupPanel) {
                    val backupText = viewModel.exportDataToJson(context) ?: "No data to export"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Copy the code below to save your backup externally completely offline:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = backupText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(backupText))
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code")
                        }
                    }
                }
            }
        }
    }

    // Modal dialog overlays
    if (showAddDegreeDialog) {
        val gradingScalesNames = allScales.map { it.name }

        AlertDialog(
            onDismissRequest = { showAddDegreeDialog = false },
            title = { Text("Add Program / Degree Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = degName,
                        onValueChange = { degName = it },
                        label = { Text("Program Name (e.g. Master of Science)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = degTargetGpa,
                        onValueChange = { degTargetGpa = it },
                        label = { Text("Target final GPA (e.g. 3.8)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = degTotalCredits,
                        onValueChange = { degTotalCredits = it },
                        label = { Text("Total Credits for Completion") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Grading scale picker
                    var dropdownOpen by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentLabel = if (allScales.isNotEmpty() && selectedScaleIdx in allScales.indices) {
                            allScales[selectedScaleIdx].name
                        } else {
                            "Choose Grading Scale"
                        }

                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Academic Grading System") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { dropdownOpen = !dropdownOpen })
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownOpen = !dropdownOpen }
                        )

                        DropdownMenu(
                            expanded = dropdownOpen,
                            onDismissRequest = { dropdownOpen = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            allScales.forEachIndexed { index, scale ->
                                DropdownMenuItem(
                                    text = { Text(scale.name) },
                                    onClick = {
                                        selectedScaleIdx = index
                                        dropdownOpen = false
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
                        val target = degTargetGpa.toDoubleOrNull() ?: 3.5
                        val total = degTotalCredits.toDoubleOrNull() ?: 120.0
                        if (degName.isNotBlank() && allScales.isNotEmpty() && selectedScaleIdx in allScales.indices) {
                            viewModel.addDegree(
                                name = degName,
                                targetGpa = target,
                                totalCredits = total,
                                activeScaleId = allScales[selectedScaleIdx].id
                            )
                            degName = ""
                            degTargetGpa = "3.5"
                            degTotalCredits = "120.0"
                            selectedScaleIdx = 0
                            showAddDegreeDialog = false
                        }
                    }
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDegreeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCustomScaleDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomScaleDialog = false },
            title = { Text("Create Custom Grading Scale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = scaleName,
                        onValueChange = { scaleName = it },
                        label = { Text("Scale Name (e.g. 5.0 System, ECTS Scale)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scaleMaxPoints,
                        onValueChange = { scaleMaxPoints = it },
                        label = { Text("Maximum Points Value (e.g., 5.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scaleGradesInput,
                        onValueChange = { scaleGradesInput = it },
                        label = { Text("Letter mappings (comma separated list)") },
                        placeholder = { Text("E.g. A+:4.0, A:4.0, B:3.0, F:0.0") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Format letters & scores exactly as: LETTER:POINTS, separated by commas.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val maxPointsVal = scaleMaxPoints.toDoubleOrNull() ?: 4.0
                        if (scaleName.isNotBlank() && scaleGradesInput.isNotBlank()) {
                            try {
                                val entries = scaleGradesInput.split(",")
                                    .map { word ->
                                        val parts = word.split(":")
                                        val letterStr = parts[0].trim()
                                        val ptsVal = parts[1].trim().toDouble()
                                        Pair(letterStr, ptsVal)
                                    }
                                viewModel.addGradingScaleWithEntries(scaleName, maxPointsVal, entries)
                                scaleName = ""
                                scaleMaxPoints = "4.0"
                                scaleGradesInput = "A+:4.0, A:4.0, B:3.0, C:2.0, D:1.0, F:0.0"
                                showAddCustomScaleDialog = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error parsing letter grade scores. Please double check letters format.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save Scale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomScaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Restore GPA Database Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste your raw exported JSON backup text down below. This will create a local degree profile containing your custom data offline.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = { jsonImportText = it },
                        label = { Text("Paste JSON Backup Text") },
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonImportText.isNotBlank()) {
                            val success = viewModel.importDataFromJson(context, jsonImportText)
                            if (success) {
                                Toast.makeText(context, "Database profile successfully restored offline!", Toast.LENGTH_SHORT).show()
                                showImportJsonDialog = false
                                jsonImportText = ""
                            } else {
                                Toast.makeText(context, "Import failed. Invalid JSON model structure.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Import Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
