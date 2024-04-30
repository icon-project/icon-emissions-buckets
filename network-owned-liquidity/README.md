# Introduction
The Network Owned Liquidity contract is the contract that will hold and buy liquidity on the Balanced DEX.
In this contract validators can assign different amount of LP tokens to be bought continuously.
## Overview

### Requirements
* The contract should be owned by governance.
* The contract should expose a method to do smaller swaps in case of slippage issues.
* The contract should allow governance to setup buy orders with limits. Ex buy $100K dollars of sICX/bnUSD liquidity each month.
* The contract should allow anyone to fill liquidity orders.
* The contract should be able to disburse any token via governance.
* The contract should be able to withdraw liquidity via governance.

# Design

## Storage and Structs
```java
public DictDB<BigInteger, LiquidityOrder> orders;
public VarDB<BigInteger> investedEmissions;

// All below parameters should be configurable by governance only.
public VarDB<BigInteger> orderPeriod; // Blocks

public VarDB<Address> balancedDex;
public VarDB<Address> balancedOracle;

public VarDB<BigInteger> swapReward; // Points (0-10000)
public VarDB<BigInteger> lPSlippage; // Points (0-10000)


LiquidityOrder {
    BigInteger limit
    BigInteger lastPurchaseBlock
    BigInteger remaining
}
```

## Methods

```java
/**
 * Configures a liquidity order
 *
 * @param pid The poolId on the balanced dex
 * @param limit The max ICX limit to purchase for each Order Period
 */
@External
public void configureOrder(BigInteger pid, BigInteger limit) {
    OnlyICONGovernance()
    order = LiquidityOrder {
        limit = limit,
        remaining = 0,
        lastPurchaseBlock = Context.getBlockHeight()
    }

    orders.set(pid, order);
}
```

```java
    /**
     * Configures the remaining amount of a liquidity order.
     *
     * @param pid   The poolId on the balanced dex
     * @param amount The amount to make available for purchase, max limited by the order limit.
     */

    @External
    public void setAvailableAmount(BigInteger pid, BigInteger amount) {
        OnlyICONGovernance();
        order = orders.get(pid);
        order.remaining = amount;
        orders.set(pid, order);
    }
```

```java
/**
 * Removes a liquidity order
 *
 * @param pid The poolId on the balanced dex
 */
@External
public void removeOrder(BigInteger pid) {
    OnlyICONGovernance()
    orders.set(pid, null);
}
```
```java
/**
 * Sends a token to given recipient
 *
 * @param token TThe token address
 * @param recipient The address to receive the token
 * @param amount The amount to send
 * @param data Optional data to use for contract interaction.
 */
@External
public void disburse(Address token, Address recipient, BigInteger amount, @Optional byte[] data) {
    OnlyICONGovernance();
    token.transfer(recipient, amount, data);
}
```

```java
/**
 * Sends ICX to given recipient
 *
 * @param recipient The address to receive ICX
 * @param amount The amount to send
 */
@External
public void disburseICX(Address recipient, BigInteger amount) {
    OnlyICONGovernance();
    Context.transfer(recipient, amount);
}
```

```java
/**
 * Removes liquidity
 *
 * @param pid The poolId on the balanced dex
 * @param amount The amount of LP tokens to withdraw
 */
@External
public void withdrawLiquidity(BigInteger pid, BigInteger amount) {
    OnlyICONGovernance();
    balancedDex.remove(pid, amount);
}
```


```java
/**
 * Calculates the ICX rewards gained by swapping 'amount' of LP tokens with a specific pid
 *
 * @param pid The poolId on the balanced dex
 * @param amount The amount of LP tokens to swap
 */
@External(readonly = true)
public BigInteger calculateICXReward(BigInteger pid, BigInteger amount) {
    stats = balancedDex.getPoolStats(pid);

    baseAmount = stats["base"]*amount  / stats["total_supply"];
    quoteAmount = stats["quote"]*amount  / stats["total_supply"];

    baseSymbol = stats["baseToken"].symbol()
    baseUSDPrice = balancedOracle.getPriceInUSD(baseSymbol);


    quoteSymbol = stats["quoteToken"].symbol()
    quoteUSDPrice = balancedOracle.getPriceInUSD(quoteSymbol);

    // USD value of supply in 18 decimals
    baseAmountInUSD = baseAmount*baseUSDPrice / stats["baseDecimals"];
    quoteAmountInUSD = quoteAmount*quoteUSDPrice / stats["quoteDecimals"];

    // If the pool is perfectly priced, we should have the two USD amount be exactly equal
    absDiff = abs(baseAmountInUSD-quoteAmountInUSD) * POINTS;
    avg = (baseAmountInUSD+quoteAmountInUSD) / 2
    slippage = absDiff / avg
    assert slippage < lPSlippage

    reward = (baseAmountInUSD+quoteAmountInUSD)*(POINTS+swapReward) / POINTS

    rewardsInICX = reward * 10**18 / balancedOracle.getPriceInUSD("ICX")

    return rewardsInICX
}
```


```java
/**
 * Receives LP tokens from the balanced dex, If the method is swap then calculate payout to user.
 *
 */
@External
public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
    OnlyBalancedDex();
    data = json(_data);
    assert unpackedData != empty
    switch data["method"]:
        case: "deposit"
            return
        case: "swap":
            swapLPTokens(_from, _id, _value);
            return
}
```


```java
@External
public void fallback() {
}
```

```java
@External
public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
}
```


```java
/**
 * Swaps LP tokens for ICX
 *
 * @param from The user who supplied the LP tokens
 * @param id The poolId on the balanced dex
 * @param value The amount of lp tokens to be swapped
 */
private void swapLPTokens(Address from, BigInteger id, BigInteger value) {
    order = orders.get(id);
    reward = calculateICXReward(id, value);
    order = validateOrder(order, reward);
    orders.set(id, order)
    Context.transfer(from, reward)
    investedEmissions += reward
    LiquidityPurchased(id, value, reward)
}
```

```java
/**
 * Validates a order so that purchases do not exceed the defined limit.
 *
 */
private LiquidityOrder validateOrder(LiquidityOrder order, BigInteger payoutAmount) {
    blockDiff = Context.getBlockHeight() - order.lastPurchaseBlock;
    rate = limit/orderPeriod;
    addedAmount = blockDiff * rate;
    order.remaining =  order.limit.min(addedAmount + order.remaining);
    assert order.remaining >= payoutAmount
    order.remaining -= payoutAmount

    return order;
}
```

## Eventlogs

```java
@EventLog(indexed = 1)
void LiquidityPurchased(BigInteger pid, BigInteger lpTokenAmount, BigInteger payout)
```
