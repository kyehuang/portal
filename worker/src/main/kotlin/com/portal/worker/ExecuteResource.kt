package com.portal.worker

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/execute")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ExecuteResource(
    @param:RestClient private val pythonSidecarClient: PythonSidecarClient,
) {
    @POST
    fun execute(request: ExecuteRequest): ExecuteResponse {
        if (request.task.isBlank() && request.code.isBlank()) {
            throw WebApplicationException("task or code is required", Response.Status.BAD_REQUEST)
        }

        return pythonSidecarClient.execute(
            ExecuteRequest(code = "import numpy as np\\nprint(np.loadtxt(\\\"numbers.csv\\\", delimiter=\\\",\\\").mean())\""),
        )
    }
}
