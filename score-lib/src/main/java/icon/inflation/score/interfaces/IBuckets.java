package icon.inflation.score.interfaces;

import icon.inflation.score.structs.Bucket;
import score.annotation.External;
import score.annotation.Payable;

public interface IBuckets {

    @External(readonly = true)
    String name();

    /**
     * Getter for inflation configuration
     *
     * @return Returns the bucket configuration
     */
    @External(readonly = true)
    Bucket[] getBuckets();

    /**
     * Configures the inflations buckets.
     *
     * @param _buckets a list of all buckets for the current configuration.
     */
    @External
    void configureBuckets(Bucket[] _buckets);

    /**
     * Distributes inflations to all currently configured buckets
     *
     */
    @External
    void distribute();

    @Payable
    void fallback();
}
