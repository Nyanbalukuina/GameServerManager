package gameservermanager.application.authentication

import gameservermanager.domain.authentication.AdministratorCredentials

interface AdministratorCredentialStore {
    fun exists(): Boolean

    fun load(): AdministratorCredentials?

    fun create(credentials: AdministratorCredentials)
}
