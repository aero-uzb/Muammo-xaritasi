package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "problems")
data class LocalProblem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val categoryId: String,       // "road", "trash", "traffic_light", "water", "electricity"
    val categoryName: String,     // e.g. "Yo'l muammolari" (Road Damage)
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val status: String,           // "NEW", "ACCEPTED", "IN_PROGRESS", "RESOLVED"
    val votesCount: Int = 0,
    val hasVoted: Boolean = false,
    val imageUrl: String = "",    // Simulation path or actual Base64 / URL
    val createdAt: Long = System.currentTimeMillis(),
    val reporterName: String = "Sardor Ahmedov",
    val isAiApproved: Boolean = true,
    val aiLabels: String = ""     // AI-detected labels comma separated
) : Serializable
