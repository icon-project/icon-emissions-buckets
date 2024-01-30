package icon.inflation.test.interfaces;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import foundation.icon.score.client.ScoreInterface;

import score.annotation.External;
import score.annotation.Optional;

@ScoreInterface
public interface BalancedDex {

    @External(readonly = true)
    Map<String, Object> getPoolStats(BigInteger _id);

    @External
    void remove(BigInteger _id, BigInteger _value, @Optional boolean _withdraw);



}
