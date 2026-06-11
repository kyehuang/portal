package com.portal.portal.ar.dao

import com.portal.portal.ar.model.SubArRow
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.LocalDateTime

@RegisterKotlinMapper(SubArRow::class)
interface SubArDao {
    @SqlQuery(
        """
        SELECT s.id,
               s.parent_ar_id AS parentArId,
               s.title,
               s.description,
               s.section,
               s.priority,
               s.status,
               s.creator,
               s.assignee,
               s.created_at AS createdAt,
               s.updated_at AS updatedAt,
               s.due_date AS dueDate
        FROM sub_ars s
        LEFT JOIN sub_ar_tags t ON t.sub_ar_id = s.id
        WHERE (:normalizedTag IS NULL OR t.normalized_tag = :normalizedTag)
          AND (:priority IS NULL OR s.priority = :priority)
          AND (:status IS NULL OR s.status = :status)
          AND (:assignee IS NULL OR s.assignee = :assignee)
        GROUP BY s.id,
                 s.parent_ar_id,
                 s.title,
                 s.description,
                 s.section,
                 s.priority,
                 s.status,
                 s.creator,
                 s.assignee,
                 s.created_at,
                 s.updated_at,
                 s.due_date
        ORDER BY
            CASE s.priority
                WHEN 'p0' THEN 0
                WHEN 'p1' THEN 1
                WHEN 'p2' THEN 2
                ELSE 3
            END,
            s.due_date IS NULL,
            s.due_date,
            s.created_at DESC
        """,
    )
    fun list(
        @Bind("normalizedTag") normalizedTag: String?,
        @Bind("priority") priority: String?,
        @Bind("status") status: String?,
        @Bind("assignee") assignee: String?,
    ): List<SubArRow>

    @SqlQuery(
        """
        SELECT id,
               parent_ar_id AS parentArId,
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
        FROM sub_ars
        WHERE id = :id
        """,
    )
    fun findById(@Bind("id") id: String): SubArRow?

    @SqlQuery(
        """
        SELECT id,
               parent_ar_id AS parentArId,
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
        FROM sub_ars
        WHERE parent_ar_id = :parentArId
        """,
    )
    fun listByParentArId(@Bind("parentArId") parentArId: String): List<SubArRow>

    @SqlUpdate(
        """
        INSERT INTO sub_ars (
            id, parent_ar_id, title, description, section, priority, status, creator, assignee, due_date
        )
        VALUES (
            :id, :parentArId, :title, :description, :section, :priority, :status, :creator, :assignee, :dueDate
        )
        """,
    )
    fun insert(
        @Bind("id") id: String,
        @Bind("parentArId") parentArId: String,
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
        UPDATE sub_ars
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
        DELETE FROM sub_ars
        WHERE id = :id
        """,
    )
    fun delete(@Bind("id") id: String): Int
}
