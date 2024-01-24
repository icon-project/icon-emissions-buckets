package icon.inflation.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public class Utils {
    public static void expectErrorMessage(Executable contractCall, String expectedErrorMessage) {
        Exception e = Assertions.assertThrows(Exception.class, contractCall);
        assertTrue(e.getMessage().contains(expectedErrorMessage));
    }

}
