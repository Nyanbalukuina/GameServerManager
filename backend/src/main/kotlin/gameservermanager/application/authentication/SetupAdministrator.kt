package gameservermanager.application.authentication

import gameservermanager.domain.authentication.AdministratorCredentials
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.time.Clock

@Service
class SetupAdministrator(
    private val credentialStore: AdministratorCredentialStore,
    private val passwordEncoder: PasswordEncoder,
) {
    fun execute(command: Command) {
        require(isLoopback(command.remoteAddress)) {
            "初回管理者設定はサーバーPC上で実行してください"
        }
        require(!credentialStore.exists()) {
            "管理者はすでに設定されています"
        }
        require(command.password.length in 12..128) {
            "パスワードは12文字以上128文字以内で入力してください"
        }
        require(command.password == command.passwordConfirmation) {
            "確認用パスワードが一致しません"
        }

        credentialStore.create(
            AdministratorCredentials(
                passwordHash = passwordEncoder.encode(command.password),
                createdAt = Clock.systemUTC().instant(),
            ),
        )
    }

    private fun isLoopback(address: String): Boolean {
        return runCatching { InetAddress.getByName(address).isLoopbackAddress }.getOrDefault(false)
    }

    data class Command(
        val password: String,
        val passwordConfirmation: String,
        val remoteAddress: String,
    )
}
