package io.waveplatform.blockchain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.db.PoolUser
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.wallet.Transaction
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

data class Block(
    val id: Long = 0,
    val previousHash: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val timestamp: Long,
    val validator: String?,
    var hash: String?,
    val secret : String?,
    val finished : Boolean = false,
    var confirmations : Int = 0,
    val blockReward: Int = Constants.BLOCK_REWARD
) {


    @get:JsonIgnore
    lateinit var signature : ByteArray

    private var bytes : ByteArray? = null
    private var version = 0
    private var hasValidSignature = false

    fun attachSignature(signature:String){
        this.signature = Crypto.sign(bytes(),signature)
    }

    init {
        if (hash == null){
            hash = Convert.toHexString(
                Crypto.sha256().digest(bytes())
            )
        }
    }

    fun checkSignature(): Boolean {
        if (!hasValidSignature && this::signature.isInitialized) {
            val data = bytes().copyOf(bytes!!.size)
            hasValidSignature =  Crypto.verify(signature, data, Convert.parseHexString(validator), true)
        }
        println("signature init ")
        println(this::signature.isInitialized)
        return hasValidSignature
    }

    fun verifyLeader(block: Block, leader: PoolUser): Boolean{
        return leader.address == block.validator
//        return leader.contentEquals(Convert.parseHexString(block.validator))
    }

    fun bytes() : ByteArray {
        if (bytes == null){
            val buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (if (version < 3) 4 + 4 else 8 + 8) + 4 + 32 + 32 + (32 + 32) + 64)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(version)
            buffer.putInt(timestamp.toInt())
            buffer.putInt(transactions.size)
            transactions.filter { it.type.type != "FREEZE" }.forEach {
                buffer.putLong(it.input!!.timestamp)
            }
            buffer.put(Convert.parseHexString(previousHash))
            bytes = buffer.array()
        }
        return bytes!!
    }

    companion object{
        fun genesis(): Block{
            //hardcoded genesis
            val genesis = """
                {
                "id":1,
                "previousHash":"6e6f2d707265762d68617368",
                "transactions":[
                     {
                        "id": 0,
                        "type": {
                            "type": "Coin"
                        },
                        "input": {
                            "signature": "[B@1170d841",
                            "from": "blockchain",
                            "timestamp": 1615819880256
                        },
                        "output": {
                            "to":"dc2152fd2bfe753c2a65af51c38c0c798cb101d1ac7bcdb906866fe2b7d07c3b",
                            "amount": 25000000.00,
                            "fee": 0.00000000
                        },
                        "txId": "a9e6194dbaa8d220cb4d0dfdfa488c528a1537473aca4c1c8d9fe720490cf212"
                    },
                    {
                        "id": 0,
                        "type": {
                            "type": "transaction"
                        },
                        "input": {
                            "signature": "[B@1510r1x2",
                            "from": "dc2152fd2bfe753c2a65af51c38c0c798cb101d1ac7bcdb906866fe2b7d07c3b",
                            "timestamp": 1615819880257
                        },
                        "output": {
                            "to":"0512d818771130abf35543032887fe2ae9677379c013126e1f092b366ad3391a",
                            "amount": 100000.00,
                            "fee": 0.000000
                        },
                        "txId": "b9e6194dbaa8d220cb4d0dfdfa488c528a1537173aca4c1c8d9fe220490cf212"
                    },
                    {
                        "id": 0,
                        "type": {
                            "type": "validator"
                        },
                        "input": {
                            "signature": "[B@1170d841",
                            "from": "0512d818771130abf35543032887fe2ae9677379c013126e1f092b366ad3391a",
                            "timestamp": 1615819880258
                        },
                        "output": {
                            "to":"0512d818771130abf35543032887fe2ae9677379c013126e1f092b366ad3391a",
                            "amount": 100000.00,
                            "fee": 0.1615819880258
                        },
                        "txId": "a2c6194dbaa8d220cb4d0dfdfa488c528a1537473aka4c1c8d9fe720490cf212"
                    }
                ],
                "timestamp":1615819880255,
                "validator":"6e6f2d707265762d68617368",
                "hash":"b974c35b0ef543fac72ccd6ad63a8583c3ee840821a4ac4e334098e13626b087",
                "secret":"none",
                "blockReward":0
                }
            """.trimIndent()

            val objm = jacksonObjectMapper()
            val genesisBlock = objm.readValue(genesis,Block::class.java)
            genesisBlock.attachSignature("b974c35b0ef543fac72ccd6ad63a8583c3ee840821a4ac4e334098e13626b087")
            return genesisBlock

        }


        fun createBlock(lastBlock: Block, transactions: MutableList<Transaction>, wallet: String): Block{
            val digest = Crypto.sha256()
            val timestamp = Instant.now().toEpochMilli()
            transactions.forEach { transaction ->
                digest.update(transaction.bytes())
            }
            digest.update(lastBlock.hash!!.toByteArray())
            val hash = Convert.toHexString(digest.digest())
            val lastHash = lastBlock.hash!!
            val validator =  wallet
            val block = Block(
                previousHash = lastHash,
                transactions=transactions,
                timestamp=timestamp,
                validator = validator,
                hash = hash,
                secret = wallet
            )
            return block
        }
    }

}