package com.portal.portal.user

import java.time.LocalDateTime

data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class CreateUserRequest(val name: String, val email: String)
data class UpdateUserRequest(val name: String, val email: String)
data class AddUserSectionRequest(val sectionId: String)
