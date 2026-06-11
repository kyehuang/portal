package com.portal.portal.ar

import com.github.f4b6a3.uuid.UuidCreator
import com.portal.portal.user.UserRepo
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime

@ApplicationScoped
class ArService(private val arRepo: ArRepo, private val userRepo: UserRepo) {
    suspend fun listArs(tag: String?, priority: String?, status: String?, assignee: String?): List<Ar> = arRepo.listArs(
        normalizedTag = validateSearchTag(tag)?.lowercase(),
        priority = priority?.let(::validatePriority),
        status = status?.let(::validateStatus),
        assignee = assignee?.trim()?.takeIf { it.isNotEmpty() },
    )

    suspend fun getAr(id: String): Ar = arRepo.findArById(id)
        ?: throw WebApplicationException("AR not found", Response.Status.NOT_FOUND)

    suspend fun createAr(
        title: String?,
        description: String?,
        priority: String?,
        status: String?,
        tags: List<String>?,
        assignee: String?,
        dueDate: LocalDateTime?,
        section: String?,
        creator: String?,
        currentUser: CurrentUser,
    ): Ar {
        validateImmutableFields(section, creator)
        validateSectionMembership(currentUser.id, currentUser.sectionId)
        val normalizedTitle = requireTitle(title)
        val normalizedDescription = description ?: ""
        val normalizedPriority = validatePriority(priority ?: PRIORITY_P2)
        val normalizedStatus = validateStatus(status ?: STATUS_IN_PROGRESS)
        val normalizedTags = normalizeTags(tags ?: emptyList())
        validateDueDate(dueDate)

        val id = UuidCreator.getTimeOrderedEpoch().toString()
        arRepo.insertAr(
            id = id,
            title = normalizedTitle,
            description = normalizedDescription,
            section = currentUser.sectionId,
            priority = normalizedPriority,
            status = normalizedStatus,
            tags = normalizedTags,
            creator = currentUser.id,
            assignee = assignee?.trim()?.takeIf { it.isNotEmpty() },
            dueDate = dueDate,
        )

        return getAr(id)
    }

    suspend fun updateAr(
        id: String,
        title: String?,
        description: String?,
        priority: String?,
        status: String?,
        tags: List<String>?,
        assignee: String?,
        dueDate: LocalDateTime?,
        dueDateProvided: Boolean,
        section: String?,
        creator: String?,
    ): Ar {
        validateImmutableFields(section, creator)
        dueDate?.let(::validateDueDate)

        val existing = getAr(id)
        val nextStatus = status?.let(::validateStatus) ?: existing.status
        if (existing.status != STATUS_DONE && nextStatus == STATUS_DONE) {
            val subArs = arRepo.listSubArsByParentArId(id)
            if (subArs.any { it.status != STATUS_DONE }) {
                throw WebApplicationException(
                    "All sub ARs must be done before marking AR as done",
                    Response.Status.BAD_REQUEST,
                )
            }
        }

        val updated = existing.copy(
            title = title?.let(::requireTitle) ?: existing.title,
            description = description ?: existing.description,
            priority = priority?.let(::validatePriority) ?: existing.priority,
            status = nextStatus,
            tags = tags?.let(::normalizeTags) ?: existing.tags,
            assignee = assignee?.trim()?.takeIf { it.isNotEmpty() } ?: existing.assignee,
            dueDate = if (dueDateProvided) dueDate else existing.dueDate,
        )

        arRepo.updateAr(updated)
        return getAr(id)
    }

    suspend fun deleteAr(id: String) {
        val deleted = arRepo.deleteAr(id)
        if (deleted == 0) {
            throw WebApplicationException("AR not found", Response.Status.NOT_FOUND)
        }
    }

    suspend fun listSubArs(tag: String?, priority: String?, status: String?, assignee: String?): List<SubAr> =
        arRepo.listSubArs(
            normalizedTag = validateSearchTag(tag)?.lowercase(),
            priority = priority?.let(::validatePriority),
            status = status?.let(::validateStatus),
            assignee = assignee?.trim()?.takeIf { it.isNotEmpty() },
        )

    suspend fun getSubAr(id: String): SubAr = arRepo.findSubArById(id)
        ?: throw WebApplicationException("Sub AR not found", Response.Status.NOT_FOUND)

    suspend fun createSubAr(
        parentArId: String?,
        title: String?,
        description: String?,
        priority: String?,
        status: String?,
        tags: List<String>?,
        assignee: String?,
        dueDate: LocalDateTime?,
        section: String?,
        creator: String?,
        currentUser: CurrentUser,
    ): SubAr {
        validateImmutableFields(section, creator)
        val normalizedParentArId = parentArId?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw WebApplicationException("parentArId is required", Response.Status.BAD_REQUEST)
        val parentAr = getAr(normalizedParentArId)
        ensureParentArIsEditable(parentAr)
        validateSectionMembership(currentUser.id, parentAr.section)

        val normalizedTitle = requireTitle(title)
        val normalizedDescription = description ?: ""
        val normalizedPriority = validatePriority(priority ?: PRIORITY_P2)
        val normalizedStatus = validateStatus(status ?: STATUS_IN_PROGRESS)
        val normalizedTags = normalizeTags(tags ?: emptyList())
        validateDueDate(dueDate)

        val id = UuidCreator.getTimeOrderedEpoch().toString()
        arRepo.insertSubAr(
            id = id,
            parentArId = parentAr.id,
            title = normalizedTitle,
            description = normalizedDescription,
            section = parentAr.section,
            priority = normalizedPriority,
            status = normalizedStatus,
            tags = normalizedTags,
            creator = currentUser.id,
            assignee = assignee?.trim()?.takeIf { it.isNotEmpty() },
            dueDate = dueDate,
        )

        return getSubAr(id)
    }

    suspend fun updateSubAr(
        id: String,
        title: String?,
        description: String?,
        priority: String?,
        status: String?,
        tags: List<String>?,
        assignee: String?,
        dueDate: LocalDateTime?,
        dueDateProvided: Boolean,
        section: String?,
        creator: String?,
        parentArId: String?,
    ): SubAr {
        validateImmutableFields(section, creator)
        if (!parentArId.isNullOrBlank()) {
            throw WebApplicationException("parentArId cannot be updated", Response.Status.BAD_REQUEST)
        }
        dueDate?.let(::validateDueDate)

        val existing = getSubAr(id)
        ensureParentArIsEditable(getAr(existing.parentArId))

        val updated = existing.copy(
            title = title?.let(::requireTitle) ?: existing.title,
            description = description ?: existing.description,
            priority = priority?.let(::validatePriority) ?: existing.priority,
            status = status?.let(::validateStatus) ?: existing.status,
            tags = tags?.let(::normalizeTags) ?: existing.tags,
            assignee = assignee?.trim()?.takeIf { it.isNotEmpty() } ?: existing.assignee,
            dueDate = if (dueDateProvided) dueDate else existing.dueDate,
        )

        arRepo.updateSubAr(updated)
        return getSubAr(id)
    }

    suspend fun deleteSubAr(id: String) {
        val existing = getSubAr(id)
        ensureParentArIsEditable(getAr(existing.parentArId))

        val deleted = arRepo.deleteSubAr(id)
        if (deleted == 0) {
            throw WebApplicationException("Sub AR not found", Response.Status.NOT_FOUND)
        }
    }

    private fun ensureParentArIsEditable(parentAr: Ar) {
        if (parentAr.status == STATUS_DONE) {
            throw WebApplicationException(
                "Sub AR cannot be changed when parent AR is done",
                Response.Status.BAD_REQUEST,
            )
        }
    }

    private suspend fun validateSectionMembership(userId: String, sectionId: String) {
        if (!userRepo.isMemberOfSection(userId, sectionId)) {
            throw WebApplicationException("user is not a member of section", Response.Status.FORBIDDEN)
        }
    }

    private fun validateImmutableFields(section: String?, creator: String?) {
        if (section != null) {
            throw WebApplicationException("section cannot be provided or updated", Response.Status.BAD_REQUEST)
        }

        if (creator != null) {
            throw WebApplicationException("creator cannot be provided or updated", Response.Status.BAD_REQUEST)
        }
    }

    private fun requireTitle(title: String?): String {
        val trimmed = title?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            throw WebApplicationException("title is required", Response.Status.BAD_REQUEST)
        }
        return trimmed
    }

    private fun validatePriority(priority: String): String {
        val normalized = priority.trim()
        if (normalized !in setOf(PRIORITY_P0, PRIORITY_P1, PRIORITY_P2)) {
            throw WebApplicationException("priority must be p0, p1, or p2", Response.Status.BAD_REQUEST)
        }
        return normalized
    }

    private fun validateStatus(status: String): String {
        val normalized = status.trim()
        if (normalized !in setOf(STATUS_IN_PROGRESS, STATUS_DONE)) {
            throw WebApplicationException("status must be in_progress or done", Response.Status.BAD_REQUEST)
        }
        return normalized
    }

    private fun validateDueDate(dueDate: LocalDateTime?) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw WebApplicationException(
                "dueDate must be greater than or equal to server now",
                Response.Status.BAD_REQUEST,
            )
        }
    }

    private fun validateSearchTag(tag: String?): String? {
        val trimmed = tag?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        validateTag(trimmed)
        return trimmed
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        val normalizedTags = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        tags.forEach { tag ->
            val trimmed = tag.trim()
            validateTag(trimmed)
            val key = trimmed.lowercase()
            if (seen.add(key)) {
                normalizedTags.add(trimmed)
            }
        }

        return normalizedTags
    }

    private fun validateTag(tag: String) {
        if (tag.isBlank()) {
            throw WebApplicationException("tag cannot be blank", Response.Status.BAD_REQUEST)
        }

        if (tag.any { it.isWhitespace() }) {
            throw WebApplicationException("Tag cannot contain whitespace", Response.Status.BAD_REQUEST)
        }
    }
}
