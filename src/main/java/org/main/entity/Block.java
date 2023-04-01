package org.main.entity;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class Block {
    private static final int DIFFICULTY = 4;
    private static final String LAST_SYMBOLS = "0".repeat(DIFFICULTY);
    private static final DigestUtils HASH_COMPUTER = new DigestUtils("SHA-256");

    private final long timestamp;
    private final long index;
    private final String prevHash;
    private final String hash;
    private final String data;
    private final Long nonce;

    public Block(long index, @Nullable String prevHash, @NotNull String data) {
        this.index = index;
        this.prevHash = prevHash;
        this.data = data;
        Pair<String, Long> hashToNonce = calculateHash();
        this.hash = hashToNonce.getKey();
        this.nonce = hashToNonce.getValue();
        this.timestamp = System.nanoTime();
    }

    public Pair<String, Long> calculateHash() {
        String basicText = index + prevHash + data;
        long localNonce = 0L;
        String hashResult;

        do {
            String dataToHash = basicText + localNonce++;
            hashResult = HASH_COMPUTER.digestAsHex(dataToHash.getBytes(StandardCharsets.UTF_8));
        } while (!isHashMeetsRequirements(hashResult));
        return Pair.of(hashResult, localNonce);
    }

    private static boolean isHashMeetsRequirements(String hash) {
        String lastSymbols = hash.substring(hash.length() - DIFFICULTY);
        return LAST_SYMBOLS.equals(lastSymbols);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getIndex() {
        return index;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getHash() {
        return hash;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("index", index)
                .append("prevHash", prevHash)
                .append("hash", hash)
                .append("data", data)
                .append("nonce", nonce)
                .toString();
    }
}
