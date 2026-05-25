package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GpaRepository(private val gpaDao: GpaDao) {

    val activeDegreeFlow: Flow<Degree?> = gpaDao.getActiveDegreeFlow()
    val allDegreesFlow: Flow<List<Degree>> = gpaDao.getAllDegreesFlow()
    val allGradingScalesFlow: Flow<List<GradingScale>> = gpaDao.getAllGradingScalesFlow()

    fun getGradeEntriesFlow(scaleId: Int): Flow<List<GradeEntry>> = gpaDao.getGradeEntriesFlow(scaleId)
    suspend fun getGradeEntries(scaleId: Int) = gpaDao.getGradeEntries(scaleId)

    fun getSemestersForDegreeFlow(degreeId: Int): Flow<List<Semester>> = gpaDao.getSemestersForDegreeFlow(degreeId)
    suspend fun getSemestersForDegree(degreeId: Int) = gpaDao.getSemestersForDegree(degreeId)

    fun getCoursesForSemesterFlow(semesterId: Int): Flow<List<Course>> = gpaDao.getCoursesForSemesterFlow(semesterId)
    suspend fun getCoursesForSemester(semesterId: Int) = gpaDao.getCoursesForSemester(semesterId)

    fun getAllCompletedCoursesForDegreeFlow(degreeId: Int): Flow<List<Course>> = gpaDao.getAllCompletedCoursesForDegreeFlow(degreeId)
    fun getAllCoursesIncludingHypotheticalFlow(degreeId: Int): Flow<List<Course>> = gpaDao.getAllCoursesIncludingHypotheticalFlow(degreeId)

    fun searchCoursesFlow(degreeId: Int, query: String): Flow<List<Course>> = gpaDao.searchCoursesFlow(degreeId, query)

    // Ensure initial database seeds are created
    suspend fun ensureDatabaseSeeded() {
        val scales = gpaDao.getAllGradingScales()
        if (scales.isEmpty()) {
            // Seed 4.0 scale
            val scale4Id = gpaDao.insertGradingScale(GradingScale(name = "4.0 Scale (Standard)", maxPoints = 4.0, isSystemDefault = true)).toInt()
            val entries4 = listOf(
                GradeEntry(scaleId = scale4Id, letter = "A+", points = 4.0),
                GradeEntry(scaleId = scale4Id, letter = "A", points = 4.0),
                GradeEntry(scaleId = scale4Id, letter = "A-", points = 3.7),
                GradeEntry(scaleId = scale4Id, letter = "B+", points = 3.3),
                GradeEntry(scaleId = scale4Id, letter = "B", points = 3.0),
                GradeEntry(scaleId = scale4Id, letter = "B-", points = 2.7),
                GradeEntry(scaleId = scale4Id, letter = "C+", points = 2.3),
                GradeEntry(scaleId = scale4Id, letter = "C", points = 2.0),
                GradeEntry(scaleId = scale4Id, letter = "C-", points = 1.7),
                GradeEntry(scaleId = scale4Id, letter = "D+", points = 1.3),
                GradeEntry(scaleId = scale4Id, letter = "D", points = 1.0),
                GradeEntry(scaleId = scale4Id, letter = "F", points = 0.0)
            )
            entries4.forEach { gpaDao.insertGradeEntry(it) }

            // Seed 5.0 scale
            val scale5Id = gpaDao.insertGradingScale(GradingScale(name = "5.0 Scale", maxPoints = 5.0, isSystemDefault = false)).toInt()
            val entries5 = listOf(
                GradeEntry(scaleId = scale5Id, letter = "A", points = 5.0),
                GradeEntry(scaleId = scale5Id, letter = "B", points = 4.0),
                GradeEntry(scaleId = scale5Id, letter = "C", points = 3.0),
                GradeEntry(scaleId = scale5Id, letter = "D", points = 2.0),
                GradeEntry(scaleId = scale5Id, letter = "E", points = 1.0),
                GradeEntry(scaleId = scale5Id, letter = "F", points = 0.0)
            )
            entries5.forEach { gpaDao.insertGradeEntry(it) }

            // Seed 10.0 scale
            val scale10Id = gpaDao.insertGradingScale(GradingScale(name = "10.0 Scale (Percentile/CGPA)", maxPoints = 10.0, isSystemDefault = false)).toInt()
            val entries10 = listOf(
                GradeEntry(scaleId = scale10Id, letter = "O", points = 10.0),
                GradeEntry(scaleId = scale10Id, letter = "A+", points = 9.0),
                GradeEntry(scaleId = scale10Id, letter = "A", points = 8.0),
                GradeEntry(scaleId = scale10Id, letter = "B+", points = 7.0),
                GradeEntry(scaleId = scale10Id, letter = "B", points = 6.0),
                GradeEntry(scaleId = scale10Id, letter = "C", points = 5.0),
                GradeEntry(scaleId = scale10Id, letter = "P", points = 4.0),
                GradeEntry(scaleId = scale10Id, letter = "F", points = 0.0)
            )
            entries10.forEach { gpaDao.insertGradeEntry(it) }

            // Seed initial degree
            val degreeId = gpaDao.insertDegree(
                Degree(
                    name = "B.Sc. Computer Science",
                    targetGpa = 3.5,
                    totalCreditsNeeded = 120.0,
                    isActive = true,
                    activeScaleId = scale4Id
                )
            ).toInt()

            // Seed some default semesters
            val sem1Id = gpaDao.insertSemester(Semester(degreeId = degreeId, name = "Semester 1", goalGpa = 3.5)).toInt()
            val sem2Id = gpaDao.insertSemester(Semester(degreeId = degreeId, name = "Semester 2", goalGpa = 3.6)).toInt()

            // Seed some courses
            gpaDao.insertCourse(Course(semesterId = sem1Id, code = "CS101", name = "Intro to Computer Science", credits = 4.0, grade = "A", points = 4.0, notes = "Covered basics of Python & memory management."))
            gpaDao.insertCourse(Course(semesterId = sem1Id, code = "MATH101", name = "Calculus I", credits = 3.0, grade = "B+", points = 3.3, notes = "Focused heavily on integration & differentiation."))
            gpaDao.insertCourse(Course(semesterId = sem1Id, code = "ENG101", name = "Academic Writing", credits = 3.0, grade = "A-", points = 3.7, notes = "Learned APA styling & structuring research literature."))

            gpaDao.insertCourse(Course(semesterId = sem2Id, code = "CS102", name = "Data Structures & Algorithms", credits = 4.0, grade = "A", points = 4.0, notes = "Covered Big-O, stack, queue, tree, graph, sorting."))
            gpaDao.insertCourse(Course(semesterId = sem2Id, code = "MATH102", name = "Linear Algebra", credits = 3.0, grade = "A-", points = 3.7, notes = "Solve systems of linear equations using matrices."))
            gpaDao.insertCourse(Course(semesterId = sem2Id, code = "PHYS101", name = "General Physics I", credits = 3.0, grade = "B", points = 3.0, notes = "Mechanics and thermodynamics. Lab reports count 30%."))
        }
    }

    suspend fun getActiveDegree() = gpaDao.getActiveDegree()

    suspend fun insertDegree(degree: Degree) = gpaDao.insertDegree(degree)
    suspend fun updateDegree(degree: Degree) = gpaDao.updateDegree(degree)
    suspend fun deleteDegree(degree: Degree) {
        gpaDao.deleteDegree(degree)
        // If the deleted degree was active, make another active if available
        val active = gpaDao.getActiveDegree()
        if (active == null) {
            val all = gpaDao.getAllDegreesFlow().firstOrNull() ?: emptyList()
            if (all.isNotEmpty()) {
                gpaDao.setActiveDegree(all[0].id)
            }
        }
    }

    suspend fun setActiveDegree(degreeId: Int) = gpaDao.setActiveDegree(degreeId)
    suspend fun getDegreeById(id: Int) = gpaDao.getDegreeById(id)

    suspend fun insertGradingScale(scale: GradingScale) = gpaDao.insertGradingScale(scale)
    suspend fun deleteGradingScaleById(id: Int) = gpaDao.deleteGradingScaleById(id)
    suspend fun getGradingScaleById(id: Int) = gpaDao.getGradingScaleById(id)

    suspend fun insertGradeEntry(entry: GradeEntry) = gpaDao.insertGradeEntry(entry)
    suspend fun deleteGradeEntriesForScale(scaleId: Int) = gpaDao.deleteGradeEntriesForScale(scaleId)

    suspend fun insertSemester(semester: Semester) = gpaDao.insertSemester(semester)
    suspend fun updateSemester(semester: Semester) = gpaDao.updateSemester(semester)
    suspend fun deleteSemester(semester: Semester) = gpaDao.deleteSemester(semester)

    suspend fun insertCourse(course: Course) = gpaDao.insertCourse(course)
    suspend fun updateCourse(course: Course) = gpaDao.updateCourse(course)
    suspend fun deleteCourse(course: Course) = gpaDao.deleteCourse(course)
}
