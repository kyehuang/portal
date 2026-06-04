package com.portal.portal.global.db

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import javax.sql.DataSource

@ApplicationScoped
class JdbiConfig(private val dataSource: DataSource) {
    @Produces
    @ApplicationScoped
    fun jdbi(): Jdbi = Jdbi
        .create(dataSource)
        .installPlugin(SqlObjectPlugin())
        .installPlugin(KotlinPlugin())
}
