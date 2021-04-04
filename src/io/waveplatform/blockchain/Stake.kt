package io.waveplatform.blockchain

import io.waveplatform.blockchain.db.PoolUser
import io.waveplatform.blockchain.db.PoolUserRecord
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.StakerService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.wallet.Transaction
import java.math.BigDecimal


class Stake(
    blockService: BlockService
) {

    val walletService  = WalletService(blockService)
    val stakeService : StakerService = StakerService(walletService)

    private fun addStake(from: String, amount: BigDecimal){
        stakeService.addStake(from,amount)
    }


    fun announcePoolAndGetLeader() : String{
        val stake = stakeService.getStakers().random().staker
        return getLeader(stake)
    }

    /**
     * added lottery functionalities
     * more chance has validator stake is greater
     */
    private fun getLeader(pool : String): String{
        val stakes = stakeService.getPoolUsers(pool)
        println("getting leader")
        println(stakes.isNotEmpty())
        if (stakes.isNotEmpty()){
            return Lottery.participants(stakes).getWinner()
        }
        return pool
    }


    fun poolLeaderExists(pool: String, validator:String) : PoolUser? {
        val rs =  stakeService.getPoolUser(pool,validator)
        if (rs != null){
            return PoolUserRecord.wrapRow(rs).convert()
        }
        return null
    }

    fun update(transaction: Transaction){
        val amount = transaction.output!!.amount
        val from = transaction.input!!.from
        //amount staked
        this.addStake(from,amount)
    }

    private fun addToStake(from: String, to:String, amount: BigDecimal) : Boolean {
        return stakeService.addToStake(to,amount,from)
    }

    private fun decrementToStake(from:String,to: String,amount: BigDecimal) : Boolean {
        if (stakeService.getPoolUser(to,from) == null){
            throw TransactionTransferException("cannot transfer pool user doesn't exists")
        }
        return stakeService.decrementToStake(to,amount,from)
    }

    fun updateValidator(transaction: Transaction): Boolean{
        var pool =  stakeService.getPool(transaction.output!!.to)?.let{
            val amount = transaction.output!!.amount
            val from = transaction.input!!.from
            val to = transaction.output!!.to
            this.addToStake(from,to,amount)
        }
        if (pool == null) pool = false
        return pool
    }

    fun decrementUpdateValidator(transaction: Transaction) : Boolean {
        var pool = stakeService.getPool(transaction.output!!.to)?.let {
            val amount = transaction.output!!.amount
            val from = transaction.input!!.from
            val to = transaction.output!!.to
            this.decrementToStake(from,to,amount)
        }

        if (pool == null) pool = false
        return pool
    }

}


