package org.main.grpc.entity;


import org.jetbrains.annotations.NotNull;

public enum MinedBlockResponseCode {
    UNKNOWN(0),
    ACCEPTED(1),
    REJECTED(2),
    FAILED(3)
    ;

    private final int code;

    MinedBlockResponseCode(int code) {
        this.code = code;
    }

    @NotNull
    public static MinedBlockResponseCode from(int code) {
        for (MinedBlockResponseCode value : MinedBlockResponseCode.values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Failed to get ResponseCode from code: " + code);
    }
}
