package io.waveplatform.blockchain.db

import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionInput
import io.waveplatform.blockchain.wallet.TransactionOutput
import io.waveplatform.blockchain.wallet.TransactionType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable


class TransactionRecord(id : EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long,TransactionRecord>(TransactionRecordRepository)

    val txId by TransactionRecordRepository.txId
    val to by TransactionRecordRepository.to
    val amount by TransactionRecordRepository.amount
    val fee by TransactionRecordRepository.fee
    val signature by TransactionRecordRepository.signature
    val from by TransactionRecordRepository.from
    val timestamp by TransactionRecordRepository.timestamp
    val type by TransactionRecordRepository.type
    val blockId by TransactionRecordRepository.blockId
    val finished by TransactionRecordRepository.finished


    fun convert() : Transaction{
        return Transaction(
            txId = txId,
            id = id.value,
            type = TransactionType(type),
            input = TransactionInput(
                signature = signature,
                from = from,
                timestamp = timestamp
            ),
            output = TransactionOutput(
                to = to,
                amount = amount,
                fee = fee
            )
        )
    }

    override fun toString(): String  = with(StringBuilder()){
        append("BlockRecord(")
        append("txId=${to}")
        append("to=${to}")
        append("amount=${amount}")
        append("fee=${fee}")
        append("signature=${signature}")
        append("from=${from}")
        append("timestamp=${timestamp}")
        append("type=${type}")
        append("blockId=${blockId}")
        append("finished=${finished}")
        append(")")
    }.toString()
}

object TransactionRecordRepository : LongIdTable("transaction_record"){
    val txId = varchar("tx_id",855).index()
    val to = varchar("to",855).index()
    val amount = decimal("amount",8,8)
    val fee = decimal("fee",8,8)
    val signature = varchar("signature",855)
    val from = varchar("from",855).index()
    val timestamp = long("timestamp")
    val type = varchar("type",855)
    val blockId = long("block_id").index()
    val finished = bool("is_finished")
}
