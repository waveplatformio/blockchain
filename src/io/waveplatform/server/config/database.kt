package io.waveplatform.server.config

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import org.jetbrains.exposed.sql.Database

val appConfig =  HoconApplicationConfig(ConfigFactory.load())

fun configureDatabase() = with(Database) {

    connect(
        url =  appConfig.property("ktor.db.jdbcUrl").getString(),
        driver = appConfig.property("ktor.db.driver").getString(),
        user = appConfig.property("ktor.db.user").getString(),
        password = appConfig.property("ktor.db.password").getString()
    )
}
