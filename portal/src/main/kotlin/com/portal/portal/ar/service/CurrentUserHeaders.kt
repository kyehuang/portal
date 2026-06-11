package com.portal.portal.ar.service

import com.portal.portal.ar.model.CurrentUser
import jakarta.ws.rs.BadRequestException

fun currentUserFromHeaders(userId: String?, sectionId: String?): CurrentUser {
    if (userId.isNullOrBlank()) {
        throw BadRequestException("X-User-Id header is required")
    }

    if (sectionId.isNullOrBlank()) {
        throw BadRequestException("X-Section-Id header is required")
    }

    return CurrentUser(userId.trim(), sectionId.trim())
}
