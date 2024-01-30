# Introduction
The Network Owned Liquidity contract is the contract that will hold and buy liquidity on the Balanced DEX.
In this contract validators can assign different amount of LP tokens to be bought continuously.
## Overview

### Requirements
* The contract should be owned by governance.
* The contract should swap incoming inflation to bnUSD continuously.
* The contract should expose a method to do smaller swaps in case of slippage issues.
* The contract should allow governance to setup buy orders with limits. Ex buy $100K dollars of sICX/bnUSD liquidity each month.
* The contract should allow anyone to fill liquidity orders.
* The contract should be able to disburse any token via governance.
* The contract should be able to withdraw liquidity via governance.

# Design

## Storage and Structs
```java
public DictDB<BigInteger, LiquidityOrder> orders;

// All below parameters should be configurable by governance only.
public VarDB<BigInteger> orderPeriod; // Microseconds

public VarDB<Address> balancedDex;
public VarDB<Address> bnUSD;
public VarDB<Address> sICX;
public VarDB<Address> balancedOracle;
public VarDB<Address> balancedRouter;

public VarDB<BigInteger> maxSwapSlippage; // Points (0-10000)
public VarDB<BigInteger> swapReward; // Points (0-10000)
public VarDB<BigInteger> lPSlippage; // Points (0-10000)

LiquidityOrder {
    BigInteger limit
    BigInteger period
    BigInteger payoutThisPeriod
}
```

## Methods

```java
/**
 * Swaps ICX amount to bnUSD
 *
 * @param amount The amount of ICX to be swapped
 */
@External
public void swap(BigInteger amount) {
    icxPriceInUSD = balancedOracle.getPriceInUSD("ICX");
    usdAmount = amount*icxPriceInUSD / EXA;
    minReceive = (POINTS-maxSwapSlippage)*usdAmount / POINTS;
    balancedRouter.route(amount, [sICX, bnUSD], minReceive);
}
```

```java
/**
 * Configures a new liquidity order
 *
 * @param pid The poolId on the balanced dex
 * @param limit The max USD limit to purchase for each Order Period
 */
@External
public void configureOrder(BigInteger pid, BigInteger limit) {
    OnlyICONGovernance()
    timestamp = Context.getTimestamp()
    period = orderPeriod.get()

    order = LiquidityOrder {
        limit = limit,
        period = (timestamp/period)*period,
        payoutThisPeriod = 0
    }

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

<!-- ```java
/**
 * Manually supplies liquidity through available tokens via governance
 *
 * @param baseAddress The address of the base token
 * @param baseAmount The amount of of the base token to use
 * @param quoteAddress The address of the quote token
 * @param quoteAmount The amount of of the quote token to use
 * @param slippage The biggest allowed difference between decided price and the actual price when supplying.
 */
public void supplyLiquidity(Address baseAddress, BigInteger baseAmount, Address quoteAddress,
            BigInteger quoteAmount, BigInteger slippage) {

    OnlyICONGovernance();
    pid = balancedDex.getPoolId(baseAddress, quoteAddress);

    supplyPrice = quoteAmount.multiply(EXA).divide(baseAmount);
    dexPrice = balancedDex.getPrice(pid);
    allowedDiff = supplyPrice. * slippage / POINTS;
    assert supplyPrice - allowedDiff < dexprice;
    assert supplyPrice + allowedDiff > dexprice;
    baseAddress.transfer(dex, baseAmount, <tokenDepositData>)
    quoteAddress.transfer(dex, quoteAmount, <tokenDepositData>)
    balancedDex.add(baseAddress, quoteAddress, baseAmount, quoteAmount, true)
}
``` -->

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
 * Calculates the bnUSD rewards gained by swapping 'amount' of LP tokens with a specific pid
 *
 * @param pid The poolId on the balanced dex
 * @param amount The amount of LP tokens to swap
 */
@External(readonly = true)
public BigInteger calculateBnUSDReward(BigInteger pid, BigInteger amount) {
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

    return reward
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
/**
 * Swaps ICX amount to bnUSD
 *
 * @param amount The amount of ICX to be swapped
 */
@External
public void fallback() {
    try {
        swap(Context.getValue());
    } catch error {
        SwapFailed(error)
    }
}
```

```java
/**
 * Receives tokens
 *
 */
@External
public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
}
```



```java
/**
 * Swaps LP tokens for bnUSD
 *
 * @param from The user who supplied the LP tokens
 * @param id The poolId on the balanced dex
 * @param value The amount of lp tokens to be swapped
 */
private void swapLPTokens(Address from, BigInteger id, BigInteger value) {
    order = orders.get(id);
    reward = calculateBnUSDReward(id, value);
    order = validateOrder(order, id);
    orders.set(id, order)
    bnUSD.transfer(from, reward)
    LiquidityPurchased(id, value, reward)
}
```

```java
/**
 * Validates a order so that purchases do not exceed the defined limit.
 *
 */
private LiquidityOrder validateOrder(LiquidityOrder order, BigInteger payoutAmount) {
    orderPeriod = orderPeriod.get();
    currentTime = Context.getTimestamp();
    if (order.period + orderPeriod >=  currentTime) {
        // with integer division this return the current period
        order.period = (currentTime / orderPeriod) * orderPeriod;
        order.payoutThisPeriod = 0;
    }

    order.payoutThisPeriod +=  payoutAmount;
    assert order.payoutThisPeriod <= order.limit

    return order;

}
```

## Eventlogs

```java
@EventLog(indexed = 1)
public void LiquidityPurchased(BigInteger pid, BigInteger lpTokenAmount, BigInteger bnUSDPayout)
```

```java
@EventLog(indexed = 1)
public void SwapFailed(String reason)
```
