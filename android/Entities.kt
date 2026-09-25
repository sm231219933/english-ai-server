package com.TeacherTinkl.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String, // "A", "B", "C", or "D"
    val explanation: String
)

@Entity(tableName = "formulas")
data class Formula(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val name: String,
    val expression: String
)

@Entity(tableName = "speaking_lessons")
data class SpeakingLesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val title: String,
    val description: String,
    val level: String,
    val iconName: String = "Restaurant"
)

@Entity(tableName = "lesson_sentences")
data class LessonSentence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: Int,
    val stepType: String = "LISTEN_REPEAT",
    val personTag: String = "1st Person", // "1st Person", "2nd Person", "3rd Person"
    val promptHindi: String? = null,
    val promptQuestion: String,
    val expectedText: String,
    val patternTemplate: String? = null,
    val wordMappingJson: String? = null
)

@Entity(tableName = "speaking_reports")
data class SpeakingReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val lessonId: Int,
    val rawTranscribedText: String,
    val correctedText: String,
    val matchScore: Int,
    val mistakesCount: Int
)

@Entity(tableName = "speaking_gym_sentences")
data class SpeakingGymSentence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int,
    val category: String,
    val promptHindi: String? = null,
    val promptQuestion: String,
    val expectedSentence: String,
    val hintPattern: String? = null
)
