# ICON Insurance Fund:
This contract will store ICX that can be used to compensate damaged parties in the case of an exploit of ICON, Balanced, or any other product critical to ICON’s infrastructure.
Releasing these funds will be at the discretion of validators. Unless released, these funds are locked and non-circulating.

## Overview
The insurance fund implements a transfer method only accessible by the owner of the contracts, which in a live environment should be the governance contract.

```java
/**
 * Transfers 'amount' ICX to 'to'
 *
 * @param to address which receives payout.
 * @param amount to pay out.
 */
@External
public void transfer(Address to, BigInteger amount) {
    onlyOwner();
    Context.transfer(to, amount);
}
```