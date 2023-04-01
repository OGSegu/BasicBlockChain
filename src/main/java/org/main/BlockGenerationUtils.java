package org.main;

import org.apache.commons.lang3.RandomStringUtils;

public final class BlockGenerationUtils {

    private BlockGenerationUtils() {}

    public static final int RANDOM_DATA_TEXT_BOUND = 30;

    public static String generateRandomData() {
        return RandomStringUtils.randomAlphabetic(RANDOM_DATA_TEXT_BOUND);
    }

}
