package icon.inflation.score.util;

import java.math.BigInteger;

import score.Address;


public class Constants {
    public final static BigInteger EXA = new BigInteger("1000000000000000000");
    public final static BigInteger POINTS = BigInteger.valueOf(10000);
    public final static Address GOVERNANCE_SCORE = Address.fromString("cx0000000000000000000000000000000000000001");
    public static final BigInteger MICRO_SECONDS_IN_A_SECOND = BigInteger.valueOf(1_000_000);
    public static final BigInteger MICRO_SECONDS_IN_A_DAY =
            BigInteger.valueOf(86400).multiply(MICRO_SECONDS_IN_A_SECOND);
    public static final BigInteger MICRO_SECONDS_IN_A_WEEK = BigInteger.valueOf(7L).multiply(MICRO_SECONDS_IN_A_DAY);
    public static final BigInteger MICRO_SECONDS_IN_A_MONTH = BigInteger.valueOf(4).multiply(MICRO_SECONDS_IN_A_WEEK);
    public static final BigInteger BLOCKS_IN_A_MONTH = BigInteger.valueOf(43200 * 30);
}
