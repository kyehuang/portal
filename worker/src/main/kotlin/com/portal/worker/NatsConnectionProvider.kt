package com.portal.worker

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class NatsConnectionProvider(
    @param:ConfigProperty(name = "worker.nats.url") private val natsUrl: String,
) {
    val connection: Connection by lazy {
        Nats.connect(
            Options
                .Builder()
                .server(natsUrl)
                .connectionName("worker")
                .maxReconnects(-1)
                .build(),
        )
    }

    @PreDestroy
    fun close() {
        if (connection.status != Connection.Status.CLOSED) {
            connection.close()
        }
    }
}
