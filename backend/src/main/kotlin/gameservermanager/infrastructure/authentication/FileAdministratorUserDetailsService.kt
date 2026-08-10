package gameservermanager.infrastructure.authentication

import gameservermanager.application.authentication.AdministratorCredentialStore
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

@Component
class FileAdministratorUserDetailsService(
    private val credentialStore: AdministratorCredentialStore,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val credentials = credentialStore.load()
            ?: throw UsernameNotFoundException("管理者が設定されていません")
        if (username != credentials.username) {
            throw UsernameNotFoundException("ユーザーが見つかりません")
        }

        return User.withUsername(credentials.username)
            .password(credentials.passwordHash)
            .roles("ADMIN")
            .build()
    }
}
