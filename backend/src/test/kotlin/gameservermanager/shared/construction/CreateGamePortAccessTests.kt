package gameservermanager.shared.construction

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class CreateGamePortAccessTests {
    private val useCase = CreateGamePortAccess()

    @Test
    fun `LANとTailscaleと手動指定をFirewall接続元へ変換する`() {
        val access = useCase.execute(
            CreateGamePortAccess.Command(
                localSubnet = true,
                tailscale = true,
                customRemoteAddresses = "10.8.0.0/24\n100.80.0.20",
                allowAny = false,
            ),
        )

        assertThat(access.remoteAddresses()).containsExactly(
            "LocalSubnet",
            "100.64.0.0/10",
            "10.8.0.0/24",
            "100.80.0.20",
        )
    }

    @Test
    fun `Anyの場合はほかの接続範囲を無視する`() {
        val access = useCase.execute(
            CreateGamePortAccess.Command(true, true, "10.8.0.0/24", true),
        )

        assertThat(access.remoteAddresses()).containsExactly("Any")
    }

    @Test
    fun `不正な接続元を拒否する`() {
        assertThatIllegalArgumentException().isThrownBy {
            useCase.execute(CreateGamePortAccess.Command(false, false, "vpn.example.com", false))
        }
    }
}
