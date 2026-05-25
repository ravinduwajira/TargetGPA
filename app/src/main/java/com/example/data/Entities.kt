package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "degrees")
data class Degree(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetGpa: Double = 3.5,
    val totalCreditsNeeded: Double = 120.0,
    val isActive: Boolean = false,
    val activeScaleId: Int = 1 // points to standard scale
)

@Entity(tableName = "grading_scales")
data class GradingScale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val maxPoints: Double = 4.0,
    val isSystemDefault: Boolean = false
)

@Entity(
    tableName = "grade_entries",
    foreignKeys = [
        ForeignKey(
            entity = GradingScale::class,
            parentColumns = ["id"],
            childColumns = ["scaleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GradeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scaleId: Int,
    val letter: String,
    val points: Double
)

@Entity(
    tableName = "semesters",
    foreignKeys = [
        ForeignKey(
            entity = Degree::class,
            parentColumns = ["id"],
            childColumns = ["degreeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val degreeId: Int,
    val name: String,
    val goalGpa: Double? = null,
    val isHypothetical: Boolean = false
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val code: String,
    val name: String,
    val credits: Double,
    val grade: String,     // E.g., "A", "B+", or Empty if in-progress
    val points: Double,    // Point value at completion
    val isCompleted: Boolean = true,
    val notes: String = ""
)
