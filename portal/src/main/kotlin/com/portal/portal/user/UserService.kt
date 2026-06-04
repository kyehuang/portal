package com.portal.portal.user

import com.github.f4b6a3.uuid.UuidCreator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.sql.SQLIntegrityConstraintViolationException

@ApplicationScoped
class UserService(private val userRepo: UserRepo) {
    suspend fun list(): List<User> = userRepo.list()

    suspend fun get(id: String): User = userRepo.findById(id)
        ?: throw WebApplicationException("user not found", Response.Status.NOT_FOUND)

    suspend fun create(request: CreateUserRequest): User {
        validate(request.name, request.email)

        val id = UuidCreator.getTimeOrderedEpoch().toString()

        try {
            userRepo.insert(id, request.name.trim(), request.email.trim())
        } catch (exception: RuntimeException) {
            throw mapDatabaseException(exception)
        }

        return get(id)
    }

    suspend fun update(id: String, request: UpdateUserRequest): User {
        validate(request.name, request.email)

        val updated =
            try {
                userRepo.update(id, request.name.trim(), request.email.trim())
            } catch (exception: RuntimeException) {
                throw mapDatabaseException(exception)
            }

        if (updated == 0) {
            throw WebApplicationException("user not found", Response.Status.NOT_FOUND)
        }

        return get(id)
    }

    suspend fun delete(id: String) {
        val deleted = userRepo.delete(id)
        if (deleted == 0) {
            throw WebApplicationException("user not found", Response.Status.NOT_FOUND)
        }
    }

    private fun validate(name: String, email: String) {
        if (name.isBlank()) {
            throw WebApplicationException("name is required", Response.Status.BAD_REQUEST)
        }

        if (email.isBlank()) {
            throw WebApplicationException("email is required", Response.Status.BAD_REQUEST)
        }
    }

    private fun mapDatabaseException(exception: RuntimeException): WebApplicationException {
        val duplicateEmail =
            generateSequence<Throwable>(exception) { it.cause }
                .any { it is SQLIntegrityConstraintViolationException }

        if (duplicateEmail) {
            return WebApplicationException("email already exists", Response.Status.CONFLICT)
        }

        return WebApplicationException(
            "database error",
            exception,
            Response.Status.INTERNAL_SERVER_ERROR,
        )
    }
}
