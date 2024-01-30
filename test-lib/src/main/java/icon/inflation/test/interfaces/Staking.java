package icon.inflation.test.interfaces;

import java.math.BigInteger;

import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

@ScoreInterface
public interface Staking {
    @External
    @Payable
    BigInteger stakeICX(@Optional Address _to, @Optional byte[] _data);
}
