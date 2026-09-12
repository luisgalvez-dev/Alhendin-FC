package com.luis.alhendinfc.cloud.auth

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String
)

sealed class AuthSession {
    data object Checking : AuthSession()
    data object LoggedOut : AuthSession()
    data class Unavailable(val message: String) : AuthSession()
    data class SignedIn(val user: AuthUser) : AuthSession()
    data class NonMember(val user: AuthUser) : AuthSession()
    data class Ready(val user: AuthUser, val workspaceId: String) : AuthSession()
}

object AuthErrorMapper {
    fun map(throwable: Throwable): String {
        val code = firebaseCode(throwable)
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "El correo no es válido."
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_LOGIN_CREDENTIALS" -> "Correo o contraseña incorrectos."
            "ERROR_USER_NOT_FOUND" -> "No hay ninguna cuenta con ese correo."
            "ERROR_USER_DISABLED" -> "Esta cuenta está desactivada."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Comprueba Internet."
            "ERROR_USER_TOKEN_EXPIRED" -> "La sesión ha caducado. Vuelve a iniciar sesión."
            else -> "No se pudo iniciar sesión. Inténtalo de nuevo."
        }
    }

    private fun firebaseCode(throwable: Throwable): String? {
        return try {
            val clazz = Class.forName("com.google.firebase.auth.FirebaseAuthException")
            if (!clazz.isInstance(throwable)) return null
            val method = clazz.getMethod("getErrorCode")
            method.invoke(throwable) as? String
        } catch (_: Exception) {
            null
        }
    }
}
