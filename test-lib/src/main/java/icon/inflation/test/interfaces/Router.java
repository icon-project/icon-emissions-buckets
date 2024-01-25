package icon.inflation.test.interfaces;

import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

@ScoreInterface
public interface Router  {
    @Payable
    @External
    void route(Address[] path, @Optional BigInteger _minReceive, @Optional String _receiver);
}
