package com.portal.portal.ar.dao

import com.portal.portal.ar.model.ArTagRow
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

@RegisterKotlinMapper(ArTagRow::class)
interface ArTagDao {
    @SqlQuery(
        """
        SELECT ar_id AS arId, tag
        FROM ar_tags
        WHERE ar_id IN (<arIds>)
        ORDER BY id
        """,
    )
    fun listByArIds(@BindList("arIds") arIds: List<String>): List<ArTagRow>

    @SqlUpdate(
        """
        INSERT INTO ar_tags (ar_id, tag, normalized_tag)
        VALUES (:arId, :tag, :normalizedTag)
        """,
    )
    fun insert(@Bind("arId") arId: String, @Bind("tag") tag: String, @Bind("normalizedTag") normalizedTag: String): Int

    @SqlUpdate(
        """
        DELETE FROM ar_tags
        WHERE ar_id = :arId
        """,
    )
    fun deleteByArId(@Bind("arId") arId: String): Int
}
