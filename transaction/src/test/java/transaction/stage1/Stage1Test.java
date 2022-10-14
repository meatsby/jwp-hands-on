package transaction.stage1;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import transaction.DatabasePopulatorUtils;
import transaction.RunnableWrapper;

/**
 * 격리 레벨(Isolation Level)에 따라 여러 사용자가 동시에 db에 접근했을 때 어떤 문제가 발생하는지 확인해보자. ❗phantom reads는 docker를 실행한 상태에서 테스트를 실행한다.
 * ❗phantom reads는 MySQL로 확인한다. H2 데이터베이스에서는 발생하지 않는다.
 * <p>
 * 참고 링크 https://en.wikipedia.org/wiki/Isolation_(database_systems)
 * <p>
 * 각 테스트에서 어떤 현상이 발생하는지 직접 경험해보고 아래 표를 채워보자. + : 발생 - : 발생하지 않음 Read phenomena | Dirty reads | Non-repeatable reads |
 * Phantom reads Isolation level  |             |                      |
 * -----------------|-------------|----------------------|-------------- Read Uncommitted |      +      |           + |
 * + Read Committed   |      -      |           +          |       + Repeatable Read  |      -      | -          | +
 * Serializable     |      -      |           -          |       -
 */
class Stage1Test {

    private static final Logger log = LoggerFactory.getLogger(Stage1Test.class);
    private DataSource dataSource;
    private UserDao userDao;

    private void setUp(final DataSource dataSource) {
        this.dataSource = dataSource;
        DatabasePopulatorUtils.execute(dataSource);
        this.userDao = new UserDao(dataSource);
    }

    /**
     * 격리 수준에 따라 어떤 현상이 발생하는지 테스트를 돌려 직접 눈으로 확인하고 표를 채워보자. + : 발생 - : 발생하지 않음 Read phenomena | Dirty reads Isolation
     * level  | -----------------|------------- Read Uncommitted | Read Committed   | Repeatable Read  | Serializable |
     */
    @Test
    void dirtyReading() throws SQLException {
        setUp(createH2DataSource());

        // 새로운 연결(사용자A)을 받아와서 트랜잭션을 시작한 뒤, gugu 객체를 저장한다.
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        User gugu = new User("gugu", "password", "hkkang@woowahan.com");
        userDao.insert(connection, gugu);

        for (IsolationLevel isolationLevel : IsolationLevel.values()) {
            new Thread(RunnableWrapper.accept(() -> {
                // connection(사용자A) 이 아닌 subConnection(사용자B) 이라는 새로운 연결을 받아온다.
                Connection subConnection = dataSource.getConnection();

                // 트랜잭션 격리 레벨을 설정한다.
                subConnection.setTransactionIsolation(isolationLevel.getLevel());

                // gugu 객체는 connection 에서 아직 커밋하지 않은 상태지만, 격리 레벨에 따라 커밋하지 않은 gugu 객체를 조회할 수 있다.
                User actual = userDao.findByAccount(subConnection, "gugu");
                log.info("isolation level : {}, user : {}", String.format("%-16s", isolationLevel.name()), actual);
            })).start();
            sleep(0.1);
        }
        sleep(0.1);

        // 롤백하면 사용자A 의 user 데이터를 저장하지 않았는데 사용자B 는 user 데이터가 있다고 인지한 상황이 된다.
        connection.rollback();
    }

    /**
     * 격리 수준에 따라 어떤 현상이 발생하는지 테스트를 돌려 직접 눈으로 확인하고 표를 채워보자. + : 발생 - : 발생하지 않음 Read phenomena | Non-repeatable reads
     * Isolation level  | -----------------|--------------------- Read Uncommitted | Read Committed   | Repeatable Read
     * | Serializable     |
     */
    @Test
    void noneRepeatable() throws SQLException {
        for (IsolationLevel isolationLevel : IsolationLevel.values()) {
            setUp(createH2DataSource());

            // 테스트 전에 필요한 데이터를 추가한다.
            User gugu = new User("gugu", "password", "hkkang@woowahan.com");
            userDao.insert(dataSource.getConnection(), gugu);

            // 새로운 연결(사용자A)을 받아온 뒤, 트랜잭션을 시작한다.
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            connection.setTransactionIsolation(isolationLevel.getLevel());

            // 사용자A 가 gugu 객체를 조회했다.
            User user = userDao.findByAccount(connection, "gugu");
            log.info("user : {}", user);

            new Thread(RunnableWrapper.accept(() -> {
                // 사용자B 가 새로 연결하여
                Connection subConnection = dataSource.getConnection();

                // 사용자A 가 조회한 gugu 객체를 사용자B 가 다시 조회한 뒤 비밀번호를 변경했다.(subConnection은 auto commit 상태)
                User anotherUser = userDao.findByAccount(subConnection, "gugu");
                anotherUser.changePassword("qqqq");
                userDao.update(subConnection, anotherUser);
            })).start();
            sleep(0.1);

            // 사용자B 가 패스워드를 변경하고 아직 커밋하지 않은 상태에서 사용자A 가 다시 gugu 객체를 조회했다.
            User actual = userDao.findByAccount(connection, "gugu");
            log.info("isolation level : {}, user : {}", String.format("%-16s", isolationLevel.name()), actual);

            connection.rollback();
            dataSource.getConnection().prepareStatement("drop table if exists users").execute();
        }
    }

    /**
     * phantom read는 h2에서 발생하지 않는다. mysql로 확인해보자. 격리 수준에 따라 어떤 현상이 발생하는지 테스트를 돌려 직접 눈으로 확인하고 표를 채워보자. + : 발생 - : 발생하지 않음
     * Read phenomena | Phantom reads Isolation level  | -----------------|-------------- Read Uncommitted | Read
     * Committed   | Repeatable Read  | Serializable     |
     */
    @ParameterizedTest
    @EnumSource(value = IsolationLevel.class)
    void phantomReading(final IsolationLevel isolationLevel) throws SQLException {
        // testcontainer 로 docker 를 실행해서 mysql 에 연결한다.
        MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.30"))
                .withLogConsumer(new Slf4jLogConsumer(log));
        mysql.start();
        setUp(createMySQLDataSource(mysql));

        // 테스트 전에 필요한 데이터를 추가한다.
        User gugu = new User("gugu", "password", "hkkang@woowahan.com");
        userDao.insert(dataSource.getConnection(), gugu);

        // 새로운 연결(사용자A)을 받아온 뒤, 트랜잭션을 시작한다.
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        // 트랜잭션 격리 레벨을 설정한다.
        connection.setTransactionIsolation(isolationLevel.getLevel());

        // 사용자A 가 id 로 범위를 조회했다.
        userDao.findGreaterThan(connection, 1);

        new Thread(RunnableWrapper.accept(() -> {
            // 사용자B 가 새로 연결한 뒤, 트랜잭션을 시작한다.
            Connection subConnection = dataSource.getConnection();
            subConnection.setAutoCommit(false);

            // 새로운 user 객체가 id는 2인 상태로 저장된다.
            User bird = new User("bird", "password", "bird@woowahan.com");
            userDao.insert(subConnection, bird);

            subConnection.commit();
        })).start();
        sleep(0.1);

        // MySQL에서 팬텀 읽기를 시연하려면 update를 실행해야 한다.
        // http://stackoverflow.com/questions/42794425/unable-to-produce-a-phantom-read/42796969#42796969
//        userDao.updatePasswordGreaterThan(connection, "qqqq", 1);

        // 사용자A 가 다시 id로 범위를 조회했다.
        List<User> actual = userDao.findGreaterThan(connection, 1);
        log.info("isolation level : {}, user : {}", String.format("%-16s", isolationLevel.name()), actual);

        connection.rollback();
        mysql.close();
    }

    private static DataSource createMySQLDataSource(final JdbcDatabaseContainer<?> container) {
        final var config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(config);
    }

    private static DataSource createH2DataSource() {
        final var jdbcDataSource = new JdbcDataSource();
        // h2 로그를 확인하고 싶을 때 사용
//        jdbcDataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;TRACE_LEVEL_SYSTEM_OUT=3;MODE=MYSQL");
        jdbcDataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MYSQL;");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("");
        return jdbcDataSource;
    }

    private void sleep(double seconds) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (seconds * 1000));
        } catch (InterruptedException ignored) {
        }
    }
}
