package icon.inflation.score.interfaces;

import java.math.BigInteger;

import score.Address;
import score.annotation.External;
import score.annotation.Payable;

public interface IInsurance {
    @External(readonly = true)
    String name();

    /**
     * Transfers 'amount' ICX to 'to'
     *
     * @param to     address which receives payout.
     * @param amount to pay out.
     */
    @External
    void transfer(Address to, BigInteger amount);

    @Payable
    void fallback();
}
