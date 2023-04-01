package org.main.exception;

public class ChainValidationException extends Exception {

    private static final String CHAIN_VALIDATION_FAILED_TEMPLATE =
            "Chain is not valid in transition from block [%d] to block [%d]";

    public ChainValidationException(long prevBlockIndex, long curBlockIndex) {
        super(String.format(CHAIN_VALIDATION_FAILED_TEMPLATE, prevBlockIndex, curBlockIndex));
    }
}
