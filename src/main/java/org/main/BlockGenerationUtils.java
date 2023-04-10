package org.main;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Nullable;
import org.main.entity.Block;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

public final class BlockGenerationUtils {
    private static final DigestUtils HASH_COMPUTER = new DigestUtils("SHA-256");

    private static final int RANDOM_DATA_TEXT_BOUND = 30;
    private static final int DIFFICULTY = 5;
    private static final String LAST_SYMBOLS = "0".repeat(DIFFICULTY);


    public static final long GENESIS_BLOCK_INDEX = 0L;
    public static final String GENESIS_BLOCK_PREV_HASH = "stub";


    private BlockGenerationUtils() {}

    public static String generateRandomData() {
        return RandomStringUtils.randomAlphabetic(RANDOM_DATA_TEXT_BOUND);
    }

    public static Block generateGenesis() {
        System.out.println("Generating genesis block...");
        long index = GENESIS_BLOCK_INDEX;
        String data = BlockGenerationUtils.generateRandomData();
        String text = index + data;

        String hash;
        long nonce = 0L;
        do {
            hash = calculateHash(text, ++nonce);
        } while (!isHashMeetsRequirements(hash));
        return new Block(index, GENESIS_BLOCK_PREV_HASH, hash, nonce, data);
    }

    @Nullable
    public static Block generateBlock(Block prevBlock, BooleanSupplier stopCondition) {
        long index = prevBlock.getIndex() + 1;
        String prevHash = prevBlock.getHash();
        String data = BlockGenerationUtils.generateRandomData();
        String text = index + prevHash + data;

        System.out.printf("Mining block with index: [%d]%n", index);
        String hash;
        long nonce = 0L;
        do {
            if (stopCondition.getAsBoolean()) {
                return null;
            }
            hash = calculateHash(text, ++nonce);
        } while (!isHashMeetsRequirements(hash));
        return new Block(index, prevHash, hash, nonce, data);
    }

    private static String calculateHash(String basicText, long nonce) {
        String dataToHash = basicText + nonce;
        return HASH_COMPUTER.digestAsHex(dataToHash.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isHashMeetsRequirements(String hash) {
        String lastSymbols = hash.substring(hash.length() - DIFFICULTY);
        return LAST_SYMBOLS.equals(lastSymbols);
    }

}
