package io.waveplatform.blockchain.crypto;



import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class EncryptedData {

    public static final EncryptedData EMPTY_DATA = new EncryptedData(new byte[0], new byte[0]);

    public static EncryptedData encrypt(byte[] plaintext, String secretPhrase, byte[] theirPublicKey) {
        if (plaintext.length == 0) {
            return EMPTY_DATA;
        }
        byte[] nonce = new byte[32];
        Crypto.getSecureRandom().nextBytes(nonce);
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), theirPublicKey, nonce);
        byte[] data = Crypto.aesEncrypt(plaintext, sharedKey);
        return new EncryptedData(data, nonce);
    }

    public static EncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength)
            throws NotValidException {
        if (length == 0) {
            return EMPTY_DATA;
        }
        if (length > maxLength) {
            throw new NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] data = new byte[length];
        buffer.get(data);
        byte[] nonce = new byte[32];
        buffer.get(nonce);
        return new EncryptedData(data, nonce);
    }

    public static EncryptedData readEncryptedData(byte[] bytes) {
        if (bytes.length == 0) {
            return EMPTY_DATA;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            return readEncryptedData(buffer, bytes.length - 32, Integer.MAX_VALUE);
        } catch (NotValidException e) {
            throw new RuntimeException(e.toString(), e); // never
        }
    }

    public static int getEncryptedDataLength(byte[] plaintext) {
        if (plaintext.length == 0) {
            return 0;
        }
        return Crypto.aesEncrypt(plaintext, new byte[32]).length;
    }

    public static int getEncryptedSize(byte[] plaintext) {
        if (plaintext.length == 0) {
            return 0;
        }
        return getEncryptedDataLength(plaintext) + 32;
    }

    private final byte[] data;
    private final byte[] nonce;

    public EncryptedData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public byte[] decrypt(String secretPhrase, byte[] theirPublicKey) {
        if (data.length == 0) {
            return data;
        }
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), theirPublicKey, nonce);
        return Crypto.aesDecrypt(data, sharedKey);
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public int getSize() {
        return data.length + nonce.length;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(nonce.length + data.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data);
        buffer.put(nonce);
        return buffer.array();
    }

    @Override
    public String toString() {
        return "data: " + Convert.toHexString(data) + " nonce: " + Convert.toHexString(nonce);
    }

}
