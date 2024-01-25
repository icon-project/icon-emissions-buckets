package icon.inflation.score.nol;

import java.math.BigInteger;
import java.util.Map;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import icon.inflation.score.structs.LiquidityOrder;
import icon.inflation.score.util.Checks;
import icon.inflation.test.MockContract;
import icon.inflation.test.interfaces.*;
import score.Address;
import score.UserRevertedException;

import static icon.inflation.score.util.Constants.EXA;
import static icon.inflation.score.util.Constants.MICRO_SECONDS_IN_A_MONTH;
import static icon.inflation.score.util.Constants.MICRO_SECONDS_IN_A_SECOND;
import static icon.inflation.score.util.Constants.POINTS;
import static icon.inflation.test.Utils.expectErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NetworkOwnedLiquidityTest extends TestBase {
    protected final ServiceManager sm = getServiceManager();
    protected final Account governance = sm.createAccount();

    protected Score networkOwnedLiquidity;

    protected MockContract<BalancedDex> dex;
    protected MockContract<IRC2> bnUSD;
    protected MockContract<IRC2> sICX;
    protected MockContract<IRC2> sARCH;
    protected MockContract<BalancedOracle> oracle;
    protected MockContract<Router> router;

    @BeforeEach
    public void setup() throws Exception {
        dex = new MockContract<>(BalancedDexScoreInterface.class, BalancedDex.class, sm, governance);
        bnUSD = new MockContract<>(IRC2ScoreInterface.class, IRC2.class, sm, governance);
        sICX = new MockContract<>(IRC2ScoreInterface.class, IRC2.class, sm, governance);
        sARCH = new MockContract<>(IRC2ScoreInterface.class, IRC2.class, sm, governance);
        oracle = new MockContract<>(BalancedOracleScoreInterface.class, BalancedOracle.class, sm, governance);
        router = new MockContract<>(RouterScoreInterface.class, Router.class, sm, governance);

        when(bnUSD.mock.symbol()).thenReturn("bnUSD");
        when(sICX.mock.symbol()).thenReturn("sICX");
        when(sARCH.mock.symbol()).thenReturn("sARCH");

        networkOwnedLiquidity = sm.deploy(governance, NetworkOwnedLiquidity.class, dex.getAddress(), bnUSD.getAddress(),
                sICX.getAddress(), oracle.getAddress(), router.getAddress());
    }

    @Test
    public void configureOrder_add() {
        // Arrange
        BigInteger pid1 = BigInteger.ONE;
        BigInteger pid2 = BigInteger.TWO;
        BigInteger limit1 = BigInteger.valueOf(10000);
        BigInteger limit2 = BigInteger.valueOf(20000);

        // Act
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid1, limit1);
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid2, limit2);
        // Assert
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");

        assertEquals(2, orders.length);

        assertEquals(pid1, orders[0].pid);
        assertEquals(limit1, orders[0].limit);
        assertEquals(getCurrentPeriod(), orders[0].period);
        assertEquals(BigInteger.ZERO, orders[0].payoutThisPeriod);

        assertEquals(pid2, orders[1].pid);
        assertEquals(limit2, orders[1].limit);
        assertEquals(getCurrentPeriod(), orders[1].period);
        assertEquals(BigInteger.ZERO, orders[1].payoutThisPeriod);
    }

    @Test
    public void configureOrder_reconfigure() {
        // Arrange
        BigInteger pid1 = BigInteger.ONE;
        BigInteger limit1 = BigInteger.valueOf(10000);
        BigInteger limit2 = BigInteger.valueOf(20000);

        // Act
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid1, limit1);
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid1, limit2);

        // Assert
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");

        assertEquals(1, orders.length);

        assertEquals(pid1, orders[0].pid);
        assertEquals(limit2, orders[0].limit);
        assertEquals(getCurrentPeriod(), orders[0].period);
        assertEquals(BigInteger.ZERO, orders[0].payoutThisPeriod);
    }

    @Test
    public void configureOrder_remove() {
        // Arrange
        BigInteger pid1 = BigInteger.ONE;
        BigInteger pid2 = BigInteger.TWO;
        BigInteger limit1 = BigInteger.valueOf(10000);
        BigInteger limit2 = BigInteger.valueOf(20000);

        // Act
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid1, limit1);
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid2, limit2);
        networkOwnedLiquidity.invoke(governance, "removeOrder", pid1);

        // Assert
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");

        assertEquals(1, orders.length);

        assertEquals(pid2, orders[0].pid);
        assertEquals(limit2, orders[0].limit);
        assertEquals(getCurrentPeriod(), orders[0].period);
        assertEquals(BigInteger.ZERO, orders[0].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_simple() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, EXA.multiply(EXA));

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        BigInteger baseUSDPrice = EXA.multiply(BigInteger.TWO);
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // this results in a 40 bnUSD + reward payout since price is exactly correct'
        // Default rewards is 1% so total = 40.4 bnUSD
        BigInteger expectedRewards = BigInteger.valueOf(404).multiply(BigInteger.TEN.pow(17));

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);

        // Assert
        verify(bnUSD.mock, times(2)).transfer(user.getAddress(), expectedRewards, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards.add(expectedRewards), orders[0].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_multipleOrders() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid1 = BigInteger.ONE;
        BigInteger pid2 = BigInteger.TWO;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid1, EXA.multiply(EXA));
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid2, EXA.multiply(EXA));

        Address baseToken1 = sICX.getAddress();
        Address baseToken2 = sARCH.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base1 = BigInteger.valueOf(1000).multiply(EXA);
        // 1 sARCH = 0.5 dollar
        BigInteger base2 = BigInteger.valueOf(4000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        // sICX = 2.01$
        BigInteger base1USDPrice = BigInteger.valueOf(201).multiply(BigInteger.TEN.pow(16));
        // sARCH = 0.495$
        BigInteger base2USDPrice = BigInteger.valueOf(495).multiply(BigInteger.TEN.pow(15));
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats1 = Map.of(
                "baseToken", baseToken1,
                "quoteToken", quoteToken,
                "base", base1,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);
        Map<String, Object> stats2 = Map.of(
                "baseToken", baseToken2,
                "quoteToken", quoteToken,
                "base", base2,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

            when(oracle.mock.getPriceInUSD("sICX")).thenReturn(base1USDPrice);
            when(oracle.mock.getPriceInUSD("sARCH")).thenReturn(base2USDPrice);
            when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
            when(dex.mock.getPoolStats(pid1)).thenReturn(stats1);
            when(dex.mock.getPoolStats(pid2)).thenReturn(stats2);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // this results in a 20 + 10*2.01 = 40.1
        // Default rewards is 1% so total = 40.501 bnUSD
        BigInteger expectedRewards1 = BigInteger.valueOf(40501).multiply(BigInteger.TEN.pow(15));

        // User send 1% of supply, this means 40 sARCH and 20 bnUSD value
        // this results in a 20 + 40*0.495 = 39.8
        // Default rewards is 1% so total = 40.198 bnUSD
        BigInteger expectedRewards2 = BigInteger.valueOf(40198).multiply(BigInteger.TEN.pow(15));

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid1, amount,
                swapData);
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid2, amount,
                swapData);

        // Assert
        verify(bnUSD.mock).transfer(user.getAddress(), expectedRewards1, null);
        verify(bnUSD.mock).transfer(user.getAddress(), expectedRewards2, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards1, orders[0].payoutThisPeriod);
        assertEquals(expectedRewards2, orders[1].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_overSlippageLimit() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, EXA.multiply(EXA));

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        // default limit is 3%
        // 1 sICX = 1.93 dollar
        BigInteger baseUSDPrice = BigInteger.valueOf(193).multiply(BigInteger.TEN.pow(16));
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // Act
        Executable overSlippageLimit = () -> networkOwnedLiquidity.invoke(dex.account, "onIRC31Received",
                user.getAddress(), user.getAddress(), pid, amount, swapData);

        // Assert
        expectErrorMessage(overSlippageLimit, Errors.LP_OVER_SLIPPAGE_LIMIT);
    }

    @Test
    public void swapLPTokens_aboveLimit_periodUpdate() throws Throwable {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        BigInteger baseUSDPrice = EXA.multiply(BigInteger.TWO);
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // this results in a 40 bnUSD + reward payout since price is exactly correct'
        // Default rewards is 1% so total = 40.4 bnUSD
        BigInteger expectedRewards = BigInteger.valueOf(404).multiply(BigInteger.TEN.pow(17));
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, expectedRewards.add(BigInteger.ONE));

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);
        Executable overLimit = () -> networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(),
                user.getAddress(), pid, amount, swapData);
        expectErrorMessage(overLimit, Errors.ORDER_LIMIT_REACHED);
        BigInteger monthInBlocks = MICRO_SECONDS_IN_A_MONTH.divide(MICRO_SECONDS_IN_A_SECOND).divide(BigInteger.TWO);
        sm.getBlock().increase(monthInBlocks.longValue());
        overLimit.execute();

        // Assert
        verify(bnUSD.mock, times(2)).transfer(user.getAddress(), expectedRewards, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards, orders[0].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_aboveLimit() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        BigInteger baseUSDPrice = EXA.multiply(BigInteger.TWO);
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // this results in a 40 bnUSD + reward payout since price is exactly correct'
        // Default rewards is 1% so total = 40.4 bnUSD
        BigInteger expectedRewards = BigInteger.valueOf(404).multiply(BigInteger.TEN.pow(17));
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, expectedRewards.add(BigInteger.ONE));

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);
        Executable overLimit = () -> networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(),
                user.getAddress(), pid, amount, swapData);

        // Assert
        expectErrorMessage(overLimit, Errors.ORDER_LIMIT_REACHED);
    }

    @Test
    public void swapLPTokens_non18Decimal() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, EXA.multiply(EXA));

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        BigInteger baseUSDPrice = EXA.multiply(BigInteger.TWO);
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // this results in a 40 bnUSD + reward payout since price is exactly correct'
        // Default rewards is 1% so total = 40.4 bnUSD
        BigInteger expectedRewards = BigInteger.valueOf(404).multiply(BigInteger.TEN.pow(17));

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);

        // Assert
        verify(bnUSD.mock, times(2)).transfer(user.getAddress(), expectedRewards, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards.add(expectedRewards), orders[0].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_nonUSDQuote() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, EXA.multiply(EXA));

        Address baseToken = sARCH.getAddress();
        Address quoteToken = sICX.getAddress();
        // 2 sARCH = 1 ICX
        BigInteger base = BigInteger.valueOf(3000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(1000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        // 1 sARCH = 0.495 Dollar
        // 1 sICX = 1.5 Dollar
        BigInteger baseUSDPrice = BigInteger.valueOf(495).multiply(BigInteger.TEN.pow(15));
        BigInteger quoteUSDPrice = BigInteger.valueOf(15).multiply(BigInteger.TEN.pow(17));

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sARCH")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 20 sARCH and 10 sICX value
        // 10 ICX  = 15 USD
        // 30 sARCH = 0.49*3 = 14.85 USD
        // Reward  is then 29.85 + reward
        BigInteger rewardsPercentage = (BigInteger) networkOwnedLiquidity.call("getSwapReward");
        BigInteger expectedRewards = BigInteger.valueOf(2985).multiply(BigInteger.TEN.pow(16))
                .multiply(rewardsPercentage.add(POINTS)).divide(POINTS);

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);

        // Assert
        verify(bnUSD.mock).transfer(user.getAddress(), expectedRewards, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards, orders[0].payoutThisPeriod);
    }

    @Test
    public void swapLPTokens_missPriced() {
        // Arrange
        Account user = sm.createAccount();
        byte[] swapData = "{\"method\":\"swap\"}".getBytes();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        BigInteger pid = BigInteger.ONE;
        networkOwnedLiquidity.invoke(governance, "configureOrder", pid, EXA.multiply(EXA));

        Address baseToken = sICX.getAddress();
        Address quoteToken = bnUSD.getAddress();
        // 1 sICX = 2 dollar
        BigInteger base = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger quote = BigInteger.valueOf(2000).multiply(EXA);

        BigInteger totalSupply = BigInteger.valueOf(1000).multiply(EXA);
        BigInteger baseDecimals = BigInteger.valueOf(18);
        BigInteger quoteDecimals = BigInteger.valueOf(18);
        // 1 sICX = 1.98 dollar
        BigInteger baseUSDPrice = BigInteger.valueOf(198).multiply(BigInteger.TEN.pow(16));
        BigInteger quoteUSDPrice = EXA;

        Map<String, Object> stats = Map.of(
                "baseToken", baseToken,
                "quoteToken", quoteToken,
                "base", base,
                "quote", quote,
                "total_supply", totalSupply,
                "baseDecimals", baseDecimals,
                "quoteDecimals", quoteDecimals);

        when(oracle.mock.getPriceInUSD("sICX")).thenReturn(baseUSDPrice);
        when(oracle.mock.getPriceInUSD("bnUSD")).thenReturn(quoteUSDPrice);
        when(dex.mock.getPoolStats(pid)).thenReturn(stats);

        // User send 1% of supply, this means 10 ICX and 20 bnUSD value
        // 10 ICX *1.98 = 19.8 USD
        // this results in a 39.8 bnUSD + reward payout
        BigInteger rewardsPercentage = (BigInteger) networkOwnedLiquidity.call("getSwapReward");
        BigInteger expectedRewards = BigInteger.valueOf(398).multiply(BigInteger.TEN.pow(17))
                .multiply(rewardsPercentage.add(POINTS)).divide(POINTS);

        // Act
        networkOwnedLiquidity.invoke(dex.account, "onIRC31Received", user.getAddress(), user.getAddress(), pid, amount,
                swapData);

        // Assert
        verify(bnUSD.mock).transfer(user.getAddress(), expectedRewards, null);
        LiquidityOrder[] orders = (LiquidityOrder[]) networkOwnedLiquidity.call("getOrders");
        assertEquals(expectedRewards, orders[0].payoutThisPeriod);
    }

    @Test
    public void disburse() {
        // Arrange
        BigInteger amount = BigInteger.TEN;
        Address to = sm.createAccount().getAddress();
        byte[] data = "test".getBytes();

        // Act
        networkOwnedLiquidity.invoke(governance, "disburse", bnUSD.getAddress(), to, amount, data);

        // Assert
        verify(bnUSD.mock).transfer(to, amount, data);
    }

    @Test
    public void disburseICX() {
        // Arrange
        BigInteger amount = BigInteger.TEN;
        Account to = sm.createAccount();
        networkOwnedLiquidity.getAccount().addBalance(amount);

        // Act
        networkOwnedLiquidity.invoke(governance, "disburseICX", to.getAddress(), amount);

        // Assert
        assertEquals(BigInteger.ZERO, networkOwnedLiquidity.getAccount().getBalance());
        assertEquals(amount, to.getBalance());
    }

    @Test
    public void withdrawLiquidity() {
        // Arrange
        BigInteger pid = BigInteger.ONE;
        BigInteger amount = BigInteger.TEN;

        // Act
        networkOwnedLiquidity.invoke(governance, "withdrawLiquidity", pid, amount);

        // Assert
        verify(dex.mock).remove(pid, amount, true);
    }

    @Test
    public void fallback() {
        // Arrange
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        governance.addBalance(amount);
        // add some remaining balance that should not swapped on fallback
        networkOwnedLiquidity.getAccount().addBalance(BigInteger.ONE);
        BigInteger ICXBnUSDPrice = EXA.multiply(BigInteger.TWO); // 1 ICX = $2
        when(oracle.mock.getPriceInUSD("ICX")).thenReturn(ICXBnUSDPrice);
        BigInteger slippage = (BigInteger) networkOwnedLiquidity.call("getMaxSwapSlippage");
        BigInteger USDValue = amount.multiply(ICXBnUSDPrice).divide(EXA);
        BigInteger minReceive = USDValue.multiply(POINTS.subtract(slippage)).divide(POINTS);

        // Act
        networkOwnedLiquidity.invoke(governance, amount, "fallback");

        // Assert
        verify(router.mock).route(new Address[] { sICX.getAddress(), bnUSD.getAddress() }, minReceive, null);
        assertEquals(BigInteger.ONE, networkOwnedLiquidity.getAccount().getBalance());
    }

    @Test
    public void fallback_failedSwap() {
        // Arrange
        NetworkOwnedLiquidity _spy = (NetworkOwnedLiquidity) spy(networkOwnedLiquidity.getInstance());
        networkOwnedLiquidity.setInstance(_spy);

        BigInteger amount = BigInteger.TEN.multiply(EXA);
        governance.addBalance(amount);

        doThrow(UserRevertedException.class).when(_spy).swap(amount);

        // Act
        networkOwnedLiquidity.invoke(governance, amount, "fallback");

        // Assert
        verify(_spy).SwapFailed(null);
    }

    @Test
    public void ICXSwap() {
        // Arrange
        Account user = sm.createAccount();
        BigInteger amount = BigInteger.TEN.multiply(EXA);
        networkOwnedLiquidity.getAccount().addBalance(amount);
        networkOwnedLiquidity.getAccount().addBalance(amount);

        BigInteger ICXBnUSDPrice = EXA.multiply(BigInteger.TWO); // 1 ICX = $2
        when(oracle.mock.getPriceInUSD("ICX")).thenReturn(ICXBnUSDPrice);
        BigInteger slippage = (BigInteger) networkOwnedLiquidity.call("getMaxSwapSlippage");
        BigInteger USDValue = amount.multiply(ICXBnUSDPrice).divide(EXA);
        BigInteger minReceive = USDValue.multiply(POINTS.subtract(slippage)).divide(POINTS);

        // Act
        networkOwnedLiquidity.invoke(user, "swap", amount);

        // Assert
        verify(router.mock).route(new Address[] { sICX.getAddress(), bnUSD.getAddress() }, minReceive, null);
        assertEquals(amount, networkOwnedLiquidity.getAccount().getBalance());
    }

    @Test
    public void testPermissions() {
        _testPermission("setOrderPeriod", Checks.Errors.ONLY_OWNER, BigInteger.ONE);
        _testPermission("setBalancedDex", Checks.Errors.ONLY_OWNER, dex.getAddress());
        _testPermission("setBnUSD", Checks.Errors.ONLY_OWNER, dex.getAddress());
        _testPermission("setsICX", Checks.Errors.ONLY_OWNER, dex.getAddress());
        _testPermission("setBalancedOracle", Checks.Errors.ONLY_OWNER, dex.getAddress());
        _testPermission("setBalancedRouter", Checks.Errors.ONLY_OWNER, dex.getAddress());
        _testPermission("setMaxSwapSlippage", Checks.Errors.ONLY_OWNER, BigInteger.ONE);
        _testPermission("setSwapReward", Checks.Errors.ONLY_OWNER, BigInteger.ONE);
        _testPermission("setLPSlippage", Checks.Errors.ONLY_OWNER, BigInteger.ONE);

        _testPermission("configureOrder", Checks.Errors.ONLY_OWNER, BigInteger.ONE, BigInteger.ONE);
        _testPermission("removeOrder", Checks.Errors.ONLY_OWNER, BigInteger.ONE);
        _testPermission("disburse", Checks.Errors.ONLY_OWNER, dex.getAddress(), dex.getAddress(), BigInteger.ONE,
                new byte[0]);
        _testPermission("disburseICX", Checks.Errors.ONLY_OWNER, dex.getAddress(), BigInteger.ONE);
        _testPermission("withdrawLiquidity", Checks.Errors.ONLY_OWNER, BigInteger.ONE, BigInteger.ONE);
        _testPermission("onIRC31Received", Checks.Errors.ONLY(dex.getAddress()), bnUSD.getAddress(), dex.getAddress(),
                BigInteger.ONE, BigInteger.ONE, new byte[0]);
    }

    private void _testPermission(String method, String error, Object... params) {
        Account dummy = sm.createAccount();
        Executable call = () -> networkOwnedLiquidity.invoke(dummy, method, params);
        expectErrorMessage(call, error);
    }

    private BigInteger getCurrentPeriod() {
        BigInteger period = (BigInteger) networkOwnedLiquidity.call("getOrderPeriod");
        return BigInteger.valueOf(sm.getBlock().getTimestamp()).divide(period).multiply(period);
    }
}