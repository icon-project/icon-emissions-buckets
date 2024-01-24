package icon.inflation.score.util;

import score.ArrayDB;

public class DBUtils {
    public static <T>  void clear(ArrayDB<T> db) {
        int size = db.size();
        for (int i = 0; i < size; i++) {
            db.removeLast();
        }
    }
}
