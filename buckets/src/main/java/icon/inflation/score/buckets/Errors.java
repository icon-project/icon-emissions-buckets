package icon.inflation.score.buckets;


public class Errors {
    public static final String NEGATIVE_PERCENTAGE = "The inflation share cannot be negative";
    public static final String INVALID_SUM = "The total shares of all buckets must add up to 100%";
    public static final String BUCKETS_NOT_CONFIGURED = "Buckets has not yet been configured";
    public static final String EMPTY_BALANCE = "Current ICX Balance is empty";
    public static final String NO_REENTRY = "No reentry allowed";
}
