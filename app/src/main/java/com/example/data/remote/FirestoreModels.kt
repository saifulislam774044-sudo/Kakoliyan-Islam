package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "merchant",
    val active: Boolean = true,
    val businessCount: Long = 0L,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SupportRequestDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val businessName: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AnnouncementDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
