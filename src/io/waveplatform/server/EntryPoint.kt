package io.waveplatform.server

import io.ktor.application.*
import io.ktor.routing.*
import io.waveplatform.blockchain.BlockChain
import io.waveplatform.blockchain.Constants
import io.waveplatform.blockchain.db.*
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.rpc.server.JsonRpcServer
import io.waveplatform.rpc.server.service.FullNodeService
import io.waveplatform.server.config.appConfig
import io.waveplatform.server.handlers.p2p.P2PClient
import io.waveplatform.server.handlers.serverHandler
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.net.URI

/**
 * @author jedy
 * @since 0.1
 */
fun Application.entryPoint(){
    transaction {
        SchemaUtils.create(BlockRecordRepository)
        SchemaUtils.create(TransactionRecordRepository)
        SchemaUtils.create(StakeRecordRepository)
        SchemaUtils.create(WalletRecordRepository)
        SchemaUtils.create(PoolUsersRepository)
    }
    val blockService = BlockService()
    val walletService = WalletService(blockService)
//    val walletSecret = "bless fiction fame tell crater maze february fault long maid bring legend"
    walletService.getWallet("0512d818771130abf35543032887fe2ae9677379c013126e1f092b366ad3391a",null).let {
        if (it == null){
           transaction {
               StakeRecordRepository.insert {
                   it[amount] = BigDecimal(100000.0)
                   it[staker] = Constants.INITIAL_POOL_LEADER
               }
               //secret "bless fiction fame tell crater maze february fault long maid bring legend"
               //secret "often weasel hard trial globe weather merge drastic submit ring bunker monkey"
//               pubKey dc2152fd2bfe753c2a65af51c38c0c798cb101d1ac7bcdb906866fe2b7d07c3b
           }
        }
    }

//    var walletSecret = appConfig.property("ktor.node.owner").getString()
//    val wallet = walletService.logIn(walletSecret)
    val wallet = walletService.logIn("b1f8c76b249b4343fb31947eeeb416cf34011a3767cd46d7d4799f3d65c22b1a")
    val blockhain = BlockChain(wallet=wallet)
    val transactionPool = TransactionPool()
    val wsServer = P2PServer(
        blockhain,
        transactionPool,
        wallet,
        peer = linkedSetOf("ws://51.255.211.135:8282"),
        inetSocketAddress = InetSocketAddress("0.0.0.0",8282),
    )
    async {
        wsServer.run()
    }
    wsServer.listen()
    val p2pClient = P2PClient(URI.create("ws://localhost:8282"))
    val jsonRpcServer = JsonRpcServer(
        arrayOf(
            FullNodeService(
                blockhain,transactionPool,walletService,p2pClient
            )
        )
    )
    routing {
        //json rpc
        post("/jsonrpc") {
            jsonRpcServer.handle(call)
        }
        serverHandler(blockhain,transactionPool,wallet, blockService,walletService,p2pClient)
    }
}