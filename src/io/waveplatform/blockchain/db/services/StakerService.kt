package io.waveplatform.blockchain.db.services

import io.waveplatform.blockchain.TransactionTransferException
import io.waveplatform.blockchain.db.*
import io.waveplatform.blockchain.db.PoolUsersRepository.pool
import io.waveplatform.blockchain.db.util.convertDecimal
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class StakerService(
    val walletService: WalletService
) {

    fun addStake(st: String, amnt: BigDecimal) = transaction {
        walletService.getWallet(st,null)?.let { walletRecord ->
            StakeRecordRepository.insert { stake ->
                stake[staker] = st
                stake[amount] = amnt
            }
            WalletRecordRepository.update({WalletRecordRepository.pubKey eq st}){
                it[balance] = walletRecord.balance - amnt
                it[blocked] = amnt
            }
        }
    }

    fun addToStake(poolPubKey:String, amnt: BigDecimal , vl: String) : Boolean = transaction {
        getPool(poolPubKey)!!.let { pl ->
            getPoolUser(poolPubKey,vl).let{ rs ->
                if(rs == null){
                    PoolUsersRepository.insert { t ->
                        t[amount] = amnt
                        t[validator] = vl
                        t[pool] = poolPubKey
                    }
                    true
                }else{
                    PoolUsersRepository.update({ PoolUsersRepository.id eq rs[PoolUsersRepository.id] }){
                        it[amount] = rs[PoolUsersRepository.amount] + amnt
                    }
                    true
                }
            }
        }
    }

    fun decrementToStake(poolPubKey: String,amnt: BigDecimal,vl: String) = transaction {
        getPool(poolPubKey)!!.let {  pool ->
            getPoolUser(poolPubKey,vl).let { rs ->
                if (rs != null) {
                    //if amount greater than validator stake remove all stakes
                        if (rs[PoolUsersRepository.amount] == convertDecimal(0.0)){
                            PoolUsersRepository.deleteWhere {
                                PoolUsersRepository.id eq rs[PoolUsersRepository.id]
                            }
                        }
                        if (rs[PoolUsersRepository.amount]  <= amnt){
                            PoolUsersRepository.update({ PoolUsersRepository.id eq rs[PoolUsersRepository.id] }){
                                it[amount] = BigDecimal(0)
                                PoolUsersRepository.deleteWhere {
                                    PoolUsersRepository.id eq rs[PoolUsersRepository.id]
                                }
                            }
                        }else{
                            PoolUsersRepository.update({ PoolUsersRepository.id eq rs[PoolUsersRepository.id] }){
                                it[amount] = rs[PoolUsersRepository.amount] - amnt
                            }
                        }
                    true
                }else{
                    false
                }
            }
        }
    }

    fun getPool(pubKey:String) : StakeDTO? = transaction {
        StakeRecordRepository.select {
            StakeRecordRepository.staker eq  pubKey
        }.limit(1,offset = 0).firstOrNull()?.let { rs ->
            StakeRecord.wrapRow(rs).convert()
        }
    }

    fun getPoolUsers(pl : String) : List<PoolUser> = transaction {
        PoolUsersRepository.select {
            PoolUsersRepository.pool eq pl
        }.map { rs ->
            PoolUserRecord.wrapRow(rs).convert()
        }
    }

    fun getValidators() : List<PoolUser> = transaction {
        PoolUsersRepository.selectAll().map {
            PoolUserRecord.wrapRow(it).convert()
        }
    }

    fun getPoolUser(pl: String,pbKey:String) : ResultRow? = transaction {
        PoolUsersRepository.select {
            PoolUsersRepository.pool eq pl
            PoolUsersRepository.validator eq pbKey
        }.firstOrNull()
    }

    fun getPoolUser(pbKey:String) : PoolUser? = transaction {
        PoolUsersRepository.select {
            PoolUsersRepository.validator eq pbKey
        }.firstOrNull().let {
            if (it != null){
                PoolUserRecord.wrapRow(it).convert()
            }else{
                null
            }
        }
    }

    fun getStakers(): List<StakeDTO> = transaction {
        StakeRecordRepository.selectAll().orderBy(StakeRecordRepository.amount, SortOrder.DESC)
            .map {
                StakeRecord.wrapRow(it).convert()
            }
    }
}