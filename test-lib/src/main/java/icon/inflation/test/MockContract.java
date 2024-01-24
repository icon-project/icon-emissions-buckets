package icon.inflation.test;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import org.mockito.Mockito;
import score.Address;

public class MockContract<T> {
    final private static Address SCORE_ZERO = Address.fromString("cx" + "0".repeat(40));
    public final Account account;
    public final T mock;

    public MockContract(Class<? extends T> classToMock, ServiceManager sm, Account admin) throws Exception {
        mock = Mockito.mock(classToMock);
        Score score = sm.deploy(admin, classToMock, SCORE_ZERO);
        score.setInstance(mock);
        account = score.getAccount();
    }

    public MockContract(Class<? extends T> classToMock, Class<T> mockClass, ServiceManager sm, Account admin) throws Exception {
        mock = Mockito.mock(mockClass);
        Score score = sm.deploy(admin, classToMock, SCORE_ZERO);
        score.setInstance(mock);
        account = score.getAccount();
    }

    public Address getAddress() {
        return account.getAddress();
    }
}