package icon.inflation.score.util;

import java.math.BigInteger;

public class Math {
    public static BigInteger pow(BigInteger base, int exponent) {
        BigInteger res = BigInteger.ONE;
        for (int i = 1; i <= exponent; i++) {
            res = res.multiply(base);
        }
        return res;
    }
}
