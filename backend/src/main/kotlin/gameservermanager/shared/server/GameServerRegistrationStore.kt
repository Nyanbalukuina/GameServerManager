package gameservermanager.shared.server

import gameservermanager.shared.server.GameServerRegistration

interface GameServerRegistrationStore {
    fun findAll(): List<GameServerRegistration>

    fun findByGame(game: String): GameServerRegistration?

    fun create(registration: GameServerRegistration)

    fun update(registration: GameServerRegistration)

    fun delete(game: String)
}
