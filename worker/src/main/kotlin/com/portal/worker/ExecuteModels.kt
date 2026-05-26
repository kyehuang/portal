package com.portal.worker

data class ExecuteRequest(
    val task: String = "",
    val params: Map<String, String> = emptyMap(),
    val code: String = "",
    val state: Map<String, String> = emptyMap(),
)

data class ExecuteResponse(
    val task: String = "",
    val result: Map<String, Any?>? = null,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null,
)
