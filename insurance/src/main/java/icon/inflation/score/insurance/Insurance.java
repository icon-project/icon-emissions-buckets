package icon.inflation.score.insurance;

import java.math.BigInteger;

import icon.inflation.score.interfaces.IInsurance;
import score.Context;
import score.Address;
import score.annotation.External;
import score.annotation.Payable;

import static icon.inflation.score.util.Checks.onlyOwner;

public class Insurance implements IInsurance {

    private static String NAME = "ICON Insurance Fund";

    public Insurance() {
    }

    @External(readonly = true)
    public String name() {
        return NAME;
    }

    @External
    public void transfer(Address to, BigInteger amount) {
        onlyOwner();
        Context.transfer(to, amount);
    }

    @Payable
    public void fallback() {
    }

}
