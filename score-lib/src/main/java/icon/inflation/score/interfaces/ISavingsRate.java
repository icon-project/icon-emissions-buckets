package icon.inflation.score.interfaces;

import java.math.BigInteger;

import score.Address;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

public interface ISavingsRate {

    @External(readonly = true)
    String name();

    @External(readonly = true)
    Address getStaking();

    @External
    void setStaking(Address _staking);

    @External(readonly = true)
    Address getBalancedReceiver();

    @External
    void setBalancedReceiver(Address _balancedReceiver);

    @Payable
    void fallback();

    @External
    void stakeAndSend(BigInteger amount);

    @EventLog(indexed = 0)
    void StakeFailed();
}
