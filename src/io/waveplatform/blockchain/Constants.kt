package io.waveplatform.blockchain

import io.waveplatform.blockchain.db.util.convertDecimal
import java.math.BigDecimal
import java.time.Instant

class Constants {
    companion object{
        val TRANSACTION_FEE: BigDecimal = BigDecimal(0.15)
        val TRANSACTION_FEE_PER_BYTE : BigDecimal = BigDecimal(0.0005)
        const val BLOCK_REWARD: Int = 5
        const val TRANSACTIONS_PER_BLOCK = 4
        const val VALIDATORS_FEE = 30
        const val INITIAL_COINS = 10000
        const val MAX_BALANCE = 100000000000000000L
        const val EPOCH_BEGINNING = 20
        const val MINIMUM_STAKE_AMOUNT = 1
        const val INITIAL_POOL_LEADER =  "0512d818771130abf35543032887fe2ae9677379c013126e1f092b366ad3391a"
        const val POOL_ADDRESS_RECEIVER = "11f37f565eec2cac7c06a68705cdeb304b294dcfd856cdb4c153a03a2af45335"


        const val MIN_VALIDATOR_AMOUNT  = 10000.0
        const val MIN_STAKER_AMOUNT  = 100000.0



    }
}