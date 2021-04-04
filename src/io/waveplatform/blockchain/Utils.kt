package io.waveplatform.blockchain


import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.wallet.KeyPairModel
import io.github.novacrypto.bip39.wordlists.English

import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.MnemonicValidator

import io.github.novacrypto.bip39.Words
import java.security.SecureRandom
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Validation.InvalidChecksumException
import io.github.novacrypto.bip39.Validation.InvalidWordCountException
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException
import io.github.novacrypto.bip39.Validation.WordNotFoundException
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.Transaction
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.time.Instant


fun String.hash(algorithm: String = "SHA-256"): String {
    val pubKey = Crypto.getPublicKey(this)
    val messageDigest = Crypto.sha256()

    val signature = messageDigest.digest(pubKey)
//    val previousBlockHash =
    return Convert.toHexString(Crypto.sha256().digest(this.toByteArray()))
}



fun verify(plainText: String, signature: ByteArray, publicKey: ByteArray): Boolean {
    return Crypto.verify(signature,Convert.toBytes(plainText),publicKey,true)
}

fun sign(plainText: String, secretPhrase: String): ByteArray {
    return Crypto.sign(plainText.toByteArray(),secretPhrase)
}

fun generateKeyPair(secret : String): KeyPairModel {
    return KeyPairModel(
        Crypto.getPublicKey(secret),
        Crypto.getPrivateKey(secret)
    )
}

fun generateMnemonic(): String {
    val sb = StringBuilder()
    val entropy = ByteArray(Words.TWELVE.byteLength())
    SecureRandom().nextBytes(entropy)
    MnemonicGenerator(English.INSTANCE)
        .createMnemonic(entropy) { s: CharSequence? -> sb.append(s) }
    return sb.toString()
}

fun generatePubKeyFromRandomMnemonics(passPhrase : String = "") : MutableMap<String, String> {
    val mnemonics = generateMnemonic()
    val map = mutableMapOf<String,String>()
    map.put("pubKey", Convert.toHexString(Crypto.getPublicKey(mnemonics)))
    map.put("privKey", Convert.toHexString(Crypto.getPrivateKey(mnemonics)))
    map.put("phrases", mnemonics)
    return map
}

fun getPublicKey(mnemonic: String, passPhrase: String = ""): String {
    return Convert.toHexString(Crypto.getPublicKey(mnemonic))
}

fun getKeyPair(mnemonic: String,passPhrase: String = "") : Pair<String,String> {
    val pubKey = Convert.toHexString(Crypto.getPublicKey(mnemonic))
    val privKey = Convert.toHexString(Crypto.getPrivateKey(mnemonic))
    return Pair(pubKey,privKey)
}

fun validateMnemonic(mnemonic : String){
    try {
        MnemonicValidator
            .ofWordList(English.INSTANCE)
            .validate(mnemonic);
    } catch (e : UnexpectedWhiteSpaceException) {
        throw InvalidMnemonicException("invalid Mnemonic")
    } catch (ei : InvalidWordCountException) {
        throw InvalidMnemonicException("invalid Mnemonic")
    } catch (eic : InvalidChecksumException) {
        throw InvalidMnemonicException("invalid Mnemonic")
    } catch (wn : WordNotFoundException) {
        throw InvalidMnemonicException("invalid Mnemonic")
    }
}

fun verifyChallenge(publicKey: ByteArray,privateKey: ByteArray, challenge : ByteArray = ByteArray(32)) : Boolean {
    val signature = Crypto.sign(challenge,privateKey)
    return Crypto.verify(signature, challenge ,publicKey,true)
}

fun main(args : Array<String>) {

    val buffer = ByteBuffer.allocate( 64)
    buffer.putLong(999999999999999999L)
    println(1)
    println(Instant.now().toEpochMilli())
    val gn = convertDecimal(25000000.00)
    val anWallet = convertDecimal(100000.15)
    // 24989949.850
    val fee = convertDecimal(50.150000)
    println(gn - anWallet + convertDecimal(5.1750000))

    println(Transaction.calculateFee(convertDecimal(100050.15)))

//    val mnemonic = generateMnemonic()
//    val mnemonic = "forest stereo cannon hurry jar lamp open iron online lonely receive jewel"
    val mnemonic = "bless fiction fame tell crater maze february fault long maid bring legend"
    validateMnemonic(mnemonic)
    val kp = getKeyPair(mnemonic)
//    println(kp.first)
//    println(kp.second)

    val pubKey = kp.first
    val privKey = kp.second

    println(pubKey.hash())
    println(pubKey)

////    val signature = Crypto.sign(mnemonic.toByteArray(),Convert.toHexString(privKey))
//    val signature = Crypto.sign(ByteArray(32),privKey)
//
//    val verifi = Crypto.verify(signature, ByteArray(32) ,pubKey,true)
//    val verifi1 = Crypto.verify(signature, ByteArray(32) ,pubKey1,true)

//    println(verifyChallenge(pubKey,privKey))
//    println(verifyChallenge(
//      Convert.parseHexString(kp.first)  , Convert.parseHexString(kp.second)
//    ))
//
//    println(Convert.toHexString(privKey))
//    println(Convert.toHexString(pubKey))

}

