package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class GpaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GpaRepository

    init {
        val database = GpaDatabase.getDatabase(application)
        repository = GpaRepository(database.gpaDao())
        
        // Seed database and set state
        viewModelScope.launch {
            repository.ensureDatabaseSeeded()
        }
    }

    // --- State Observables ---
    val activeDegree: StateFlow<Degree?> = repository.activeDegreeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDegrees: StateFlow<List<Degree>> = repository.allDegreesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGradingScales: StateFlow<List<GradingScale>> = repository.allGradingScalesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of scaleId to grade entries
    private val _gradeEntries = MutableStateFlow<Map<Int, List<GradeEntry>>>(emptyMap())
    val gradeEntries = _gradeEntries.asStateFlow()

    // Semesters for the active degree
    private val _semesters = MutableStateFlow<List<Semester>>(emptyList())
    val semesters = _semesters.asStateFlow()

    // Courses across all semesters for the active degree
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses = _courses.asStateFlow()

    // Completed courses (for the standard CGPA calculations)
    private val _completedCourses = MutableStateFlow<List<Course>>(emptyList())
    val completedCourses = _completedCourses.asStateFlow()

    // Course search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedCourses: StateFlow<List<Course>> = combine(
        activeDegree,
        _searchQuery
    ) { activeDeg, query ->
        if (activeDeg == null || query.isBlank()) emptyList()
        else repository.searchCoursesFlow(activeDeg.id, query).firstOrNull() ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks remaining semesters input for Planner (default: 4)
    private val _remainingSemestersInput = MutableStateFlow(4)
    val remainingSemestersInput = _remainingSemestersInput.asStateFlow()

    init {
        // Collect active degree changes to update dependent flows
        viewModelScope.launch {
            activeDegree.collectLatest { deg ->
                if (deg != null) {
                    // Update semesters
                    repository.getSemestersForDegreeFlow(deg.id).collectLatest { sems ->
                        _semesters.value = sems
                    }
                } else {
                    _semesters.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            activeDegree.collectLatest { deg ->
                if (deg != null) {
                    // Update courses
                    repository.getAllCoursesIncludingHypotheticalFlow(deg.id).collectLatest { crs ->
                        _courses.value = crs
                    }
                } else {
                    _courses.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            activeDegree.collectLatest { deg ->
                if (deg != null) {
                    // Update completed courses
                    repository.getAllCompletedCoursesForDegreeFlow(deg.id).collectLatest { crs ->
                        _completedCourses.value = crs.filter { it.isCompleted && it.grade.isNotBlank() }
                    }
                } else {
                    _completedCourses.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            allGradingScales.collectLatest { scales ->
                val entriesMap = mutableMapOf<Int, List<GradeEntry>>()
                scales.forEach { scale ->
                    val entries = repository.getGradeEntries(scale.id)
                    entriesMap[scale.id] = entries
                }
                _gradeEntries.value = entriesMap
            }
        }
    }

    // --- Degree Actions ---
    fun addDegree(name: String, targetGpa: Double, totalCredits: Double, activeScaleId: Int) {
        viewModelScope.launch {
            val isFirstPrg = allDegrees.value.isEmpty()
            val degId = repository.insertDegree(
                Degree(
                    name = name,
                    targetGpa = targetGpa,
                    totalCreditsNeeded = totalCredits,
                    isActive = isFirstPrg,
                    activeScaleId = activeScaleId
                )
            ).toInt()
            
            if (isFirstPrg) {
                repository.setActiveDegree(degId)
            }
        }
    }

    fun updateDegreeInfo(degree: Degree) {
        viewModelScope.launch {
            repository.updateDegree(degree)
            // Refresh completed courses
            val actDeg = activeDegree.value
            if (actDeg != null && actDeg.id == degree.id) {
                _completedCourses.value = repository.getAllCompletedCoursesForDegreeFlow(degree.id).firstOrNull()?.filter { it.isCompleted && it.grade.isNotBlank() } ?: emptyList()
            }
        }
    }

    fun switchActiveDegree(degreeId: Int) {
        viewModelScope.launch {
            repository.setActiveDegree(degreeId)
        }
    }

    fun deleteDegree(degree: Degree) {
        viewModelScope.launch {
            repository.deleteDegree(degree)
        }
    }

    // --- Semester Actions ---
    fun addSemester(name: String, goalGpa: Double?, isHypothetical: Boolean = false) {
        val degId = activeDegree.value?.id ?: return
        viewModelScope.launch {
            repository.insertSemester(
                Semester(
                    degreeId = degId,
                    name = name,
                    goalGpa = goalGpa,
                    isHypothetical = isHypothetical
                )
            )
        }
    }

    fun updateSemesterInfo(semester: Semester) {
        viewModelScope.launch {
            repository.updateSemester(semester)
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch {
            repository.deleteSemester(semester)
        }
    }

    // --- Course Actions ---
    fun addCourse(semesterId: Int, code: String, name: String, credits: Double, grade: String, points: Double, notes: String, isCompleted: Boolean = true) {
        viewModelScope.launch {
            repository.insertCourse(
                Course(
                    semesterId = semesterId,
                    code = code,
                    name = name,
                    credits = credits,
                    grade = grade,
                    points = points,
                    isCompleted = isCompleted,
                    notes = notes
                )
            )
        }
    }

    fun updateCourseInfo(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    // --- Grading Scale Actions ---
    fun addGradingScaleWithEntries(name: String, maxPoints: Double, gradeEntries: List<Pair<String, Double>>) {
        viewModelScope.launch {
            val scaleId = repository.insertGradingScale(
                GradingScale(name = name, maxPoints = maxPoints, isSystemDefault = false)
            ).toInt()
            gradeEntries.forEach { (letter, pts) ->
                repository.insertGradeEntry(GradeEntry(scaleId = scaleId, letter = letter, points = pts))
            }
            // Trigger load entries
            val scales = repository.allGradingScalesFlow.first()
            val entriesMap = _gradeEntries.value.toMutableMap()
            scales.forEach { scale ->
                entriesMap[scale.id] = repository.getGradeEntries(scale.id)
            }
            _gradeEntries.value = entriesMap
        }
    }

    fun deleteCustomGradingScale(scaleId: Int) {
        viewModelScope.launch {
            val deg = activeDegree.value
            if (deg != null && deg.activeScaleId == scaleId) {
                // Cannot delete currently in use scale directly without reverting to default (usually scaleId 1)
                val updatedDeg = deg.copy(activeScaleId = 1)
                repository.updateDegree(updatedDeg)
            }
            repository.deleteGradingScaleById(scaleId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateRemainingSemesters(count: Int) {
        _remainingSemestersInput.value = count
    }

    // --- Math Calculations ---

    fun getActiveDegreeScale(): GradingScale? {
        val deg = activeDegree.value ?: return null
        return allGradingScales.value.find { it.id == deg.activeScaleId }
    }

    fun getActiveScaleEntries(): List<GradeEntry> {
        val scaleId = activeDegree.value?.activeScaleId ?: return emptyList()
        return gradeEntries.value[scaleId] ?: emptyList()
    }

    fun calculateGPAForCourses(courseList: List<Course>): Double {
        var weightedSum = 0.0
        var totalCredits = 0.0
        courseList.forEach { course ->
            if (course.grade.isNotBlank()) {
                weightedSum += course.credits * course.points
                totalCredits += course.credits
            }
        }
        return if (totalCredits > 0.0) weightedSum / totalCredits else 0.0
    }

    fun getCompletedCredits(): Double {
        return _completedCourses.value.sumOf { it.credits }
    }

    fun getCurrentCGPA(): Double {
        return calculateGPAForCourses(_completedCourses.value)
    }

    // Trend detection: Compare CGPA of last semester vs entire completed CGPA before last semester
    fun getGpaTrendUpward(): Boolean? {
        val completedSems = semesters.value.filter { !it.isHypothetical }.sortedBy { it.id }
        if (completedSems.size < 2) return null
        
        // Find GPA for all semesters except the last
        val lastSem = completedSems.last()
        val allCompletedCourseList = _completedCourses.value
        
        val lastSemCourses = allCompletedCourseList.filter { it.semesterId == lastSem.id }
        val earlierCourses = allCompletedCourseList.filter { it.semesterId != lastSem.id }
        
        if (earlierCourses.isEmpty() || lastSemCourses.isEmpty()) return null
        
        val earlierCgpa = calculateGPAForCourses(earlierCourses)
        val finalCgpa = calculateGPAForCourses(allCompletedCourseList)
        
        return finalCgpa >= earlierCgpa
    }

    // Planning metrics
    fun getTargetGpaFeasibility(): String {
        val deg = activeDegree.value ?: return "Unknown"
        val scale = getActiveDegreeScale() ?: return "Unknown"
        val maxScaleGpa = scale.maxPoints
        
        val target = deg.targetGpa
        val completedCredits = getCompletedCredits()
        val totalNeeded = deg.totalCreditsNeeded
        val currentCgpa = getCurrentCGPA()

        if (completedCredits >= totalNeeded) {
            return if (currentCgpa >= target) "Goal Reached!" else "Completed (Missed Target)"
        }

        val remainingCredits = (totalNeeded - completedCredits).coerceAtLeast(0.0)
        if (remainingCredits <= 0.0) return "Goal Reached!"

        val requiredPoints = (target * totalNeeded) - (currentCgpa * completedCredits)
        val requiredAvg = requiredPoints / remainingCredits

        if (requiredAvg <= 0.0) return "Very Realistic"
        if (requiredAvg > maxScaleGpa) return "Impossible"
        
        val pct = requiredAvg / maxScaleGpa
        return when {
            pct < 0.5 -> "Very Realistic"
            pct < 0.8 -> "Realistic / Achievable"
            pct < 0.92 -> "Challenging"
            else -> "Very Difficult"
        }
    }

    fun getRequiredFutureGpa(): Double {
        val deg = activeDegree.value ?: return 0.0
        val completedCredits = getCompletedCredits()
        val totalNeeded = deg.totalCreditsNeeded
        val currentCgpa = getCurrentCGPA()

        val remainingCredits = (totalNeeded - completedCredits).coerceAtLeast(0.0)
        if (remainingCredits <= 0.0) return 0.0

        val requiredPoints = (target * totalNeeded) - (currentCgpa * completedCredits)
        return (requiredPoints / remainingCredits).coerceAtLeast(0.0)
    }

    private val target: Double
        get() = activeDegree.value?.targetGpa ?: 3.5

    // --- WHAT-IF CALCULATOR PRESETS ---
    fun getwhatIfCgpaAndPreview(scenariosPreset: String): Double {
        // Scenarios: "Optimistic", "Realistic", "Minimum"
        val scale = getActiveDegreeScale() ?: return 0.0
        val entries = getActiveScaleEntries()
        
        val optimisticGpa = scale.maxPoints // E.g., 4.0
        val realisticGpa = getCurrentCGPA().coerceIn(0.0, scale.maxPoints)
        val minimumGpa = entries.lastOrNull { it.points > 0.0 }?.points ?: 1.0 // E.g., D or equivalent pass points
        
        val targetPoints = when (scenariosPreset) {
            "Optimistic" -> optimisticGpa
            "Realistic" -> realisticGpa
            else -> minimumGpa
        }
        
        // Compute combined CGPA of standard completed courses PLUS all non-completed/hypothetical courses using the selected target scenario points
        val currentCourses = _completedCourses.value
        val simulatedCourses = courses.value.filter { !it.isCompleted || it.grade.isBlank() }
        
        val currentWeights = currentCourses.sumOf { it.credits * it.points }
        val currentCredits = currentCourses.sumOf { it.credits }
        
        val simulatedWeights = simulatedCourses.sumOf { it.credits * targetPoints }
        val simulatedCredits = simulatedCourses.sumOf { it.credits }
        
        val totalCredits = currentCredits + simulatedCredits
        if (totalCredits <= 0.0) return 0.0
        return (currentWeights + simulatedWeights) / totalCredits
    }

    fun getEstimatedHypotheticalCgpa(): Double {
        // Compute overall CGPA including all courses (both completed with actual grades, and hypothetical with their simulation grades)
        val allCourses = courses.value
        var weightedSum = 0.0
        var totalCredits = 0.0
        allCourses.forEach { course ->
            // Let's count course if it has points assigned
            weightedSum += course.credits * course.points
            totalCredits += course.credits
        }
        return if (totalCredits > 0.0) weightedSum / totalCredits else 0.0
    }

    // --- JSON BACKUP IMPORT/EXPORT ---
    fun exportDataToJson(context: Context): String? {
        val activeDeg = activeDegree.value ?: return null
        try {
            val json = JSONObject()
            json.put("degree_name", activeDeg.name)
            json.put("target_gpa", activeDeg.targetGpa)
            json.put("total_credits_needed", activeDeg.totalCreditsNeeded)
            json.put("active_scale_id", activeDeg.activeScaleId)

            val semestersArray = JSONArray()
            val sems = semesters.value
            val crs = courses.value

            sems.forEach { sem ->
                val semJson = JSONObject()
                semJson.put("name", sem.name)
                semJson.put("goal_gpa", sem.goalGpa ?: JSONObject.NULL)
                semJson.put("is_hypothetical", sem.isHypothetical)

                val semCourses = crs.filter { it.semesterId == sem.id }
                val coursesArray = JSONArray()
                semCourses.forEach { course ->
                    val cJson = JSONObject()
                    cJson.put("code", course.code)
                    cJson.put("name", course.name)
                    cJson.put("credits", course.credits)
                    cJson.put("grade", course.grade)
                    cJson.put("points", course.points)
                    cJson.put("is_completed", course.isCompleted)
                    cJson.put("notes", course.notes)
                    coursesArray.put(cJson)
                }
                semJson.put("courses", coursesArray)
                semestersArray.put(semJson)
            }
            json.put("semesters", semestersArray)

            val exportString = json.toString(4)
            return exportString
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun importDataFromJson(context: Context, jsonString: String): Boolean {
        try {
            val json = JSONObject(jsonString)
            val name = json.getString("degree_name")
            val targetGpa = json.getDouble("target_gpa")
            val totalCreditsNeeded = json.getDouble("total_credits_needed")
            val activeScaleId = json.optInt("active_scale_id", 1)

            viewModelScope.launch {
                val degId = repository.insertDegree(
                    Degree(
                        name = name,
                        targetGpa = targetGpa,
                        totalCreditsNeeded = totalCreditsNeeded,
                        isActive = true,
                        activeScaleId = activeScaleId
                    )
                ).toInt()
                
                repository.setActiveDegree(degId)

                val semestersArray = json.getJSONArray("semesters")
                for (i in 0 until semestersArray.length()) {
                    val semJson = semestersArray.getJSONObject(i)
                    val semName = semJson.getString("name")
                    val goalGpaRaw = semJson.get("goal_gpa")
                    val goalGpa = if (goalGpaRaw == JSONObject.NULL) null else semJson.getDouble("goal_gpa")
                    val isHypo = semJson.optBoolean("is_hypothetical", false)

                    val semId = repository.insertSemester(
                        Semester(degreeId = degId, name = semName, goalGpa = goalGpa, isHypothetical = isHypo)
                    ).toInt()

                    val coursesArray = semJson.getJSONArray("courses")
                    for (j in 0 until coursesArray.length()) {
                        val cJson = coursesArray.getJSONObject(j)
                        repository.insertCourse(
                            Course(
                                semesterId = semId,
                                code = cJson.getString("code"),
                                name = cJson.getString("name"),
                                credits = cJson.getDouble("credits"),
                                grade = cJson.getString("grade"),
                                points = cJson.getDouble("points"),
                                isCompleted = cJson.optBoolean("is_completed", true),
                                notes = cJson.optString("notes", "")
                            )
                        )
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // --- PDF EXPORT (COMPLETELY LOCAL & OFFLINE) ---
    fun exportToPdf(context: Context): File? {
        val activeDeg = activeDegree.value ?: return null
        val sems = semesters.value.filter { !it.isHypothetical }
        val allCrs = courses.value

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page size in postscript points
        val page = pdfDocument.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Page title
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("TargetGPA Academic Report", 40f, 60f, paint)

        // Subtitle & Degree
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Program: ${activeDeg.name}", 40f, 85f, paint)
        canvas.drawText("Target GPA: ${String.format("%.2f", activeDeg.targetGpa)} | Current CGPA: ${String.format("%.2f", getCurrentCGPA())}", 40f, 105f, paint)
        canvas.drawText("Credits Completed: ${String.format("%.1f", getCompletedCredits())} / ${String.format("%.1f", activeDeg.totalCreditsNeeded)}", 40f, 125f, paint)

        var yPos = 160f
        paint.isFakeBoldText = true
        canvas.drawText("Semester Summary", 40f, yPos, paint)
        yPos += 20f
        paint.isFakeBoldText = false

        sems.forEach { sem ->
            val semCourses = allCrs.filter { it.semesterId == sem.id }
            val sgpa = calculateGPAForCourses(semCourses)
            
            paint.isFakeBoldText = true
            canvas.drawText("${sem.name} | SGPA: ${String.format("%.2f", sgpa)}", 40f, yPos, paint)
            yPos += 15f
            paint.isFakeBoldText = false

            semCourses.forEach { course ->
                val line = "  • ${course.code} - ${course.name} (${course.credits} Credits): Grade ${course.grade}"
                canvas.drawText(line, 40f, yPos, paint)
                yPos += 15f
                if (yPos > 800f) {
                    // Start new page in production if needed, but for small records single page is fine
                }
            }
            yPos += 10f
        }

        // Add generation signature
        yPos += 20f
        paint.textSize = 10f
        canvas.drawText("Generated offline by TargetGPA Planner App", 40f, 800f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to files directory
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TargetGPA_Academic_Record.pdf")
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
