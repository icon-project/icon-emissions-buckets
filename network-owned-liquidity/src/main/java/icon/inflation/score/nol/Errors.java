package icon.inflation.score.nol;


public class Errors {
    public static final String TOKEN_FALLBACK_DATA_EMPTY = "Token Fallback: Data can't be empty";
    public static final String IRC31_METHOD_NOT_FOUND = "IRC31: method not found";
    public static final String ORDER_LIMIT_REACHED = "Order is above configured limit";
    public static final String LP_OVER_SLIPPAGE_LIMIT = "The price of the liquidity pool is to far off the oracle price of supplied assets";
    public static final String INVALID_PERIOD = "Order period must be between 1 day and 3 months";
}
