package com.portal.portal.user

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

@RegisterKotlinMapper(User::class)
interface UserDao {
    @SqlQuery(
        """
        SELECT id, name, email, created_at AS createdAt, updated_at AS updatedAt
        FROM users
        ORDER BY id
        """,
    )
    fun list(): List<User>

    @SqlQuery(
        """
        SELECT id, name, email, created_at AS createdAt, updated_at AS updatedAt
        FROM users
        WHERE id = :id
        """,
    )
    fun findById(@Bind("id") id: String): User?

    @SqlUpdate(
        """
        INSERT INTO users (id, name, email)
        VALUES (:id, :name, :email)
        """,
    )
    fun insert(@Bind("id") id: String, @Bind("name") name: String, @Bind("email") email: String): Int

    @SqlUpdate(
        """
        UPDATE users
        SET name = :name,
            email = :email
        WHERE id = :id
        """,
    )
    fun update(@Bind("id") id: String, @Bind("name") name: String, @Bind("email") email: String): Int

    @SqlUpdate(
        """
        DELETE FROM users
        WHERE id = :id
        """,
    )
    fun delete(@Bind("id") id: String): Int
}
