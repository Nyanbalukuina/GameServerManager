package gameservermanager.application.preflight

interface ManagedPathPolicy {
    fun isServerPathAllowed(path: String): Boolean

    fun isToolPathAllowed(path: String): Boolean

    fun managedRoot(): String
}
