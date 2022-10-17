package transaction.stage2;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChildService {

    private static final Logger log = LoggerFactory.getLogger(ChildService.class);

    private final UserRepository userRepository;

    public ChildService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String required() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public String supports() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String mandatory() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String requiresNew() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String notSupported() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.NEVER)
    public String never() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    @Transactional(propagation = Propagation.NESTED)
    public String nested() {
        userRepository.save(User.createTest());
        logActualTransactionActive();
        return getMethodName();
    }

    private void logActualTransactionActive() {
        final var actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        final var emoji = actualTransactionActive ? "🟢" : "🚫";
        log.info(" Child Transaction Active : {}", emoji);
    }

    private String getMethodName() {
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        if (Objects.isNull(currentTransactionName)) {
            return null;
        }
        String[] strings = currentTransactionName.split("\\.");
        return strings[strings.length - 2];
    }
}
