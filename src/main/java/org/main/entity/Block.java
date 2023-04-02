package org.main.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Block {

    private final long timestamp;
    private final long index;
    private final String prevHash;
    private final String data;
    private String hash;
    private Long nonce;

    public Block(long index, @Nullable String prevHash, @NotNull String data) {
        this.index = index;
        this.prevHash = prevHash;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Block(long index, @Nullable String prevHash, @NotNull String hash, @NotNull Long nonce, @NotNull String data) {
        this.index = index;
        this.prevHash = prevHash;
        this.data = data;
        this.hash = hash;
        this.nonce = nonce;
        this.timestamp = System.currentTimeMillis();
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

    public Long getNonce() {
        return nonce;
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
