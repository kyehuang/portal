package com.portal.portal

data class ExecuteRequest(
    val script: String = "",
    val content: Map<String, Any?> = emptyMap(),
)

data class ExecuteResponse(
    val result: Map<String, Any?>? = null,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null,
)
