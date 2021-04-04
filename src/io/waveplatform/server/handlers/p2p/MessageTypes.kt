package io.waveplatform.server.handlers.p2p

enum class MessageTypes(s: String) {
    BLOCK("BLOCK"),
    TRANSACTION("TRANSACTION"),
    WALLETTRANSACTIONS("WALLET_TRANSACTIONS"),
    FREEZE("FREEZE"),
    UNFREEZE("UNFREEZE"),
    SYNC("SYNC")
}