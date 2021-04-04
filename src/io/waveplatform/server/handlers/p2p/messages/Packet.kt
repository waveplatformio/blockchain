package io.waveplatform.server.handlers.p2p.messages

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.ChainMode
import io.waveplatform.blockchain.CurrentChain
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.server.handlers.p2p.MessageTypes
import java.awt.TrayIcon

class Packet {


    companion object{

        fun sync() : String {
            return jsonify(
                Message(
                    type = MessageTypes.SYNC,
                    block = null,
                    transaction = null,
                    transactions = null,
                    height = CurrentChain(0,Block.genesis(),state = ChainMode.INIT)
                )
            )!!
        }

        fun serve(currentChain: CurrentChain) : String {
            return jsonify(
                Message(
                    type = MessageTypes.SYNC,
                    block = null,
                    transaction = null,
                    transactions = null,
                    height = currentChain
                )
            )!!
        }

        fun serve(block: Block): String{
            return jsonify(
                Message(
                    type = MessageTypes.BLOCK,
                    block = block,
                    transaction = null,
                    transactions = null,
                    height = null
                )
            )!!
        }

        fun serve(transaction: MutableList<Transaction>): String{
            return jsonify(
                Message(
                    type = MessageTypes.WALLETTRANSACTIONS,
                    block = null,
                    transaction = null,
                    transactions = transaction,
                    height = null
                )
            )!!
        }

        fun serve(transaction: Transaction): String{
            return jsonify(
                Message(
                    type = MessageTypes.TRANSACTION,
                    block = null,
                    transaction = transaction,
                    transactions = null,
                    height = null
                )
            )!!
        }

        fun serve(transaction: Transaction, type : MessageTypes): String{
            return jsonify(
                Message(
                    type = type,
                    block = null,
                    transaction = transaction,
                    transactions = null,
                    height = null
                )
            )!!
        }

        private fun jsonify(obj: Message): String? {
            val mapper = jacksonObjectMapper()
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
            return mapper.writeValueAsString(obj)
        }

        public fun jsonify(obj: Any): String {
            val mapper = jacksonObjectMapper()
            return mapper.writeValueAsString(obj)
        }

    }
}