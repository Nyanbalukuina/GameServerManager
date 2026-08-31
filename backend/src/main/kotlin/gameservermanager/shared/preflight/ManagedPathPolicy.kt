package gameservermanager.shared.preflight

interface ManagedPathPolicy {
    fun isServerPathAllowed(path: String): Boolean

    fun isToolPathAllowed(path: String): Boolean

    fun managedRoot(): String
}
