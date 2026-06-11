package com.portal.portal

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration

@Path("/execute")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ExecuteResource(
    private val natsConnectionProvider: NatsConnectionProvider,
    private val objectMapper: ObjectMapper,
    @param:ConfigProperty(name = "portal.execute.subject") private val executeSubject: String,
    @param:ConfigProperty(name = "portal.execute.timeout") private val executeTimeout: Duration,
) {
    @POST
    fun execute(request: ExecuteRequest): ExecuteResponse {
        if (request.script.isBlank()) {
            throw BadRequestException("script is required")
        }

        val response =
            natsConnectionProvider.connection.request(
                executeSubject,
                objectMapper.writeValueAsBytes(request),
                executeTimeout,
            ) ?: throw ServerErrorException("worker timed out", Response.Status.GATEWAY_TIMEOUT)

        return objectMapper.readValue(response.data, ExecuteResponse::class.java)
    }
}
