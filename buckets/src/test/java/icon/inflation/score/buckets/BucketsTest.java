package icon.inflation.score.buckets;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import icon.inflation.score.structs.Bucket;
import icon.inflation.score.util.Checks;

import static icon.inflation.test.Utils.expectErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static icon.inflation.score.util.Constants.EXA;

public class BucketsTest extends TestBase {
    protected final ServiceManager sm = getServiceManager();
    protected final Account governance = sm.createAccount();

    protected Score buckets;

    @BeforeEach
    public void setup() throws Exception {
        buckets = sm.deploy(governance, Buckets.class);
    }

    @Test
    public void configureBuckets_onlyOwner() {
        // Arrange
        Account nonOwner = sm.createAccount();

        // Act
        Executable nonOwnerConfigure = () -> buckets.invoke(nonOwner, "configureBuckets", (Object) new Bucket[0]);

        // Assert
        expectErrorMessage(nonOwnerConfigure, Checks.Errors.ONLY_OWNER);
    }

    @Test
    public void configureBuckets_invalidSum() {
        // Arrange
        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(95).multiply(EXA).divide(BigInteger.valueOf(100))); // 95%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        Bucket insurance = newBucket("Insurance", BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };

        // Act
        Executable invalidSum = () -> buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        // Assert
        expectErrorMessage(invalidSum, Errors.INVALID_SUM);
    }

    @Test
    public void configureBuckets_negativeShare() {
        // Arrange
        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(15).multiply(EXA).divide(BigInteger.valueOf(100))); // 15%
        Bucket insurance = newBucket("Insurance",
                BigInteger.valueOf(5).multiply(EXA).divide(BigInteger.valueOf(100)).negate()); // -5%

        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };

        // Act
        Executable negativeShare = () -> buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        // Assert
        expectErrorMessage(negativeShare, Errors.NEGATIVE_PERCENTAGE);
    }

    @Test
    public void configureBuckets() {
        // Arrange
        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        Bucket insurance = newBucket("Insurance", BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%

        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };

        // Act
        buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        // Assert
        Bucket[] configuredBuckets = (Bucket[]) buckets.call("getBuckets");
        bucketEq(_buckets[0], configuredBuckets[0]);
        bucketEq(_buckets[0], configuredBuckets[0]);
        bucketEq(_buckets[0], configuredBuckets[0]);
    }

    @Test
    public void distribute_emptyBalance() {
        // Arrange
        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        Bucket insurance = newBucket("Insurance", BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%
        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };
        buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        // Act
        Executable distributeEmptyBalance = () -> buckets.invoke(governance, "distribute");

        // Assert
        expectErrorMessage(distributeEmptyBalance, Errors.EMPTY_BALANCE);
    }

    @Test
    public void distribute_bucketsNotConfigured() {
        // Arrange
        buckets.getAccount().addBalance(BigInteger.TEN.multiply(EXA));

        // Act
        Executable distributeBucketsNotConfigured = () -> buckets.invoke(governance, "distribute");

        // Assert
        expectErrorMessage(distributeBucketsNotConfigured, Errors.BUCKETS_NOT_CONFIGURED);
    }

    @Test
    public void distribute_manualMath() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(100).multiply(EXA);
        buckets.getAccount().addBalance(balance);

        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        Bucket insurance = newBucket("Insurance", BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%
        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };
        buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        BigInteger expectedNOLShare = BigInteger.valueOf(90).multiply(EXA);
        BigInteger expectedSavingsShare = BigInteger.valueOf(6).multiply(EXA);
        BigInteger expectedInsuranceShare = BigInteger.valueOf(4).multiply(EXA);

        // Act
        buckets.invoke(governance, "distribute");

        // Assert
        assertEquals(expectedNOLShare, balanceOf(networkOwnedLiquidity));
        assertEquals(expectedSavingsShare, balanceOf(savings));
        assertEquals(expectedInsuranceShare, balanceOf(insurance));
    }

    @Test
    public void distribute_decimalMath() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(1234567).multiply(EXA);
        buckets.getAccount().addBalance(balance);

        Bucket networkOwnedLiquidity = newBucket("NOL",
                BigInteger.valueOf(90).multiply(EXA).divide(BigInteger.valueOf(100))); // 90%
        Bucket savings = newBucket("Savings", BigInteger.valueOf(6).multiply(EXA).divide(BigInteger.valueOf(100))); // 6%
        Bucket insurance = newBucket("Insurance", BigInteger.valueOf(4).multiply(EXA).divide(BigInteger.valueOf(100))); // 4%
        Bucket[] _buckets = new Bucket[] { networkOwnedLiquidity, savings, insurance };
        buckets.invoke(governance, "configureBuckets", (Object) _buckets);

        BigInteger expectedNOLShare = networkOwnedLiquidity.share.multiply(balance).divide(EXA);
        BigInteger expectedSavingsShare = savings.share.multiply(balance).divide(EXA);
        BigInteger expectedInsuranceShare = insurance.share.multiply(balance).divide(EXA);

        // Act
        buckets.invoke(governance, "distribute");

        // Assert
        assertEquals(expectedNOLShare, balanceOf(networkOwnedLiquidity));
        assertEquals(expectedSavingsShare, balanceOf(savings));
        assertEquals(expectedInsuranceShare, balanceOf(insurance));
    }

    public BigInteger balanceOf(Bucket bucket) {
        return sm.getAccount(bucket.address).getBalance();
    }

    public Bucket newBucket(String name, BigInteger share) {
        Bucket bucket = new Bucket();
        bucket.name = name;
        bucket.address = sm.createAccount().getAddress();
        bucket.share = share;

        return bucket;
    }

    public void bucketEq(Bucket a, Bucket b) {
        assertEquals(a.name, b.name);
        assertEquals(a.share, b.share);
        assertEquals(a.address, b.address);
    }
}
