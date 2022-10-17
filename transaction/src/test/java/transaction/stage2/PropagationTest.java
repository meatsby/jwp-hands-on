package transaction.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PropagationTest {

    private static final Logger log = LoggerFactory.getLogger(PropagationTest.class);

    @Autowired
    private ParentService parentService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Nested
    class testRequired {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Required")
        void withParent() {
            final var actual = parentService.required(ChildService::required);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Required")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::required);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }
    }

    @Nested
    class testSupports {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Supports")
        void withParent() {
            final var actual = parentService.required(ChildService::supports);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Supports")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::supports);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }
    }

    @Nested
    class testMandatory {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Mandatory")
        void withParent() {
            final var actual = parentService.required(ChildService::mandatory);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Mandatory")
        void withoutParent() {
            assertThatThrownBy(() -> parentService.nonTransactional(ChildService::mandatory))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessage("No existing transaction found for transaction marked with propagation 'mandatory'");
        }
    }

    @Nested
    class testRequiredNew {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Requires_New")
        void withParent() {
            final var actual = parentService.required(ChildService::requiresNew);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(2);
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Requires_New")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::requiresNew);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }

        @Test
        @DisplayName("부모 트랜잭션이 예외를 발생하는 Requires_New")
        void withRollback() {
            assertThat(parentService.findAll()).hasSize(0);

            assertThatThrownBy(() -> parentService.exception(ChildService::requiresNew))
                    .isInstanceOf(RuntimeException.class);

            assertThat(parentService.findAll()).hasSize(1);
        }
    }

    @Nested
    class testNotSupported {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Not_Supported")
        void withParent() {
            final var actual = parentService.required(ChildService::notSupported);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(2);
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Not_Supported")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::notSupported);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }
    }

    @Nested
    class testNever {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Never")
        void withParent() {
            assertThatThrownBy(() -> parentService.required(ChildService::never))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessage("Existing transaction found for transaction marked with propagation 'never'");
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Never")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::never);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }
    }

    @Nested
    class testNested {

        @Test
        @DisplayName("부모 트랜잭션이 있는 Nested")
        void withParent() {
            assertThatThrownBy(() -> parentService.required(ChildService::nested))
                    .isInstanceOf(NestedTransactionNotSupportedException.class)
                    .hasMessage("JpaDialect does not support savepoints - check your JPA provider's capabilities");
        }

        @Test
        @DisplayName("부모 트랜잭션이 없는 Nested")
        void withoutParent() {
            final var actual = parentService.nonTransactional(ChildService::nested);

            log.info("transactions : {}", actual);
            assertThat(actual)
                    .hasSize(1);
        }
    }
}
