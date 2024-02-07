package icon.inflation.score.structs;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class LiquidityOrder {
    public BigInteger limit;
    public BigInteger lastPurchaseBlock;
    public BigInteger remaining;
    // return value only
    public BigInteger pid;

    public static void writeObject(ObjectWriter writer, LiquidityOrder liquidityOrder) {
        liquidityOrder.writeObject(writer);
    }

    public static LiquidityOrder readObject(ObjectReader reader) {
        LiquidityOrder liquidityOrder = new LiquidityOrder();
        reader.beginList();
        liquidityOrder.limit = reader.readBigInteger();
        liquidityOrder.lastPurchaseBlock = reader.readBigInteger();
        liquidityOrder.remaining = reader.readBigInteger();
        reader.end();
        return liquidityOrder;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.limit);
        writer.write(this.lastPurchaseBlock);
        writer.write(this.remaining);
        writer.end();
    }
}
