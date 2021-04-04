package io.waveplatform.server.handlers.p2p.messages

import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.CurrentChain
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.server.handlers.p2p.MessageTypes

data class Message (
    val type : MessageTypes?,
    val block : Block?,
    val transaction: Transaction?,
    val transactions : MutableList<Transaction>?,
    val height : CurrentChain?
) : Bundle