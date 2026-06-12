package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class LocalComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val problemId: Int,
    val authorName: String,
    val authorRole: String = "USER", // "USER", "ADMIN", "MODERATOR"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
