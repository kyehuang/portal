package com.portal.portal.ar

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response

fun currentUserFromHeaders(userId: String?, sectionId: String?): CurrentUser {
    if (userId.isNullOrBlank()) {
        throw WebApplicationException("X-User-Id header is required", Response.Status.BAD_REQUEST)
    }

    if (sectionId.isNullOrBlank()) {
        throw WebApplicationException("X-Section-Id header is required", Response.Status.BAD_REQUEST)
    }

    return CurrentUser(userId.trim(), sectionId.trim())
}
