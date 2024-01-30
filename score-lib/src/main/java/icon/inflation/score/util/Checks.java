package icon.inflation.score.util;

import static icon.inflation.score.util.Constants.POINTS;

import java.math.BigInteger;

import score.Address;
import score.Context;

public class Checks {
    public static class Errors {
        public static final String ONLY_OWNER = "Only owner is allowed to call this method";
        public static String ONLY(Address address){ return "Only " + address + " is allowed to call this method";};
        public static final String INVALID_POINTS = "POINTS values must be between 0 and " + POINTS;

    }

    public static void onlyOwner() {
            Address caller = Context.getCaller();
            Address owner = Context.getOwner();
            Context.require(caller.equals(owner), Errors.ONLY_OWNER);
    }

    public static void only(Address allowedCaller) {
        Address caller = Context.getCaller();
        Context.require(caller.equals(allowedCaller), Errors.ONLY(allowedCaller));
    }

    public static void validatePoints(BigInteger value) {
        Context.require(POINTS.compareTo(value) >= 0, Errors.INVALID_POINTS);
        Context.require(BigInteger.ZERO.compareTo(value) <= 0, Errors.INVALID_POINTS);
    }
}
