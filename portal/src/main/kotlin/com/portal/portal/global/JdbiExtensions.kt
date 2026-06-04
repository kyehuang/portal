package com.portal.portal.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Jdbi

suspend fun <T> Jdbi.io(block: () -> T): T =
    withContext(Dispatchers.IO) {
        block()
    }
