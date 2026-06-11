package com.portal.portal.user

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UserController(private val userService: UserService) {

    @GET
    suspend fun list(): List<User> = userService.list()

    @GET
    @Path("/{id}")
    suspend fun get(@PathParam("id") id: String): User = userService.get(id)

    @POST
    suspend fun create(request: CreateUserRequest): Response {
        val user = userService.create(request)
        return Response.status(Response.Status.CREATED).entity(user).build()
    }

    @PUT
    @Path("/{id}")
    suspend fun update(@PathParam("id") id: String, request: UpdateUserRequest): User = userService.update(id, request)

    @POST
    @Path("/{id}/sections")
    suspend fun addSection(@PathParam("id") id: String, request: AddUserSectionRequest): Response {
        userService.addSection(id, request)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/{id}")
    suspend fun delete(@PathParam("id") id: String): Response {
        userService.delete(id)
        return Response.noContent().build()
    }
}
