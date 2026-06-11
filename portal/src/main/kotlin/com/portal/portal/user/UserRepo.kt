package com.portal.portal.user

import com.portal.portal.global.io
import jakarta.enterprise.context.ApplicationScoped
import org.jdbi.v3.core.Jdbi

@ApplicationScoped
class UserRepo(private val jdbi: Jdbi) {
    private val dao: UserDao = jdbi.onDemand(UserDao::class.java)

    suspend fun list(): List<User> = jdbi.io { dao.list() }
    suspend fun findById(id: String): User? = jdbi.io { dao.findById(id) }
    suspend fun isMemberOfSection(userId: String, sectionId: String): Boolean =
        jdbi.io { dao.isMemberOfSection(userId, sectionId) }

    suspend fun addSection(userId: String, sectionId: String): Int = jdbi.io { dao.addSection(userId, sectionId) }
    suspend fun insert(id: String, name: String, email: String): Int = jdbi.io { dao.insert(id, name, email) }
    suspend fun update(id: String, name: String, email: String): Int = jdbi.io { dao.update(id, name, email) }
    suspend fun delete(id: String): Int = jdbi.io { dao.delete(id) }
}
