package com.portal.portal.ar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.portal.portal.ar.model.Ar
import com.portal.portal.ar.model.CreateArRequest
import com.portal.portal.ar.model.UpdateArRequest
import com.portal.portal.ar.service.ArService
import com.portal.portal.ar.service.currentUserFromHeaders
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.ResponseStatus

@Path("/ars")
class ArController(private val arService: ArService, private val objectMapper: ObjectMapper) {
    @GET
    suspend fun list(
        @QueryParam("tag") tag: String?,
        @QueryParam("priority") priority: String?,
        @QueryParam("status") status: String?,
        @QueryParam("assignee") assignee: String?,
    ): List<Ar> = arService.listArs(tag, priority, status, assignee)

    @GET
    @Path("/{id}")
    suspend fun get(@PathParam("id") id: String): Ar = arService.getAr(id)

    @POST
    @ResponseStatus(201)
    suspend fun create(
        request: CreateArRequest,
        @HeaderParam("X-User-Id") userId: String?,
        @HeaderParam("X-Section-Id") sectionId: String?,
    ): Ar = arService.createAr(
        title = request.title,
        description = request.description,
        priority = request.priority,
        status = request.status,
        tags = request.tags,
        assignee = request.assignee,
        dueDate = request.dueDate,
        section = request.section,
        creator = request.creator,
        currentUser = currentUserFromHeaders(userId, sectionId),
    )

    @PUT
    @Path("/{id}")
    suspend fun update(@PathParam("id") id: String, requestBody: JsonNode): Ar {
        val request = objectMapper.treeToValue(requestBody, UpdateArRequest::class.java)
            .copy(dueDateProvided = requestBody.has("dueDate"))
        return arService.updateAr(
            id = id,
            title = request.title,
            description = request.description,
            priority = request.priority,
            status = request.status,
            tags = request.tags,
            assignee = request.assignee,
            dueDate = request.dueDate,
            dueDateProvided = request.dueDateProvided,
            section = request.section,
            creator = request.creator,
        )
    }

    @DELETE
    @Path("/{id}")
    suspend fun delete(@PathParam("id") id: String): Response {
        arService.deleteAr(id)
        return Response.noContent().build()
    }
}
