package gameservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import gameservermanager.configuration.StorageProperties

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
class GameServerManagerApplication

fun main(args: Array<String>) {
    runApplication<GameServerManagerApplication>(*args)
}
