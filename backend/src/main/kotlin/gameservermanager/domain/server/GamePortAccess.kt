package gameservermanager.domain.server

data class GamePortAccess(
    val localSubnet: Boolean = true,
    val tailscale: Boolean = true,
    val customRemoteAddresses: List<String> = emptyList(),
    val allowAny: Boolean = false,
) {
    fun remoteAddresses(): List<String> {
        if (allowAny) {
            return listOf("Any")
        }
        return buildList {
            if (localSubnet) add("LocalSubnet")
            if (tailscale) add(TAILSCALE_CIDR)
            addAll(customRemoteAddresses)
        }
    }

    companion object {
        const val TAILSCALE_CIDR = "100.64.0.0/10"
    }
}
