package gameservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import gameservermanager.configuration.FeatureProperties
import gameservermanager.configuration.StorageProperties
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class, FeatureProperties::class)
@EnableScheduling
class GameServerManagerApplication

fun main(args: Array<String>) {
    runApplication<GameServerManagerApplication>(*args)
}
