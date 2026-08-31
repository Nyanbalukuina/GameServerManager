package gameservermanager.shared.construction

import gameservermanager.shared.server.GamePortAccess
import org.springframework.stereotype.Service

@Service
class CreateGamePortAccess {
    fun execute(command: Command): GamePortAccess {
        if (command.allowAny) {
            return GamePortAccess(localSubnet = false, tailscale = false, allowAny = true)
        }

        val customAddresses = command.customRemoteAddresses
            .split(',', '\n', '\r')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        customAddresses.forEach { address ->
            require(isIpv4AddressOrCidr(address)) {
                "接続元の手動指定にはIPv4アドレスまたはCIDRを入力してください: $address"
            }
        }
        require(command.localSubnet || command.tailscale || customAddresses.isNotEmpty()) {
            "ゲームポートを許可する接続範囲を1つ以上選択してください"
        }

        return GamePortAccess(
            localSubnet = command.localSubnet,
            tailscale = command.tailscale,
            customRemoteAddresses = customAddresses,
        )
    }

    private fun isIpv4AddressOrCidr(value: String): Boolean {
        val parts = value.split('/', limit = 2)
        if (!isIpv4Address(parts[0])) return false
        if (parts.size == 1) return true
        return parts[1].toIntOrNull() in 0..32
    }

    private fun isIpv4Address(value: String): Boolean {
        val octets = value.split('.')
        return octets.size == 4 && octets.all { octet ->
            octet.isNotEmpty() && octet.all(Char::isDigit) && octet.toIntOrNull() in 0..255
        }
    }

    data class Command(
        val localSubnet: Boolean,
        val tailscale: Boolean,
        val customRemoteAddresses: String,
        val allowAny: Boolean,
    )
}
