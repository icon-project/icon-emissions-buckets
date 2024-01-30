package icon.inflation.score.interfaces;

import java.math.BigInteger;

import icon.inflation.score.structs.LiquidityOrder;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public interface INetworkOwnedLiquidity {
    @EventLog(indexed = 1)
    void LiquidityPurchased(BigInteger pid, BigInteger lpTokenAmount, BigInteger bnUSDPayout);

    @EventLog(indexed = 1)
    void SwapFailed(String reason);

    @External(readonly = true)
    String name();

    @External(readonly = true)
    LiquidityOrder[] getOrders();

    @External(readonly = true)
    BigInteger getOrderPeriod();

    @External
    void setOrderPeriod(BigInteger _orderPeriod);

    @External(readonly = true)
    Address getBalancedDex();

    @External
    void setBalancedDex(Address _balancedDex);

    @External(readonly = true)
    Address getBnUSD();

    @External
    void setBnUSD(Address _bnUSD);

    @External(readonly = true)
    Address getsICX();

    @External
    void setsICX(Address _sICX);

    @External(readonly = true)
    Address getBalancedOracle();

    @External
    void setBalancedOracle(Address _balancedOracle);

    @External(readonly = true)
    Address getBalancedRouter();

    @External
    void setBalancedRouter(Address _balancedRouter);

    @External(readonly = true)
    BigInteger getMaxSwapSlippage();

    @External
    void setMaxSwapSlippage(BigInteger _maxSwapSlippage);

    @External(readonly = true)
    BigInteger getSwapReward();

    @External
    void setSwapReward(BigInteger _swapReward);

    @External(readonly = true)
    BigInteger getLPSlippage();

    @External
    void setLPSlippage(BigInteger _lPSlippage);

    /**
     * Swaps ICX amount to bnUSD
     *
     * @param amount The amount of ICX to be swapped
     */
    @External
    void swap(BigInteger amount);

    /**
     * Configures a new liquidity order
     *
     * @param pid   The poolId on the balanced dex
     * @param limit The max USD limit to purchase for each Order Period
     */
    @External
    void configureOrder(BigInteger pid, BigInteger limit);

    /**
     * Removes a liquidity order
     *
     * @param pid The poolId on the balanced dex
     */
    @External
    void removeOrder(BigInteger pid);

    /**
     * Sends a token to given recipient
     *
     * @param token     TThe token address
     * @param recipient The address to receive the token
     * @param amount    The amount to send
     * @param data      Optional data to use for contract interaction.
     */
    @External
    void disburse(Address token, Address recipient, BigInteger amount, @Optional byte[] data);

    /**
     * Sends ICX to given recipient
     *
     * @param recipient The address to receive ICX
     * @param amount    The amount to send
     */
    @External
    void disburseICX(Address recipient, BigInteger amount);

    /**
     * Removes liquidity
     *
     * @param pid    The poolId on the balanced dex
     * @param amount The amount of LP tokens to withdraw
     */
    @External
    void withdrawLiquidity(BigInteger pid, BigInteger amount);

    /**
     * Calculates the bnUSD rewards gained by swapping 'amount' of LP tokens with a
     * specific pid
     *
     * @param pid    The poolId on the balanced dex
     * @param amount The amount of LP tokens to swap
     */
    @External(readonly = true)
    BigInteger calculateBnUSDReward(BigInteger pid, BigInteger amount);

    /**
     * Receives LP tokens from the balanced dex, If the method is swap then
     * calculate payout to user.
     *
     */
    @External
    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data);

    /**
     * Swaps ICX amount to bnUSD
     *
     * @param amount The amount of ICX to be swapped
     */
    @External
    void fallback();

    /**
     * Receives tokens
     *
     */
    @External
    void tokenFallback(Address _from, BigInteger _value, byte[] _data);
}
