# IRelay setup
1. Deploy buckets contract, takes no parameters
2. Transfer ownership to Governance address
3. Create a proposal with the following parameters
   1. setNetworkScore("relay", \<address\>)
   2.
```
setRewardFundAllocation2([
    {"name": "Iprep",  "value" : 4530},
    {"name": "Ivoter", "value" : 0},
    {"name": "Icps",   "value": 250},
    {"name": "Irelay", "value" : 5000},
    {"name": "Iwage",  "value" : 220}
   ]
)
```

The contract should now be receiving inflation.

# Buckets setup
## Insurance setup
1. Deploy insurance contract, takes no parameters
2. Transfer ownership to Governance address

## Savings-rate setup
1. Deploy savings-rate contract
   1. Address _staking (see address list below)
   2. Address _balancedReceiver (see address list below)
2. Transfer ownership to Governance address

## Network owned liquidity setup
1. Deploy Network owned liquidity contract
   1. Address _balancedDex
   2. Address _bnUSD
   3. Address _sICX
   4. Address _balancedOracle,
   5. Address _balancedRouter
2. Transfer ownership to Governance address

## Governance vote
Call vote with two actions
1. On IRelay:

```
configureBuckets(
    [
        {"name": "Insurance", "address": \<address\>, "share": "40000000000000000"},
        {"name": "Savings rate", "address": \<address\>, "share": "40000000000000000"},
        {"name": "Network owned liquidity", "address": \<address\>, "share": "920000000000000000"}
    ]
)
```
(Share values are not a reflection of mainnet for now)

2. On Network owned liquidity:
    configureOrder(2, 100000000000000000000000)
    Configures $100000 monthly purchase of sICX bnUSD liquidity


# Lisbon addresses
_balancedDex = cx7a90ed2f781876534cf1a04be34e4af026483de4

_bnUSD = cx87f7f8ceaa054d46ba7343a2ecd21208e12913c6

_sICX = cx2d013cb55781fb54b81d629aa4b611be2daec564

_balancedOracle = cxeda795dcd69fe3d2e6c88a6473cdfe5532a3393e

_balancedRouter = cx2576925d931f3be8ff195914c10a87da2094c5e5

_balancedReceiver = cx223bb0520fb6ac4faca4a59d4cca77fbe3ebe3c1

_staking = cx442b5db251e4f27371cc5b8479a4672c0e6ae32d


# Berlin addresses
_balancedDex = cx9044771dad80611ee747ffce21949dc3f33f0948

_bnUSD = cxd06f80e28e989a67e297799ab1fb501cdddc2b4d

_sICX = cxb7d63658e3375f701af9d420ea351d0736760634

_balancedOracle = cx2dc21a1b7f602d49bfe64a49970fe02153ddf487

_balancedRouter = cxfcfb55f5b9c066b107c4a16cb2ea3d9c7ca97f30

_balancedReceiver = cx025d00ea1eb4f30d6b023828e2d1dc02a07e9c6e

_staking = cxe41e5f42b982eb88f80381ae776d1aac09b74885
