package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GpaDao {

    // --- Degree Operations ---
    @Query("SELECT * FROM degrees WHERE isActive = 1 LIMIT 1")
    fun getActiveDegreeFlow(): Flow<Degree?>

    @Query("SELECT * FROM degrees WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveDegree(): Degree?

    @Query("SELECT * FROM degrees ORDER BY name ASC")
    fun getAllDegreesFlow(): Flow<List<Degree>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDegree(degree: Degree): Long

    @Update
    suspend fun updateDegree(degree: Degree)

    @Delete
    suspend fun deleteDegree(degree: Degree)

    @Query("UPDATE degrees SET isActive = 0")
    suspend fun deactivateAllDegrees()

    @Transaction
    suspend fun setActiveDegree(degreeId: Int) {
        deactivateAllDegrees()
        val deg = getDegreeById(degreeId)
        if (deg != null) {
            updateDegree(deg.copy(isActive = true))
        }
    }

    @Query("SELECT * FROM degrees WHERE id = :id")
    suspend fun getDegreeById(id: Int): Degree?


    // --- Grading Scale Operations ---
    @Query("SELECT * FROM grading_scales ORDER BY isSystemDefault DESC, name ASC")
    fun getAllGradingScalesFlow(): Flow<List<GradingScale>>

    @Query("SELECT * FROM grading_scales ORDER BY isSystemDefault DESC, name ASC")
    suspend fun getAllGradingScales(): List<GradingScale>

    @Query("SELECT * FROM grading_scales WHERE id = :id")
    suspend fun getGradingScaleById(id: Int): GradingScale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingScale(scale: GradingScale): Long

    @Query("DELETE FROM grading_scales WHERE id = :id")
    suspend fun deleteGradingScaleById(id: Int)

    // --- Grade Entry Operations ---
    @Query("SELECT * FROM grade_entries WHERE scaleId = :scaleId ORDER BY points DESC")
    fun getGradeEntriesFlow(scaleId: Int): Flow<List<GradeEntry>>

    @Query("SELECT * FROM grade_entries WHERE scaleId = :scaleId ORDER BY points DESC")
    suspend fun getGradeEntries(scaleId: Int): List<GradeEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeEntry(entry: GradeEntry): Long

    @Query("DELETE FROM grade_entries WHERE scaleId = :scaleId")
    suspend fun deleteGradeEntriesForScale(scaleId: Int)


    // --- Semester Operations ---
    @Query("SELECT * FROM semesters WHERE degreeId = :degreeId ORDER BY isHypothetical ASC, id ASC")
    fun getSemestersForDegreeFlow(degreeId: Int): Flow<List<Semester>>

    @Query("SELECT * FROM semesters WHERE degreeId = :degreeId ORDER BY isHypothetical ASC, id ASC")
    suspend fun getSemestersForDegree(degreeId: Int): List<Semester>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)


    // --- Course Operations ---
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY id ASC")
    fun getCoursesForSemesterFlow(semesterId: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY id ASC")
    suspend fun getCoursesForSemester(semesterId: Int): List<Course>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("""
        SELECT c.* FROM courses c 
        INNER JOIN semesters s ON c.semesterId = s.id 
        WHERE s.degreeId = :degreeId AND s.isHypothetical = 0
    """)
    fun getAllCompletedCoursesForDegreeFlow(degreeId: Int): Flow<List<Course>>

    @Query("""
        SELECT c.* FROM courses c 
        INNER JOIN semesters s ON c.semesterId = s.id 
        WHERE s.degreeId = :degreeId
    """)
    fun getAllCoursesIncludingHypotheticalFlow(degreeId: Int): Flow<List<Course>>

    @Query("""
        SELECT c.* FROM courses c 
        INNER JOIN semesters s ON c.semesterId = s.id 
        WHERE s.degreeId = :degreeId AND (c.name LIKE '%' || :query || '%' OR c.code LIKE '%' || :query || '%')
    """)
    fun searchCoursesFlow(degreeId: Int, query: String): Flow<List<Course>>
}
