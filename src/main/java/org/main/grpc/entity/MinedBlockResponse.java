package org.main.grpc.entity;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.main.entity.Block;


public class MinedBlockResponse {

    private final String host;
    private final MinedBlockResponseCode minedBlockResponseCode;
    private final Block block;

    public MinedBlockResponse(@NotNull String host, @NotNull MinedBlockResponseCode minedBlockResponseCode, @Nullable Block block) {
        Validate.notBlank(host);
        Validate.notNull(minedBlockResponseCode);
        this.host = host;
        this.minedBlockResponseCode = minedBlockResponseCode;
        this.block = block;
    }


    @NotNull
    public String getHost() {
        return host;
    }

    @NotNull
    public MinedBlockResponseCode getResponseCode() {
        return minedBlockResponseCode;
    }

    @Nullable
    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("host", host)
                .append("minedBlockResponseCode", minedBlockResponseCode)
                .append("block", block)
                .toString();
    }
}
