package io.waveplatform.rpc.server.service

import io.waveplatform.server.requests.CalculateFee
import io.waveplatform.server.requests.TransactionRequestSignable

interface FullNode {

    fun getBlockByHash(hash : String) : ResponseStatus

    fun getTransactionById(txId : String) : ResponseStatus

    fun unfinishedTransactions() : ResponseStatus

    fun getWalletByAddressRaw(pubKey: String) : ResponseStatus

    fun getWalletByAddress(pubKey:String) : ResponseStatus

    fun signWallet(secret:String) : ResponseStatus

    fun chain() : ResponseStatus

    fun freezes() : ResponseStatus

    fun sync():SystemState

    fun createMnemonics():SystemState

    fun calculateFee(calculateFee : CalculateFee) : ResponseStatus

    fun transact(transactionRequest: TransactionRequestSignable) : ResponseStatus

    fun createPool(poolRequest : CreatePoolRequest) : SystemState

    fun createWallet() : SystemState

    fun validators(pool : String) : ResponseStatus
}

