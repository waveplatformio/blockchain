package io.waveplatform.blockchain.db

import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.wallet.Transaction
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import java.lang.StringBuilder



class BlockRecord(id : EntityID<Long>) : Entity<Long>(id){
    companion object : EntityClass<Long,BlockRecord>(BlockRecordRepository)

    val type by BlockRecordRepository.type
    val previousHash by BlockRecordRepository.previousHash
    val timestamp by BlockRecordRepository.timestamp
    val validator by BlockRecordRepository.validator
    val signature by BlockRecordRepository.signature
    val hash by BlockRecordRepository.hash
    val finished by BlockRecordRepository.finished
    val confirmations by BlockRecordRepository.confirmations

    fun convert(transactions : MutableList<Transaction>): Block{
        val block = Block(
            id.value,
            previousHash,
            transactions,
            timestamp,
            validator = validator,
            hash,
            null,
            finished,
            confirmations
        )

        //if signature exists it means block is finished and signature is not null
        signature?.let {
            block.attachSignature(it)
        }
        return block
    }

    override fun toString(): String  = with(StringBuilder()){
        append("BlockRecord(")
        append("type=${type}")
        append("previousHash=${previousHash}")
        append("timestamp=${timestamp}")
        append("validator=${validator}")
        append("signature=${signature}")
        append("hash=${hash}")
        append("finished=${finished}")
        append("confirmations=${confirmations}")
        append(")")
    }.toString()


}

object BlockRecordRepository : LongIdTable("block_record") {
    val type = varchar("type",133).default("BLOCK")
    val previousHash = varchar("previous_hash",855)
    val timestamp = long("timestamp")
    val validator = varchar("validator",855).nullable()
    val signature = varchar("signature",855).nullable()
    val hash = varchar("hash",855)
    val finished = bool("is_finished").default(false)
    val confirmations = integer("confirmations").default(0)
}