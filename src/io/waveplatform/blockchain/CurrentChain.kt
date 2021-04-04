package io.waveplatform.blockchain


data class CurrentChain(
    val height : Long,
    val block: Block,
    val full: Long? = 0,
    val state: ChainMode = ChainMode.CHECK,
)


enum class ChainMode(val state : String){
    INIT("INTL"),
    CHECK("CHECK"),
    NEXT("NEXTBLOCK"),
    PREVIOUS("PREVIOUSBLOCK"),
    FINISHED("HANGUP"),
}