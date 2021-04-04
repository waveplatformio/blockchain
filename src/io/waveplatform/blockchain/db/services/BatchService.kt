package io.waveplatform.blockchain.db.services

import io.waveplatform.blockchain.db.BlockRecordRepository
import io.waveplatform.blockchain.db.TransactionRecordRepository
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


object Batch {

    fun reInit() = transaction {
        BlockRecordRepository.deleteAll()
        TransactionRecordRepository.deleteAll()
    }
}
