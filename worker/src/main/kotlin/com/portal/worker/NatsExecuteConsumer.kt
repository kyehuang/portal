package com.portal.worker

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
class NatsExecuteConsumer(
    private val natsConnectionProvider: NatsConnectionProvider,
    private val objectMapper: ObjectMapper,
    @param:RestClient private val pythonSidecarClient: PythonSidecarClient,
    @param:ConfigProperty(name = "worker.execute.subject") private val executeSubject: String,
    @param:ConfigProperty(name = "worker.execute.queue") private val executeQueue: String,
) {
    fun onStart(
        @Observes event: StartupEvent,
    ) {
        val dispatcher =
            natsConnectionProvider.connection.createDispatcher { message ->
                val response =
                    runCatching {
                        val request = objectMapper.readValue(message.data, ExecuteRequest::class.java)
                        pythonSidecarClient.execute(request)
                    }.getOrElse { error ->
                        ExecuteResponse(error = error.message ?: error.javaClass.simpleName, exitCode = 1)
                    }

                natsConnectionProvider.connection.publish(message.replyTo, objectMapper.writeValueAsBytes(response))
            }

        dispatcher.subscribe(executeSubject, executeQueue)
    }
}
