package com.portal.portal.ar

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.LocalDateTime

@RegisterKotlinMapper(ArRow::class)
interface ArDao {
    @SqlQuery(
        """
        SELECT a.id,
               a.title,
               a.description,
               a.section,
               a.priority,
               a.status,
               a.creator,
               a.assignee,
               a.created_at AS createdAt,
               a.updated_at AS updatedAt,
               a.due_date AS dueDate
        FROM ars a
        LEFT JOIN ar_tags t ON t.ar_id = a.id
        WHERE (:normalizedTag IS NULL OR t.normalized_tag = :normalizedTag)
          AND (:priority IS NULL OR a.priority = :priority)
          AND (:status IS NULL OR a.status = :status)
          AND (:assignee IS NULL OR a.assignee = :assignee)
        GROUP BY a.id,
                 a.title,
                 a.description,
                 a.section,
                 a.priority,
                 a.status,
                 a.creator,
                 a.assignee,
                 a.created_at,
                 a.updated_at,
                 a.due_date
        ORDER BY
            CASE a.priority
                WHEN 'p0' THEN 0
                WHEN 'p1' THEN 1
                WHEN 'p2' THEN 2
                ELSE 3
            END,
            a.due_date IS NULL,
            a.due_date,
            a.created_at DESC
        """,
    )
    fun list(
        @Bind("normalizedTag") normalizedTag: String?,
        @Bind("priority") priority: String?,
        @Bind("status") status: String?,
        @Bind("assignee") assignee: String?,
    ): List<ArRow>

    @SqlQuery(
        """
        SELECT id,
               title,
               description,
               section,
               priority,
               status,
               creator,
               assignee,
               created_at AS createdAt,
               updated_at AS updatedAt,
               due_date AS dueDate
        FROM ars
        WHERE id = :id
        """,
    )
    fun findById(@Bind("id") id: String): ArRow?

    @SqlUpdate(
        """
        INSERT INTO ars (
            id, title, description, section, priority, status, creator, assignee, due_date
        )
        VALUES (
            :id, :title, :description, :section, :priority, :status, :creator, :assignee, :dueDate
        )
        """,
    )
    fun insert(
        @Bind("id") id: String,
        @Bind("title") title: String,
        @Bind("description") description: String,
        @Bind("section") section: String,
        @Bind("priority") priority: String,
        @Bind("status") status: String,
        @Bind("creator") creator: String,
        @Bind("assignee") assignee: String?,
        @Bind("dueDate") dueDate: LocalDateTime?,
    ): Int

    @SqlUpdate(
        """
        UPDATE ars
        SET title = :title,
            description = :description,
            priority = :priority,
            status = :status,
            assignee = :assignee,
            due_date = :dueDate
        WHERE id = :id
        """,
    )
    fun update(
        @Bind("id") id: String,
        @Bind("title") title: String,
        @Bind("description") description: String,
        @Bind("priority") priority: String,
        @Bind("status") status: String,
        @Bind("assignee") assignee: String?,
        @Bind("dueDate") dueDate: LocalDateTime?,
    ): Int

    @SqlUpdate(
        """
        DELETE FROM ars
        WHERE id = :id
        """,
    )
    fun delete(@Bind("id") id: String): Int
}
