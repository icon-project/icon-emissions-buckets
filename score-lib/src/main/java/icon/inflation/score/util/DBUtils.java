package icon.inflation.score.util;

import score.ArrayDB;

public class DBUtils {
    public static <T>  void clear(ArrayDB<T> db) {
        int size = db.size();
        for (int i = 0; i < size; i++) {
            db.removeLast();
        }
    }

    public static <T> Boolean arrayDbContains(ArrayDB<T> arrayDB, T item) {
        final int size = arrayDB.size();
        for (int i = 0; i < size; i++) {
            if (arrayDB.get(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static <T> Boolean removeFromArraydb(ArrayDB<T> _array, T _item) {
        final int size = _array.size();
        if (size < 1) {
            return false;
        }
        T top = _array.get(size - 1);
        for (int i = 0; i < size; i++) {
            if (_array.get(i).equals(_item)) {
                _array.set(i, top);
                _array.pop();
                return true;
            }
        }

        return false;
    }
}
