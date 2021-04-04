package io.waveplatform.server.sessions

import io.ktor.http.cio.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Peer(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val name = "user${lastId.getAndIncrement()}"

}