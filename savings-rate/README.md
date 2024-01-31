# Introduction
The savings rate contract stakes ICX and relays it to the balanced handler for the savings rate given to bnUSD stakers.
## Overview

### Requirements
* The contract should be owned by governance.
* The contract should swap incoming inflation to sICX and distribute to balanced target contract.
* The contract should be able to manually stake in case of failure during fallback.

# Design

## Storage and Structs
```java
public DictDB<BigInteger, LiquidityOrder> orders;

// All below parameters should be configurable by governance only.
public VarDB<Address> staking;
public VarDB<Address> balancedReceiver;

```

## Methods

```java
/**
 * Tries to stake the whole ICX balance to the balanced receiver
 *
 */
 @Payable
public void fallback() {
    try {
        stakeAndSend(Context.getBalance(Context.getAddress()));
    } catch (Exception e) {
        StakeFailed();
    }
}
```

```java
/**
 * Stakes 'amount' to the balanced receiver
 *
 * @param amount The amount to stake and send
 */
@External
public void stakeAndSend(BigInteger amount) {
    staking.stakeICX(amount, balancedReceiver)
}
```


```java
@EventLog(indexed = 1)
public void StakeFailed()
```
