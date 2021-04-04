package io.waveplatform.blockchain.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import java.math.BigDecimal

data class PoolUser(
    val amount : BigDecimal,
    val address : String,
    val pool : String,
)

class PoolUserRecord(id : EntityID<Long>) : Entity<Long>(id){
    companion object : EntityClass<Long,PoolUserRecord>(PoolUsersRepository)
    val pool by PoolUsersRepository.pool
    val amount by PoolUsersRepository.amount
    val validator by PoolUsersRepository.validator

    fun convert() : PoolUser {
        return PoolUser(
            amount,
            validator,
            pool
        )
    }

    override fun toString(): String = with(StringBuilder()){
        append("PoolUserRecord(")
        append("pool=${amount}")
        append("amount=${amount}")
        append("validator=${validator}")
        append(")")
    }.toString()
}

object PoolUsersRepository : LongIdTable("pool_users_record") {
    val amount = decimal("stake_amount",8,8).index()
    val pool = varchar("pubKey",855)
    val validator = varchar("validator",855)
}