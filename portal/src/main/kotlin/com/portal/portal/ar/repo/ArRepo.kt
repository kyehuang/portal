package com.portal.portal.ar.repo

import com.portal.portal.ar.dao.ArDao
import com.portal.portal.ar.dao.ArTagDao
import com.portal.portal.ar.dao.SubArDao
import com.portal.portal.ar.dao.SubArTagDao
import com.portal.portal.ar.model.Ar
import com.portal.portal.ar.model.ArRow
import com.portal.portal.ar.model.SubAr
import com.portal.portal.ar.model.SubArRow
import com.portal.portal.global.io
import jakarta.enterprise.context.ApplicationScoped
import org.jdbi.v3.core.Jdbi
import java.time.LocalDateTime

@ApplicationScoped
class ArRepo(private val jdbi: Jdbi) {
    private val arDao: ArDao = jdbi.onDemand(ArDao::class.java)
    private val arTagDao: ArTagDao = jdbi.onDemand(ArTagDao::class.java)
    private val subArDao: SubArDao = jdbi.onDemand(SubArDao::class.java)
    private val subArTagDao: SubArTagDao = jdbi.onDemand(SubArTagDao::class.java)

    suspend fun listArs(normalizedTag: String?, priority: String?, status: String?, assignee: String?): List<Ar> =
        jdbi.io { toArs(arDao.list(normalizedTag, priority, status, assignee)) }

    suspend fun findArById(id: String): Ar? = jdbi.io {
        arDao.findById(id)?.let { row ->
            toAr(row, arTagDao.listByArIds(listOf(row.id)).map { it.tag })
        }
    }

    suspend fun listSubArs(normalizedTag: String?, priority: String?, status: String?, assignee: String?): List<SubAr> =
        jdbi.io { toSubArs(subArDao.list(normalizedTag, priority, status, assignee)) }

    suspend fun findSubArById(id: String): SubAr? = jdbi.io {
        subArDao.findById(id)?.let { row ->
            toSubAr(row, subArTagDao.listBySubArIds(listOf(row.id)).map { it.tag })
        }
    }

    suspend fun listSubArsByParentArId(parentArId: String): List<SubAr> =
        jdbi.io { toSubArs(subArDao.listByParentArId(parentArId)) }

    suspend fun insertAr(
        id: String,
        title: String,
        description: String,
        section: String,
        priority: String,
        status: String,
        tags: List<String>,
        creator: String,
        assignee: String?,
        dueDate: LocalDateTime?,
    ): Int = jdbi.io {
        jdbi.inTransaction<Int, RuntimeException> { handle ->
            val arDao = handle.attach(ArDao::class.java)
            val arTagDao = handle.attach(ArTagDao::class.java)
            val inserted = arDao.insert(
                id,
                title,
                description,
                section,
                priority,
                status,
                creator,
                assignee,
                dueDate,
            )
            insertArTags(arTagDao, id, tags)
            inserted
        }
    }

    suspend fun updateAr(ar: Ar): Int = jdbi.io {
        jdbi.inTransaction<Int, RuntimeException> { handle ->
            val arDao = handle.attach(ArDao::class.java)
            val arTagDao = handle.attach(ArTagDao::class.java)
            val updated = arDao.update(
                ar.id,
                ar.title,
                ar.description,
                ar.priority,
                ar.status,
                ar.assignee,
                ar.dueDate,
            )
            arTagDao.deleteByArId(ar.id)
            insertArTags(arTagDao, ar.id, ar.tags)
            updated
        }
    }

    suspend fun deleteAr(id: String): Int = jdbi.io { arDao.delete(id) }

    suspend fun insertSubAr(
        id: String,
        parentArId: String,
        title: String,
        description: String,
        section: String,
        priority: String,
        status: String,
        tags: List<String>,
        creator: String,
        assignee: String?,
        dueDate: LocalDateTime?,
    ): Int = jdbi.io {
        jdbi.inTransaction<Int, RuntimeException> { handle ->
            val subArDao = handle.attach(SubArDao::class.java)
            val subArTagDao = handle.attach(SubArTagDao::class.java)
            val inserted = subArDao.insert(
                id,
                parentArId,
                title,
                description,
                section,
                priority,
                status,
                creator,
                assignee,
                dueDate,
            )
            insertSubArTags(subArTagDao, id, tags)
            inserted
        }
    }

    suspend fun updateSubAr(subAr: SubAr): Int = jdbi.io {
        jdbi.inTransaction<Int, RuntimeException> { handle ->
            val subArDao = handle.attach(SubArDao::class.java)
            val subArTagDao = handle.attach(SubArTagDao::class.java)
            val updated = subArDao.update(
                subAr.id,
                subAr.title,
                subAr.description,
                subAr.priority,
                subAr.status,
                subAr.assignee,
                subAr.dueDate,
            )
            subArTagDao.deleteBySubArId(subAr.id)
            insertSubArTags(subArTagDao, subAr.id, subAr.tags)
            updated
        }
    }

    suspend fun deleteSubAr(id: String): Int = jdbi.io { subArDao.delete(id) }

    private fun toArs(rows: List<ArRow>): List<Ar> {
        if (rows.isEmpty()) {
            return emptyList()
        }

        val tagsByArId = arTagDao
            .listByArIds(rows.map { it.id })
            .groupBy({ it.arId }, { it.tag })

        return rows.map { row -> toAr(row, tagsByArId[row.id].orEmpty()) }
    }

    private fun toSubArs(rows: List<SubArRow>): List<SubAr> {
        if (rows.isEmpty()) {
            return emptyList()
        }

        val tagsBySubArId = subArTagDao
            .listBySubArIds(rows.map { it.id })
            .groupBy({ it.subArId }, { it.tag })

        return rows.map { row -> toSubAr(row, tagsBySubArId[row.id].orEmpty()) }
    }

    private fun toAr(row: ArRow, tags: List<String>): Ar = Ar(
        id = row.id,
        title = row.title,
        description = row.description,
        section = row.section,
        priority = row.priority,
        status = row.status,
        tags = tags,
        creator = row.creator,
        assignee = row.assignee,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        dueDate = row.dueDate,
    )

    private fun toSubAr(row: SubArRow, tags: List<String>): SubAr = SubAr(
        id = row.id,
        parentArId = row.parentArId,
        title = row.title,
        description = row.description,
        section = row.section,
        priority = row.priority,
        status = row.status,
        tags = tags,
        creator = row.creator,
        assignee = row.assignee,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        dueDate = row.dueDate,
    )

    private fun insertArTags(arTagDao: ArTagDao, arId: String, tags: List<String>) {
        tags.forEach { tag -> arTagDao.insert(arId, tag, tag.lowercase()) }
    }

    private fun insertSubArTags(subArTagDao: SubArTagDao, subArId: String, tags: List<String>) {
        tags.forEach { tag -> subArTagDao.insert(subArId, tag, tag.lowercase()) }
    }
}
