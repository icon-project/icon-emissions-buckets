package icon.inflation.score.savings;

import java.math.BigInteger;

import icon.inflation.score.interfaces.ISavingsRate;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import score.annotation.Payable;
import score.annotation.EventLog;

import static icon.inflation.score.util.Checks.onlyOwner;

public class SavingsRate implements ISavingsRate {
    public static final String NAME = "ICON-Balanced Savings Rate";

    public static final VarDB<Address> staking = Context.newVarDB("STAKING_ADDRESS", Address.class);
    public static final VarDB<Address> balancedReceiver = Context.newVarDB("BALANCED_SAVINGS_RECEIVER", Address.class);

    public SavingsRate(Address _staking, Address _balancedReceiver) {
        staking.set(_staking);
        balancedReceiver.set(_balancedReceiver);
    }

    @External(readonly = true)
    public String name() {
        return NAME;
    }

    @External(readonly = true)
    public Address getStaking() {
        return staking.get();
    }

    @External
    public void setStaking(Address _staking) {
        onlyOwner();
        staking.set(_staking);
    }

    @External(readonly = true)
    public Address getBalancedReceiver() {
        return balancedReceiver.get();
    }

    @External
    public void setBalancedReceiver(Address _balancedReceiver) {
        onlyOwner();
        balancedReceiver.set(_balancedReceiver);
    }

    @Payable
    public void fallback() {
        try {
            stakeAndSend(Context.getBalance(Context.getAddress()));
        } catch (Exception e) {
            StakeFailed(e.getMessage());
        }
    }

    @External
    public void stakeAndSend(BigInteger amount) {
        Context.call(amount, staking.get(), "stakeICX", balancedReceiver.get());
    }

    @EventLog(indexed = 1)
    public void StakeFailed(String reason) {
    }
}
