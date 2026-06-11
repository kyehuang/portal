package com.portal.portal.ar.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime

const val STATUS_IN_PROGRESS = "in_progress"
const val STATUS_DONE = "done"
const val PRIORITY_P0 = "p0"
const val PRIORITY_P1 = "p1"
const val PRIORITY_P2 = "p2"

data class CurrentUser(val id: String, val sectionId: String)

data class Ar(
    val id: String,
    val title: String,
    val description: String,
    val section: String,
    val priority: String,
    val status: String,
    val tags: List<String>,
    val creator: String,
    val assignee: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
)

data class SubAr(
    val id: String,
    val parentArId: String,
    val title: String,
    val description: String,
    val section: String,
    val priority: String,
    val status: String,
    val tags: List<String>,
    val creator: String,
    val assignee: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
)

data class CreateArRequest(
    val title: String?,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val assignee: String? = null,
    val dueDate: LocalDateTime? = null,
    val section: String? = null,
    val creator: String? = null,
)

data class UpdateArRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val assignee: String? = null,
    val dueDate: LocalDateTime? = null,
    @JsonIgnore val dueDateProvided: Boolean = false,
    val section: String? = null,
    val creator: String? = null,
)

data class CreateSubArRequest(
    val parentArId: String?,
    val title: String?,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val assignee: String? = null,
    val dueDate: LocalDateTime? = null,
    val section: String? = null,
    val creator: String? = null,
)

data class UpdateSubArRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val assignee: String? = null,
    val dueDate: LocalDateTime? = null,
    @JsonIgnore val dueDateProvided: Boolean = false,
    val section: String? = null,
    val creator: String? = null,
    val parentArId: String? = null,
)

data class ArRow(
    val id: String,
    val title: String,
    val description: String,
    val section: String,
    val priority: String,
    val status: String,
    val creator: String,
    val assignee: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
)

data class SubArRow(
    val id: String,
    val parentArId: String,
    val title: String,
    val description: String,
    val section: String,
    val priority: String,
    val status: String,
    val creator: String,
    val assignee: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
)

data class ArTagRow(val arId: String, val tag: String)
data class SubArTagRow(val subArId: String, val tag: String)
