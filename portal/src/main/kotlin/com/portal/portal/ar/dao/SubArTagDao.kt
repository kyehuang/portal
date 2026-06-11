package com.portal.portal.ar.dao

import com.portal.portal.ar.model.SubArTagRow
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

@RegisterKotlinMapper(SubArTagRow::class)
interface SubArTagDao {
    @SqlQuery(
        """
        SELECT sub_ar_id AS subArId, tag
        FROM sub_ar_tags
        WHERE sub_ar_id IN (<subArIds>)
        ORDER BY id
        """,
    )
    fun listBySubArIds(@BindList("subArIds") subArIds: List<String>): List<SubArTagRow>

    @SqlUpdate(
        """
        INSERT INTO sub_ar_tags (sub_ar_id, tag, normalized_tag)
        VALUES (:subArId, :tag, :normalizedTag)
        """,
    )
    fun insert(
        @Bind("subArId") subArId: String,
        @Bind("tag") tag: String,
        @Bind("normalizedTag") normalizedTag: String,
    ): Int

    @SqlUpdate(
        """
        DELETE FROM sub_ar_tags
        WHERE sub_ar_id = :subArId
        """,
    )
    fun deleteBySubArId(@Bind("subArId") subArId: String): Int
}
