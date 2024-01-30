package icon.inflation.score.insurance;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import icon.inflation.score.util.Checks;

import static icon.inflation.test.Utils.expectErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsuranceTest extends TestBase {
    protected final ServiceManager sm = getServiceManager();
    protected final Account governance = sm.createAccount();

    protected Score insurance;

    @BeforeEach
    public void setup() throws Exception {
        insurance = sm.deploy(governance, Insurance.class);
    }

    @Test
    public void transfer_onlyOwner() {
        // Arrange
        Account nonOwner = sm.createAccount();

        // Act
        Executable nonOwnerTransfer = () -> insurance.invoke(nonOwner, "transfer", nonOwner.getAddress(),
                BigInteger.ONE);

        // Assert
        expectErrorMessage(nonOwnerTransfer, Checks.Errors.ONLY_OWNER);
    }

    @Test
    public void transfer() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(100);
        BigInteger amount = BigInteger.valueOf(10);
        Account to = sm.createAccount();
        insurance.getAccount().addBalance(balance);

        // Act
        insurance.invoke(governance, "transfer", to.getAddress(), amount);

        // Assert
        assertEquals(amount, to.getBalance());
        assertEquals(balance.subtract(amount), insurance.getAccount().getBalance());
    }

}
