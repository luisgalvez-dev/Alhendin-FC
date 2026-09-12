package com.luis.alhendinfc.cloud

/**
 * Identidad cloud por flavor. No se copia a columnas Room.
 * DEV → alhendin-dev; stable → alhendin.
 */
object CloudConfig {
    val workspaceId: String get() = CloudEnvironment.WORKSPACE_ID
    val dataSchemaVersion: Int get() = CloudEnvironment.DATA_SCHEMA_VERSION

    fun workspacePath(): String = "workspaces/$workspaceId"
    fun membersPath(): String = "${workspacePath()}/members"
    fun memberPath(uid: String): String = "${membersPath()}/$uid"
    fun collection(name: String): String = "${workspacePath()}/$name"
    fun userPath(uid: String): String = "users/$uid"
    fun homeLayoutPath(uid: String): String = "users/$uid/preferences/homeLayout"
}
