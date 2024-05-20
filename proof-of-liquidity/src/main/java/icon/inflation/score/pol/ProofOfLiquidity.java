package icon.inflation.score.pol;

import static icon.inflation.score.util.Checks.onlyOwner;
import static icon.inflation.score.util.Constants.EXA;

import java.math.BigInteger;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import icon.inflation.score.structs.LiquidityDistribution;
import icon.inflation.score.util.DBUtils;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.VarDB;
import score.annotation.External;
import score.annotation.Payable;

public class ProofOfLiquidity {

    public static final ArrayDB<LiquidityDistribution> distribution = Context.newArrayDB("DISTRIBUTIONS", LiquidityDistribution.class);

    public static final VarDB<Address> staking = Context.newVarDB("STAKING_ADDRESS", Address.class);
    public static final VarDB<Address> sICX = Context.newVarDB("STAKED_ICX", Address.class);
    public static final VarDB<Address> balancedRewards = Context.newVarDB("BALANCED_REWARDS", Address.class);

    private static BigInteger TOTAL_SHARE = EXA;
    private static String NAME = "ICON Proof of liquidity Manager";

    private static boolean distributing = false;

    public ProofOfLiquidity(Address staking, Address sICX, Address balancedRewards) {
        ProofOfLiquidity.staking.set(staking);
        ProofOfLiquidity.sICX.set(sICX);
        ProofOfLiquidity.balancedRewards.set(balancedRewards);
    }

    @External(readonly = true)
    public String name() {
        return NAME;
    }


    @External
    public void setStaking(Address _staking) {
        onlyOwner();
        staking.set(_staking);
    }

    @External(readonly = true)
    public Address getStaking() {
        return staking.get();
    }


    @External
    public void setBalancedRewards(Address _balancedRewards) {
        onlyOwner();
        balancedRewards.set(_balancedRewards);
    }

    @External(readonly = true)
    public Address getBalancedRewards() {
        return balancedRewards.get();
    }


    @External
    public void setSICX(Address _sICX) {
        onlyOwner();
        sICX.set(_sICX);
    }

    @External(readonly = true)
    public Address getSICX() {
        return sICX.get();
    }

    @External(readonly = true)
    public LiquidityDistribution[] getDistributions() {
        int size = distribution.size();
        LiquidityDistribution[] _distribution = new LiquidityDistribution[size];
        for (int i = 0; i < size; i++) {
            _distribution[i] = distribution.get(i);
        }

        return _distribution;
    }

    @External
    public void configureDistributions(LiquidityDistribution[] _distribution) {
        onlyOwner();
        BigInteger sum = BigInteger.ZERO;
        DBUtils.clear(distribution);

        for (LiquidityDistribution dist : _distribution) {
            Context.require(dist.share.compareTo(BigInteger.ZERO) > 0, Errors.NEGATIVE_PERCENTAGE);
            sum = sum.add(dist.share);
            distribution.add(dist);
        }

        Context.require(sum.equals(TOTAL_SHARE), Errors.INVALID_SUM);
    }

    @External
    public void distribute() {
        Context.require(!distributing, Errors.NO_REENTRY);

        BigInteger balance = Context.getBalance(Context.getAddress());
        Context.require(balance.compareTo(BigInteger.ZERO) > 0, Errors.EMPTY_BALANCE);

        int size = distribution.size();
        Context.require(size > 0, Errors.NOT_CONFIGURED);

        distributing = true;

        Context.call(balance, staking.get(), "stakeICX", null, null);
        BigInteger amount = Context.call(BigInteger.class, sICX.get(), "balanceOf", Context.getAddress());
        BigInteger sum = BigInteger.ZERO;

        JsonArray data = new JsonArray();
        for (int i = 0; i < size; i++) {
            LiquidityDistribution dist = distribution.get(i);
            // Dev note: This way of calculating will create remaining dust. But since we
            // are dealing with large amounts this should not matter and a good trade of for
            // simplicity
            BigInteger share = dist.share.multiply(amount).divide(TOTAL_SHARE);
            sum = sum.add(share);
            JsonValue jsonData = new JsonObject()
                .add("source", dist.source)
                .add("amount", share.toString());
            data.add(jsonData);
        }

        Context.call(sICX.get(), "transfer", balancedRewards.get(), sum, data.toString().getBytes());
        distributing = false;
    }



    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
    }

    @Payable
    public void fallback() {
        try {
            distribute();
        } catch (Exception e) {
        }
    }

}
