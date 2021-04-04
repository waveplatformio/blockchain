package io.waveplatform.blockchain

import java.lang.Exception

class InvalidMnemonicException(
    override val message : String
): Exception(message)


class TransactionTransferException(
    override val message : String
): Exception(message)


class RecordInitializationNotExecutedException(
    override val message : String
): Exception(message)

class ServiceNotBootedException (
    override val message : String
): Exception(message)