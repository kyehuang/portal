package com.portal.portal.ar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.ResponseStatus

@Path("/sub-ars")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class SubArController(private val arService: ArService, private val objectMapper: ObjectMapper) {
    @GET
    suspend fun list(
        @QueryParam("tag") tag: String?,
        @QueryParam("priority") priority: String?,
        @QueryParam("status") status: String?,
        @QueryParam("assignee") assignee: String?,
    ): List<SubAr> = arService.listSubArs(tag, priority, status, assignee)

    @GET
    @Path("/{id}")
    suspend fun get(@PathParam("id") id: String): SubAr = arService.getSubAr(id)

    @POST
    @ResponseStatus(201)
    suspend fun create(
        request: CreateSubArRequest,
        @HeaderParam("X-User-Id") userId: String?,
        @HeaderParam("X-Section-Id") sectionId: String?,
    ): SubAr = arService.createSubAr(
        parentArId = request.parentArId,
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
    suspend fun update(@PathParam("id") id: String, requestBody: JsonNode): SubAr {
        val request = objectMapper.treeToValue(requestBody, UpdateSubArRequest::class.java)
            .copy(dueDateProvided = requestBody.has("dueDate"))
        return arService.updateSubAr(
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
            parentArId = request.parentArId,
        )
    }

    @DELETE
    @Path("/{id}")
    suspend fun delete(@PathParam("id") id: String): Response {
        arService.deleteSubAr(id)
        return Response.noContent().build()
    }
}
