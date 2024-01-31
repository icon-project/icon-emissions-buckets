package icon.inflation.test.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.annotation.External;

import java.math.BigInteger;

@ScoreInterface
public interface BalancedOracle {
    @External
    BigInteger getLastPriceInUSD(String symbol);
}
