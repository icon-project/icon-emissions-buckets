package icon.inflation.score.structs;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class LiquidityDistribution {
    public String source;
    public BigInteger share;

    public static void writeObject(ObjectWriter writer, LiquidityDistribution dist) {
        dist.writeObject(writer);
    }

    public static LiquidityDistribution readObject(ObjectReader reader) {
        LiquidityDistribution dist = new LiquidityDistribution();
        reader.beginList();
        dist.source = reader.readString();
        dist.share = reader.readBigInteger();
        reader.end();
        return dist;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.source);
        writer.write(this.share);
        writer.end();
    }
}
