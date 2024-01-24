package icon.inflation.score.buckets;

import java.math.BigInteger;

import icon.inflation.score.interfaces.IBuckets;
import icon.inflation.score.structs.Bucket;
import icon.inflation.score.util.DBUtils;
import score.Context;
import score.ArrayDB;
import score.annotation.External;
import score.annotation.Payable;

import static icon.inflation.score.util.Constants.EXA;
import static icon.inflation.score.util.Checks.onlyOwner;

public class Buckets implements IBuckets {

    public ArrayDB<Bucket> buckets = Context.newArrayDB("BUCKETS", Bucket.class);

    private static BigInteger TOTAL_SHARE = EXA;
    private static String NAME = "ICON IRelay Inflation Manager";
    private static boolean distributing = false;
    public Buckets() {
    }

    @External(readonly = true)
    public String name() {
        return NAME;
    }

    @External(readonly = true)
    public Bucket[] getBuckets() {
        int size = buckets.size();
        Bucket[] _buckets = new Bucket[size];
        for (int i = 0; i < size; i++) {
            _buckets[i] = buckets.get(i);
        }

        return _buckets;
    }

    @External
    public void configureBuckets(Bucket[] _buckets) {
        // Dev note: Since IRelay enforces the owner of the contract to be the governance score
        // this should always be the governance score when deployed in a real
        // environment.
        onlyOwner();
        BigInteger sum = BigInteger.ZERO;
        DBUtils.clear(this.buckets);

        for (Bucket bucket : _buckets) {
            Context.require(bucket.share.compareTo(BigInteger.ZERO) > 0, Errors.NEGATIVE_PERCENTAGE);
            sum = sum.add(bucket.share);
            this.buckets.add(bucket);
        }

        Context.require(sum.equals(TOTAL_SHARE), Errors.INVALID_SUM);
    }

    @External
    public void distribute() {
        Context.require(!distributing, Errors.NO_REENTRY);
        int size = buckets.size();
        BigInteger balance = Context.getBalance(Context.getAddress());

        Context.require(size > 0, Errors.BUCKETS_NOT_CONFIGURED);
        Context.require(balance.compareTo(BigInteger.ZERO) > 0, Errors.EMPTY_BALANCE);

        distributing = true;
        for (int i = 0; i < size; i++) {
            Bucket bucket = buckets.get(i);
            // Dev note: This way of calculating will create remaining dust. But since we
            // are dealing with large amounts this should not matter and a good trade of for
            // simplicity
            BigInteger share = bucket.share.multiply(balance).divide(TOTAL_SHARE);
            Context.transfer(bucket.address, share);
        }

        distributing = false;
    }

    @Payable
    public void fallback() {
    }

}
