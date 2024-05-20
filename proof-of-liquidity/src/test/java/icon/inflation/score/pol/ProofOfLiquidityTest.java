package icon.inflation.score.pol;

import java.math.BigInteger;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import icon.inflation.score.structs.LiquidityDistribution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import icon.inflation.score.util.Checks;
import icon.inflation.test.MockContract;
import icon.inflation.test.interfaces.IRC2;
import icon.inflation.test.interfaces.IRC2ScoreInterface;
import icon.inflation.test.interfaces.Staking;
import icon.inflation.test.interfaces.StakingScoreInterface;
import score.Address;
import score.Context;

import static icon.inflation.test.Utils.expectErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static icon.inflation.score.util.Constants.EXA;

public class ProofOfLiquidityTest extends TestBase {
    protected final ServiceManager sm = getServiceManager();
    protected final Account governance = sm.createAccount();

    protected Score pol;

    protected MockContract<IRC2> sICX;
    protected MockContract<Staking> staking;
    protected Address rewards = sm.createAccount().getAddress();

    @BeforeEach
    public void setup() throws Exception {
        sICX = new MockContract<>(IRC2ScoreInterface.class, IRC2.class, sm, governance);
        staking = new MockContract<>(StakingScoreInterface.class, Staking.class, sm, governance);

        pol = sm.deploy(governance, ProofOfLiquidity.class, staking.getAddress(), sICX.getAddress(), rewards);
    }

    @Test
    public void configureDistributions_onlyOwner() {
        // Arrange
        Account nonOwner = sm.createAccount();

        // Act
        Executable nonOwnerConfigure = () -> pol.invoke(nonOwner, "configureDistributions",
                (Object) new LiquidityDistribution[0]);

        // Assert
        expectErrorMessage(nonOwnerConfigure, Checks.Errors.ONLY_OWNER);
    }

    @Test
    public void configureDistributions_invalidSum() {
        // Arrange
        LiquidityDistribution eth = newDist("ETH/sICX",
                BigInteger.valueOf(95).multiply(EXA).divide(BigInteger.valueOf(100))); // 95%
        LiquidityDistribution avax = newDist("AVAX/sICX",
                BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        LiquidityDistribution bnb = newDist("BNB/sICX",
                BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        LiquidityDistribution[] dist = new LiquidityDistribution[] { eth, avax, bnb };

        // Act
        Executable invalidSum = () -> pol.invoke(governance, "configureDistributions", (Object) dist);

        // Assert
        expectErrorMessage(invalidSum, Errors.INVALID_SUM);
    }

    @Test
    public void configureDistributions_negativeShare() {
        // Arrange
        LiquidityDistribution eth = newDist("ETH/sICX",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        LiquidityDistribution avax = newDist("AVAX/sICX",
                BigInteger.valueOf(15).multiply(EXA).divide(BigInteger.valueOf(100))); // 15%
        LiquidityDistribution bnb = newDist("BNB/sICX",
                BigInteger.valueOf(5).multiply(EXA).divide(BigInteger.valueOf(100)).negate()); // -5%

        LiquidityDistribution[] dist = new LiquidityDistribution[] { eth, avax, bnb };

        // Act
        Executable negativeShare = () -> pol.invoke(governance, "configureDistributions", (Object) dist);

        // Assert
        expectErrorMessage(negativeShare, Errors.NEGATIVE_PERCENTAGE);
    }

    @Test
    public void configureDistributions() {
        // Arrange
        LiquidityDistribution eth = newDist("ETH/sICX",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        LiquidityDistribution avax = newDist("AVAX/sICX",
                BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        LiquidityDistribution bnb = newDist("BNB/sICX",
                BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        LiquidityDistribution[] dist = new LiquidityDistribution[] { eth, avax, bnb };

        // Act
        pol.invoke(governance, "configureDistributions", (Object) dist);

        // Assert
        LiquidityDistribution[] configuredBuckets = (LiquidityDistribution[]) pol.call("getDistributions");
        distEq(dist[0], configuredBuckets[0]);
        distEq(dist[0], configuredBuckets[0]);
        distEq(dist[0], configuredBuckets[0]);
    }

    @Test
    public void distribute_emptyBalance() {
        // Arrange
        LiquidityDistribution eth = newDist("ETH/sICX",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        LiquidityDistribution avax = newDist("AVAX/sICX",
                BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        LiquidityDistribution bnb = newDist("BNB/sICX",
                BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        LiquidityDistribution[] dist = new LiquidityDistribution[] { eth, avax, bnb };

        pol.invoke(governance, "configureDistributions", (Object) dist);

        // Act
        pol.getAccount().subtractBalance(pol.getAccount().getBalance());

        Executable distributeEmptyBalance = () -> pol.invoke(governance,
                "distribute");

        // Assert
        expectErrorMessage(distributeEmptyBalance, Errors.EMPTY_BALANCE);
    }

    @Test
    public void distribute_notConfigured() {
        // Arrange
        pol.getAccount().addBalance(BigInteger.TEN.multiply(EXA));

        // Act
        Executable distributeNotConfigured = () -> pol.invoke(governance, "distribute");

        // Assert
        expectErrorMessage(distributeNotConfigured, Errors.NOT_CONFIGURED);
    }

    @Test
    public void distribute() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(100).multiply(EXA);
        pol.getAccount().addBalance(balance);
        LiquidityDistribution eth = newDist("ETH/sICX",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        LiquidityDistribution avax = newDist("AVAX/sICX",
                BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        LiquidityDistribution bnb = newDist("BNB/sICX",
                BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        LiquidityDistribution[] dist = new LiquidityDistribution[] { eth, avax, bnb };
        pol.invoke(governance, "configureDistributions", (Object) dist);

        BigInteger stakedAmount = BigInteger.valueOf(90).multiply(EXA);
        when(sICX.mock.balanceOf(pol.getAddress())).thenReturn(stakedAmount);

        BigInteger expectedEthShare = stakedAmount.multiply(BigInteger.valueOf(90)).divide(BigInteger.valueOf(100));
        BigInteger expectedAvaxShare = stakedAmount.multiply(BigInteger.valueOf(6)).divide(BigInteger.valueOf(100));
        BigInteger expectedBNBShare = stakedAmount.multiply(BigInteger.valueOf(4)).divide(BigInteger.valueOf(100));

        JsonArray expectedData = new JsonArray()
                .add(new JsonObject()
                        .add("source", eth.source)
                        .add("amount", expectedEthShare.toString()))
                .add(new JsonObject()
                        .add("source", avax.source)
                        .add("amount", expectedAvaxShare.toString()))
                .add(new JsonObject()
                        .add("source", bnb.source)
                        .add("amount", expectedBNBShare.toString()));
        // Act
        pol.invoke(governance, "distribute");

        // Assert
        verify(staking.mock).stakeICX(null, null);
        BigInteger total = expectedEthShare.add(expectedAvaxShare).add(expectedBNBShare);
        verify(sICX.mock).transfer(rewards, total, expectedData.toString().getBytes());
    }

    @Test
    public void testPermissions() {
        _testPermission("setStaking", Checks.Errors.ONLY_OWNER, staking.getAddress());
        _testPermission("setBalancedRewards", Checks.Errors.ONLY_OWNER, rewards);
        _testPermission("setSICX", Checks.Errors.ONLY_OWNER, sICX.getAddress());
        _testPermission("configureDistributions", Checks.Errors.ONLY_OWNER, (Object)new LiquidityDistribution[0]);

    }

    private void _testPermission(String method, String error, Object... params) {
        Account dummy = sm.createAccount();
        Executable call = () -> pol.invoke(dummy, method, params);
        expectErrorMessage(call, error);
    }

    public LiquidityDistribution newDist(String source, BigInteger share) {
        LiquidityDistribution bucket = new LiquidityDistribution();
        bucket.source = source;
        bucket.share = share;

        return bucket;
    }

    public void distEq(LiquidityDistribution a, LiquidityDistribution b) {
        assertEquals(a.share, b.share);
        assertEquals(a.source, b.source);
    }
}
