package io.waveplatform.blockchain.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import java.math.BigDecimal

data class StakeDTO(
    val staker : String,
    val amount : BigDecimal,
)

class StakeRecord(id : EntityID<Long>) : Entity<Long>(id){
    companion object : EntityClass<Long,StakeRecord>(StakeRecordRepository)
    val staker by StakeRecordRepository.staker
    val amount by StakeRecordRepository.amount

    fun convert() : StakeDTO {
        return StakeDTO(
            staker,
            amount,
        )
    }

    override fun toString(): String = with(StringBuilder()){
        append("StakerRecord(")
        append("staker=${staker}")
        append("amount=${amount}")
        append(")")
    }.toString()
}

object StakeRecordRepository : LongIdTable("staker_record") {
    val staker = varchar("staker_address", 855).index()
    val amount = decimal("stake_amount",8,8).index()
}