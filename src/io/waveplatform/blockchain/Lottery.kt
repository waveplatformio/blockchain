package io.waveplatform.blockchain

import io.waveplatform.blockchain.db.PoolUser
import java.math.BigDecimal

/**
 * ticket
 * winningCode is public key of validator
 * tickets is sum of stake percent
 */
data class Ticket(
    val winingCode : String,
    val tickets : Int,
)

/**
 * participants
 * has tickets @see ticket
 * id is validator public key
 */
data class Participants(
    val id : String,
    val tickets : Ticket
)

/**
 * lottery
 * more chance has who stake amount is greater
 */
class Lottery(
    private val participants: List<Participants>,
) {

    private val cylinder = mutableListOf<String>()

    fun initLottery() : Lottery{
        participants.forEach {
            val tickets = it.tickets.tickets
            for (i in 1..tickets){
                cylinder.add(it.tickets.winingCode)
            }
        }
        return this
    }

    fun getWinner(): String {
        return cylinder.random()
    }

    companion object {
        /**
         * convert pool user to participants and initialize lottery
         */
        fun participants(poolUser: List<PoolUser>) : Lottery{
            val participants = createParticipants(poolUser)
            val lottery = Lottery(participants)
            return lottery.initLottery()
        }

        /**
         * create lottery participants
         *  user ticket sum is user stake amount percent of sum of pool stakes
         */
        private fun createParticipants( poolUser: List<PoolUser> ) : List<Participants>{
            val participants = mutableListOf<Participants>()
            val lotterySum = calculateLotterySum(poolUser)
            poolUser.forEach {
                val userPercent = (it.amount * BigDecimal(100)) / lotterySum
                when(userPercent.toInt()){
                    0 -> participants.add(Participants(
                        id = it.address,
                        tickets =  Ticket(
                            winingCode = it.address,
                            tickets = 1
                        )
                    ))
                    else -> participants.add(Participants(
                        id = it.address,
                        tickets = Ticket(
                            winingCode = it.address,
                            tickets = userPercent.toInt()
                        )
                    ))
                }
            }
            return participants
        }

        /**
         * calculate sum of participant amount
         */
        private fun calculateLotterySum(poolUser: List<PoolUser>) : BigDecimal {
            var sum = BigDecimal( 0.0)
            poolUser.forEach {
                sum += it.amount
            }
            return sum
        }

    }
}