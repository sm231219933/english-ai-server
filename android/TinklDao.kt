package com.TeacherTinkl.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TinklDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE subject = :subject")
    fun getQuestionsBySubject(subject: String): Flow<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question)

    @Query("SELECT * FROM formulas")
    fun getAllFormulas(): Flow<List<Formula>>

    @Query("SELECT * FROM formulas WHERE subject = :subject")
    fun getFormulasBySubject(subject: String): Flow<List<Formula>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormula(formula: Formula)

    // Speaking Practice Dao Queries - GROUP BY title to prevent duplicates
    @Query("SELECT * FROM speaking_lessons GROUP BY title ORDER BY id ASC")
    fun getAllSpeakingLessons(): Flow<List<SpeakingLesson>>

    @Query("SELECT COUNT(*) FROM speaking_lessons")
    suspend fun getSpeakingLessonCount(): Int

    @Query("SELECT * FROM speaking_lessons WHERE id = :lessonId")
    suspend fun getSpeakingLessonById(lessonId: Int): SpeakingLesson?

    @Query("SELECT * FROM lesson_sentences WHERE lessonId = :lessonId GROUP BY promptHindi ORDER BY id ASC")
    fun getSentencesForLesson(lessonId: Int): Flow<List<LessonSentence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakingLesson(lesson: SpeakingLesson): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonSentence(sentence: LessonSentence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakingReport(report: SpeakingReport)

    @Query("SELECT * FROM speaking_reports ORDER BY timestamp DESC")
    fun getAllSpeakingReports(): Flow<List<SpeakingReport>>

    // Speaking Gym Dao Queries
    @Query("SELECT * FROM speaking_gym_sentences WHERE level = :level")
    fun getGymSentencesByLevel(level: Int): Flow<List<SpeakingGymSentence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymSentence(sentence: SpeakingGymSentence)
}
