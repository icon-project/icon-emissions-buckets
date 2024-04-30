package icon.inflation.score.nol;

import java.math.BigInteger;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import icon.inflation.score.interfaces.INetworkOwnedLiquidity;
import icon.inflation.score.structs.LiquidityOrder;
import icon.inflation.score.util.DBUtils;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import score.annotation.EventLog;

import static icon.inflation.score.util.Checks.only;
import static icon.inflation.score.util.Checks.onlyOwner;
import static icon.inflation.score.util.Checks.validatePoints;
import static icon.inflation.score.util.Constants.EXA;
import static icon.inflation.score.util.Constants.BLOCKS_IN_A_MONTH;
import static icon.inflation.score.util.Constants.POINTS;
import static icon.inflation.score.util.Math.pow;

public class NetworkOwnedLiquidity implements INetworkOwnedLiquidity {
    public static final String NAME = "ICON Network-owned Liquidity";

    public static final DictDB<BigInteger, LiquidityOrder> orders = Context.newDictDB("ORDERS", LiquidityOrder.class);
    public static final ArrayDB<BigInteger> ordersList = Context.newArrayDB("ORDERS_LIST", BigInteger.class);
    public static final VarDB<BigInteger> investedEmissions = Context.newVarDB("INVESTED_EMISSIONS", BigInteger.class);
    public static final VarDB<BigInteger> orderPeriod = Context.newVarDB("ORDER_PERIOD", BigInteger.class); // Microseconds

    public static final VarDB<Address> balancedDex = Context.newVarDB("BALANCED_DEX_ADDRESS", Address.class);
    public static final VarDB<Address> balancedOracle = Context.newVarDB("BALANCED_ORACLE_ADDRESS", Address.class);

    // Points(0-10000)
    public static final VarDB<BigInteger> swapReward = Context.newVarDB("SWAP_REWARD", BigInteger.class);
    public static final VarDB<BigInteger> lPSlippage = Context.newVarDB("LP_SLIPPAGE", BigInteger.class);

    public static final BigInteger DEFAULT_ORDER_PERIOD = BLOCKS_IN_A_MONTH;
    public static final BigInteger DEFAULT_SWAP_REWARDS = BigInteger.valueOf(100); // 1%
    public static final BigInteger DEFAULT_LP_SLIPPAGE = BigInteger.valueOf(100); // 1%

    public NetworkOwnedLiquidity(Address _balancedDex, Address _balancedOracle) {
        balancedDex.set(_balancedDex);
        balancedOracle.set(_balancedOracle);

        orderPeriod.set(DEFAULT_ORDER_PERIOD);
        swapReward.set(DEFAULT_SWAP_REWARDS);
        lPSlippage.set(DEFAULT_LP_SLIPPAGE);
    }

    @EventLog(indexed = 1)
    public void LiquidityPurchased(BigInteger pid, BigInteger lpTokenAmount, BigInteger payout) {
    }

    @External(readonly = true)
    public String name() {
        return NAME;
    }

    @External(readonly = true)
    public LiquidityOrder[] getOrders() {
        int numberOfOrders = ordersList.size();
        LiquidityOrder[] orderData = new LiquidityOrder[numberOfOrders];
        for (int i = 0; i < numberOfOrders; i++) {
            BigInteger pid = ordersList.get(i);
            LiquidityOrder order = orders.get(pid);
            order.pid = pid;
            orderData[i] = order;
        }

        return orderData;
    }

    @External(readonly = true)
    public BigInteger getOrderPeriod() {
        return orderPeriod.get();
    }

    @External
    public void setOrderPeriod(BigInteger _orderPeriod) {
        onlyOwner();
        orderPeriod.set(_orderPeriod);
    }

    @External(readonly = true)
    public Address getBalancedDex() {
        return balancedDex.get();
    }

    @External
    public void setBalancedDex(Address _balancedDex) {
        onlyOwner();
        balancedDex.set(_balancedDex);
    }

    @External(readonly = true)
    public Address getBalancedOracle() {
        return balancedOracle.get();
    }

    @External
    public void setBalancedOracle(Address _balancedOracle) {
        onlyOwner();
        balancedOracle.set(_balancedOracle);
    }

    @External(readonly = true)
    public BigInteger getSwapReward() {
        return swapReward.get();
    }

    @External
    public void setSwapReward(BigInteger _swapReward) {
        onlyOwner();
        validatePoints(_swapReward);
        swapReward.set(_swapReward);
    }

    @External(readonly = true)
    public BigInteger getLPSlippage() {
        return lPSlippage.get();
    }

    @External
    public void setLPSlippage(BigInteger _lPSlippage) {
        onlyOwner();
        validatePoints(_lPSlippage);
        lPSlippage.set(_lPSlippage);
    }

    @External(readonly = true)
    public BigInteger getInvestedEmissions() {
        return investedEmissions.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void configureOrder(BigInteger pid, BigInteger limit) {
        onlyOwner();
        LiquidityOrder order = orders.getOrDefault(pid, new LiquidityOrder());
        order.limit = limit;
        orders.set(pid, order);
        if (!DBUtils.arrayDbContains(ordersList, pid)) {
            ordersList.add(pid);
        }
    }

    @External
    public void setAvailableAmount(BigInteger pid, BigInteger amount) {
        onlyOwner();
        LiquidityOrder order = orders.get(pid);
        order.remaining = amount;
        orders.set(pid, order);
    }

    @External
    public void removeOrder(BigInteger pid) {
        onlyOwner();
        DBUtils.removeFromArraydb(ordersList, pid);
        orders.set(pid, null);
    }

    @External
    public void disburse(Address token, Address recipient, BigInteger amount, @Optional byte[] data) {
        onlyOwner();
        Context.call(token, "transfer", recipient, amount, data);
    }

    @External
    public void disburseICX(Address recipient, BigInteger amount) {
        onlyOwner();
        Context.transfer(recipient, amount);
    }

    @External
    public void withdrawLiquidity(BigInteger pid, BigInteger amount) {
        onlyOwner();
        Context.call(getBalancedDex(), "remove", pid, amount, true);
    }

    @External(readonly = true)
    public BigInteger calculateICXReward(BigInteger pid, BigInteger amount) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) Context.call(getBalancedDex(), "getPoolStats", pid);
        BigInteger base = (BigInteger) stats.get("base");
        BigInteger quote = (BigInteger) stats.get("quote");
        BigInteger totalSupply = (BigInteger) stats.get("total_supply");

        Address baseToken = (Address) stats.get("base_token");
        Address quoteToken = (Address) stats.get("quote_token");
        String baseSymbol = Context.call(String.class, baseToken, "symbol");
        String quoteSymbol = Context.call(String.class, quoteToken, "symbol");

        Address oracle = getBalancedOracle();
        BigInteger baseUSDPrice = Context.call(BigInteger.class, oracle, "getLastPriceInUSD", baseSymbol);
        BigInteger quoteUSDPrice = Context.call(BigInteger.class, oracle, "getLastPriceInUSD", quoteSymbol);
        BigInteger baseDecimals = (BigInteger) stats.get("base_decimals");
        BigInteger quoteDecimals = (BigInteger) stats.get("quote_decimals");
        baseDecimals = pow(BigInteger.TEN, baseDecimals.intValue());
        quoteDecimals = pow(BigInteger.TEN, quoteDecimals.intValue());

        BigInteger baseAmount = base.multiply(amount).divide(totalSupply);
        BigInteger quoteAmount = quote.multiply(amount).divide(totalSupply);

        // USD value of supply in 18 decimals
        BigInteger baseAmountInUSD = baseAmount.multiply(baseUSDPrice).divide(baseDecimals);
        BigInteger quoteAmountInUSD = quoteAmount.multiply(quoteUSDPrice).divide(quoteDecimals);

        // If the pool is perfectly priced, we should have the two USD amount be exactly
        // equal
        BigInteger absDiff = baseAmountInUSD.subtract(quoteAmountInUSD).abs().multiply(POINTS);
        BigInteger avg = baseAmountInUSD.add(quoteAmountInUSD).divide(BigInteger.TWO);
        BigInteger slippage = absDiff.divide(avg);
        Context.require(slippage.compareTo(getLPSlippage()) <= 0, Errors.LP_OVER_SLIPPAGE_LIMIT);

        BigInteger percentageReward = POINTS.add(getSwapReward());
        BigInteger totalUSDValue = baseAmountInUSD.add(quoteAmountInUSD);
        BigInteger totalUSDReward = totalUSDValue.multiply(percentageReward).divide(POINTS);

        BigInteger ICXPriceInUSD = Context.call(BigInteger.class, oracle, "getLastPriceInUSD", "ICX");
        BigInteger ICXReward = totalUSDReward.multiply(EXA).divide(ICXPriceInUSD);

        return ICXReward;
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        only(getBalancedDex());
        String unpackedData = new String(_data);
        Context.require(_value.compareTo(BigInteger.ZERO) > 0, Errors.TOKEN_FALLBACK_ZERO_VALUE);
        Context.require(!unpackedData.equals(""), Errors.TOKEN_FALLBACK_DATA_EMPTY);

        JsonObject json = Json.parse(unpackedData).asObject();

        String method = json.get("method").asString();
        switch (method) {
            case "deposit":
                return;
            case "swap":
                swapLPTokens(_from, _id, _value);
                return;
            default:
                Context.revert(Errors.IRC31_METHOD_NOT_FOUND);
        }
    }

    @Payable
    public void fallback() {
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
    }

    private void swapLPTokens(Address from, BigInteger id, BigInteger value) {
        LiquidityOrder order = orders.get(id);
        Context.require(order != null, Errors.NO_ORDER_EXISTS);
        BigInteger reward = calculateICXReward(id, value);
        order = validateOrder(order, reward);
        orders.set(id, order);
        investedEmissions.set(getInvestedEmissions().add(reward));

        Context.transfer(from, reward);
        LiquidityPurchased(id, value, reward);
    }

    private LiquidityOrder validateOrder(LiquidityOrder order, BigInteger payoutAmount) {
        BigInteger height = BigInteger.valueOf(Context.getBlockHeight());
        BigInteger blockDiff = height.subtract(order.lastPurchaseBlock);
        BigInteger rate = order.limit.divide(orderPeriod.get());
        BigInteger addedAmount = blockDiff.multiply(rate);

        order.remaining = order.limit.min(addedAmount.add(order.remaining));
        Context.require(order.remaining.compareTo(payoutAmount) >= 0, Errors.ORDER_LIMIT_REACHED);
        order.remaining = order.remaining.subtract(payoutAmount);
        order.lastPurchaseBlock = height;

        return order;
    }
}
