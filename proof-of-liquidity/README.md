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

LiquidityDistribution {
    BigInteger share;
    // Source name in balanced rewards contract
    String source;
}

// All below dbs should be configurable by governance only.
ArrayDB<LiquidityDistribution> distribution;
VarDB<Address> staking;
VarDB<Address> sICX;
VarDB<Address> balancedRewards;


BigInteger TOTAL_SHARE = EXA;

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
        distribute();
    } catch (Exception e) {
    }
}
```

```java
/**
 *
 * Returns the current distribution config
*/
@External(readonly = true)
public LiquidityDistribution[] getDistributions() {
    return distribution
}

```

```java
/**
 * Allocated the rewards to the balanced rewards contract for the configured distributions
 *
 * @param amount The amount to stake and send
 */
@External
public void distribute() {
    BigInteger balance = Context.getBalance(Context.getAddress());
    BigInteger sICXAmount = staking.stakeICX(amount, balancedReceiver)
    JsonArray data = new JsonArray();
    for (dist: distribution) {
        BigInteger share = dist.share.multiply(sICXAmount).divide(TOTAL_SHARE);
        data.add(
            '{
                "source": dist.source
                "amount": share
            }'
        );
    }

    sICX.transfer(balancedRewards, sICXAmount, jsonData)
}


```java
/**
 *
 * Configures the rewards
 *
 * @param _distribution The source names and their share, which adds up to 10**18
 */
@External
public void configureDistributions(LiquidityDistribution[] _distribution) {
    onlyOwner();
    require(sum(_distribution.share) == TOTAL_SHARE)
}

```

