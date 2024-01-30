package icon.inflation.score.savings;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import icon.inflation.score.util.Checks;
import icon.inflation.test.MockContract;
import icon.inflation.test.interfaces.*;
import score.UserRevertedException;

import static icon.inflation.test.Utils.expectErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class SavingsRateTest extends TestBase {
    protected final ServiceManager sm = getServiceManager();
    protected final Account governance = sm.createAccount();

    protected Score savings;

    protected MockContract<Staking> staking;
    protected final Account balancedSavings = sm.createAccount();

    @BeforeEach
    public void setup() throws Exception {
        staking = new MockContract<>(StakingScoreInterface.class, Staking.class, sm, governance);
        savings = sm.deploy(governance, SavingsRate.class, staking.getAddress(), balancedSavings.getAddress());
    }

    @Test
    public void fallback() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(100);
        BigInteger sendAmount = BigInteger.TEN;
        savings.getAccount().addBalance(balance);
        governance.addBalance(sendAmount);

        // Act
        savings.invoke(governance, sendAmount, "fallback");

        // Assert
        verify(staking.mock).stakeICX(balancedSavings.getAddress(), null);
        assertEquals(BigInteger.ZERO, savings.getAccount().getBalance());
    }

    @Test
    public void fallback_failed() {
        // Arrange
        BigInteger sendAmount = BigInteger.TEN;
        governance.addBalance(sendAmount);

        doThrow(UserRevertedException.class).when(staking.mock).stakeICX(balancedSavings.getAddress(), null);

        // Act
        savings.invoke(governance, sendAmount, "fallback");

        // Assert
        assertEquals(sendAmount, savings.getAccount().getBalance());
    }

    @Test
    public void stakeAndSend() {
        // Arrange
        BigInteger balance = BigInteger.valueOf(100);
        BigInteger amount = BigInteger.TEN;
        savings.getAccount().addBalance(balance);

        // Act
        savings.invoke(governance, "stakeAndSend", amount);

        // Assert
        verify(staking.mock).stakeICX(balancedSavings.getAddress(), null);
        assertEquals(balance.subtract(amount), savings.getAccount().getBalance());

    }

    @Test
    public void testPermissions() {
        _testPermission("setStaking", Checks.Errors.ONLY_OWNER, staking.getAddress());
        _testPermission("setBalancedReceiver", Checks.Errors.ONLY_OWNER, staking.getAddress());
    }

    private void _testPermission(String method, String error, Object... params) {
        Account dummy = sm.createAccount();
        Executable call = () -> savings.invoke(dummy, method, params);
        expectErrorMessage(call, error);
    }
}