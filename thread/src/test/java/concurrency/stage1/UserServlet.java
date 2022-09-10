package concurrency.stage1;

import java.util.ArrayList;
import java.util.List;

public class UserServlet {

    private final List<User> users = new ArrayList<>();

    public void service(final User user) {
        join(user);
    }

    private synchronized void join(final User user) { // synchronized 를 통해 메서드에 접근할 수 있는 스레드를 제한
        if (!users.contains(user)) {
            // 스레드2 : !users.contains(user) = true 후 2초간 대기
            // 스레드1 : !users.contains(user) = true 아직 users.add(user) 가 스레드2에 의해 호출되지 않음
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            users.add(user); // 두개의 스레드 모두 add 해버림
        }
    }

    public int size() {
        return users.size();
    }

    public List<User> getUsers() {
        return users;
    }
}
