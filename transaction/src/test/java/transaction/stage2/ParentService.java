package transaction.stage2;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ParentService {

    private static final Logger log = LoggerFactory.getLogger(ParentService.class);

    private final UserRepository userRepository;
    private final ChildService childService;

    @Autowired
    public ParentService(final UserRepository userRepository,
                         final ChildService childService) {
        this.userRepository = userRepository;
        this.childService = childService;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Set<String> required(final ChildServiceCallBack childServiceCallBack) {
        final var firstTransactionName = getMethodName();
        userRepository.save(User.createTest());
        logActualTransactionActive();
        final var secondTransactionName = childServiceCallBack.callBack(childService);
        return of(firstTransactionName, secondTransactionName);
    }

    public Set<String> nonTransactional(final ChildServiceCallBack childServiceCallBack) {
        final var firstTransactionName = getMethodName();
        userRepository.save(User.createTest());
        logActualTransactionActive();
        final var secondTransactionName = childServiceCallBack.callBack(childService);
        return of(firstTransactionName, secondTransactionName);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void exception(final ChildServiceCallBack childServiceCallBack) {
        childServiceCallBack.callBack(childService);
        userRepository.save(User.createTest());
        logActualTransactionActive();
        throw new RuntimeException();
    }

    private Set<String> of(final String firstTransactionName, final String secondTransactionName) {
        return Stream.of(firstTransactionName, secondTransactionName)
                .filter(transactionName -> !Objects.isNull(transactionName))
                .collect(Collectors.toSet());
    }

    private void logActualTransactionActive() {
        final var actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        final var emoji = actualTransactionActive ? "🟢" : "🚫";
        log.info("Parent Transaction Active : {}", emoji);
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
