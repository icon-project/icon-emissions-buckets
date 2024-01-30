package icon.inflation.test.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

@ScoreInterface
public interface IRC2 {

    @External(readonly = true)
    String name();

    @External(readonly = true)
    String symbol();

    @External(readonly = true)
    BigInteger decimals();

    @External(readonly = true)
    BigInteger totalSupply();

    @External(readonly = true)
    BigInteger balanceOf(Address _owner);

    @External
    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);

    @EventLog
    void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data);
}