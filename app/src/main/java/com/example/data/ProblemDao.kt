package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems ORDER BY createdAt DESC")
    fun getAllProblemsFlow(): Flow<List<LocalProblem>>

    @Query("SELECT * FROM problems WHERE id = :id")
    fun getProblemById(id: Int): Flow<LocalProblem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: LocalProblem): Long

    @Update
    suspend fun updateProblem(problem: LocalProblem)

    @Delete
    suspend fun deleteProblem(problem: LocalProblem)

    @Query("SELECT * FROM comments WHERE problemId = :problemId ORDER BY timestamp ASC")
    fun getCommentsForProblem(problemId: Int): Flow<List<LocalComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: LocalComment)

    @Query("SELECT COUNT(*) FROM problems")
    suspend fun getTotalProblemsCount(): Int

    @Query("SELECT COUNT(*) FROM problems WHERE status = 'RESOLVED'")
    suspend fun getResolvedProblemsCount(): Int

    @Query("SELECT COUNT(*) FROM problems WHERE status = 'IN_PROGRESS'")
    suspend fun getInProgressProblemsCount(): Int

    @Query("SELECT COUNT(*) FROM problems WHERE status = 'NEW'")
    suspend fun getNewProblemsCount(): Int

    @Query("SELECT COUNT(*) FROM problems WHERE status = 'ACCEPTED'")
    suspend fun getAcceptedProblemsCount(): Int
}
