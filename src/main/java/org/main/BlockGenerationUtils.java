package org.main;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.main.entity.Block;

import java.nio.charset.StandardCharsets;

public final class BlockGenerationUtils {
    private static final int RANDOM_DATA_TEXT_BOUND = 30;
    private static final int DIFFICULTY = 4;
    private static final String LAST_SYMBOLS = "0".repeat(DIFFICULTY);
    private static final DigestUtils HASH_COMPUTER = new DigestUtils("SHA-256");


    private BlockGenerationUtils() {}

    public static String generateRandomData() {
        return RandomStringUtils.randomAlphabetic(RANDOM_DATA_TEXT_BOUND);
    }

    public static Block generateGenesis() throws InterruptedException {
        long index = 0L;
        String data = BlockGenerationUtils.generateRandomData();
        String text = index + data;

        Pair<String, Long> result = calculateHash(text);
        String hash = result.getKey();
        Long nonce = result.getValue();

        return new Block(index, null, hash, nonce, data);
    }

    @NotNull
    public static Block generateBlock(Block prevBlock) throws InterruptedException {

        long index = prevBlock.getIndex() + 1;
        String prevHash = prevBlock.getHash();
        String data = BlockGenerationUtils.generateRandomData();
        String text = index + prevHash + data;

        Pair<String, Long> result = calculateHash(text);
        String hash = result.getKey();
        Long nonce = result.getValue();

        return new Block(index, prevHash, hash, nonce, data);
    }

    private static Pair<String, Long> calculateHash(String basicText) throws InterruptedException {
        long nonce = 0L;
        String hashResult;
        do {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            String dataToHash = basicText + nonce++;
            hashResult = HASH_COMPUTER.digestAsHex(dataToHash.getBytes(StandardCharsets.UTF_8));
        } while (!isHashMeetsRequirements(hashResult));
        return Pair.of(hashResult, nonce);
    }

    private static boolean isHashMeetsRequirements(String hash) {
        String lastSymbols = hash.substring(hash.length() - DIFFICULTY);
        return LAST_SYMBOLS.equals(lastSymbols);
    }

}
