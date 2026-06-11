package com.portal.portal.user

import com.github.f4b6a3.uuid.UuidCreator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response
import java.sql.SQLIntegrityConstraintViolationException

@ApplicationScoped
class UserService(private val userRepo: UserRepo) {
    suspend fun list(): List<User> = userRepo.list()

    suspend fun get(id: String): User = userRepo.findById(id)
        ?: throw NotFoundException("user not found")

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
            throw NotFoundException("user not found")
        }

        return get(id)
    }

    suspend fun addSection(id: String, request: AddUserSectionRequest) {
        get(id)
        val sectionId = request.sectionId.trim()
        if (sectionId.isEmpty()) {
            throw BadRequestException("sectionId is required")
        }

        userRepo.addSection(id, sectionId)
    }

    suspend fun delete(id: String) {
        val deleted = userRepo.delete(id)
        if (deleted == 0) {
            throw NotFoundException("user not found")
        }
    }

    private fun validate(name: String, email: String) {
        if (name.isBlank()) {
            throw BadRequestException("name is required")
        }

        if (email.isBlank()) {
            throw BadRequestException("email is required")
        }
    }

    private fun mapDatabaseException(exception: RuntimeException): RuntimeException {
        val duplicateEmail =
            generateSequence<Throwable>(exception) { it.cause }
                .any { it is SQLIntegrityConstraintViolationException }

        if (duplicateEmail) {
            return ClientErrorException("email already exists", Response.Status.CONFLICT)
        }

        return InternalServerErrorException("database error", exception)
    }
}
