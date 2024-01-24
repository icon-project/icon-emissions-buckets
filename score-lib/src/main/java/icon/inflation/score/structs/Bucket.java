package icon.inflation.score.structs;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Bucket {
    public String name;
    public Address address;
    public BigInteger share;

    public static void writeObject(ObjectWriter writer, Bucket bucket) {
        bucket.writeObject(writer);
    }

    public static Bucket readObject(ObjectReader reader) {
        Bucket bucket = new Bucket();
        reader.beginList();
        bucket.name = reader.readString();
        bucket.address = reader.readAddress();
        bucket.share = reader.readBigInteger();
        reader.end();
        return bucket;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.name);
        writer.write(this.address);
        writer.write(this.share);
        writer.end();
    }
}
