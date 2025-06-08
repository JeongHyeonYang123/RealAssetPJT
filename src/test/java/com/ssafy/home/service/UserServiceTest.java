package com.ssafy.home.service;

import com.ssafy.home.domain.User;
import com.ssafy.home.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
//@Transactional
class UserServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

//    @BeforeEach
//    void setUp() {
//        userMapper.deleteAll();
//    }

    private User makeUser(String email, String password, String phone, String name) {
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .build();
    }

    @Test
    void register() {
        // given
        User user = makeUser("test@ssafy.com", "test", "010-1000-1000", "테스트1");

        // when
        userService.register(user);

        // then
    }

//    @Test
//    void login() {
//        // given
//        User user = makeUser("test2@ssafy.com", "pw1234", "010-1000-1001", "테스트2");
//        userService.register(user);
//
//        // when
//        Optional<User> loginUser = userService.login("test2@ssafy.com", "pw1234");
//        Optional<User> wrongLogin = userService.login("test2@ssafy.com", "wrongpw");
//
//        // then
//        assertTrue(loginUser.isPresent());
//        assertEquals("test2@ssafy.com", loginUser.get().getEmail());
//        assertTrue(wrongLogin.isEmpty());
//    }

    @Test
    void existsByEmail() {
        // given
        User user = makeUser("test3@ssafy.com", "pw1234", "010-1000-1002", "테스트3");
        userService.register(user);

        // when
        boolean exists = userService.existsByEmail("test3@ssafy.com");
        boolean notExists = userService.existsByEmail("notexist@ssafy.com");

        // then
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void findActiveUserByEmail() {
        // given
        User user = makeUser("test4@ssafy.com", "pw1234", "010-1000-1003", "테스트4");
        userService.register(user);

        // when
        Optional<User> found = userService.findActiveUserByEmail("test4@ssafy.com");
        Optional<User> notFound = userService.findActiveUserByEmail("notexist@ssafy.com");

        // then
        assertTrue(found.isPresent());
        assertEquals("test4@ssafy.com", found.get().getEmail());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void findPasswordByEmail() {
        // given
        User user = makeUser("test5@ssafy.com", "pw1234", "010-1000-1004", "테스트5");
        userService.register(user);

        // when
        Optional<String> pwOpt = userService.findPasswordByEmail("test5@ssafy.com");

        // then
        assertTrue(pwOpt.isPresent());
        assertFalse(pwOpt.get().isEmpty());
    }

    @Test
    void searchByKeyword() {
        // given
        User user1 = makeUser("test6@ssafy.com", "pw1234", "010-1000-1005", "테스트6");
        User user2 = makeUser("searchuser@ssafy.com", "pw1234", "010-1000-1006", "홍길동");
        userService.register(user1);
        userService.register(user2);

        // when
        List<User> result = userService.searchByKeyword("홍길동");

        // then
        assertFalse(result.isEmpty());
        assertEquals("홍길동", result.get(0).getName());
    }

    @Test
    void update() {
        // given
        User user = makeUser("test7@ssafy.com", "pw1234", "010-1000-1007", "테스트7");
        userService.register(user);

        // when
        user.setName("수정된이름");
        user.setPhone("010-9999-8888");
        int result = userService.update(user, user.getMno());
        Optional<User> updated = userService.findActiveUserByEmail("test7@ssafy.com");

        // then
        assertTrue(result > 0);
        assertTrue(updated.isPresent());
        assertEquals("수정된이름", updated.get().getName());
        assertEquals("010-9999-8888", updated.get().getPhone());
    }

    @Test
    void deleteByEmail() {
        // given
        User user = makeUser("test8@ssafy.com", "pw1234", "010-1000-1008", "테스트8");
        userService.register(user);

        // when
        int result = userService.deleteByEmail("test8@ssafy.com");
        boolean exists = userService.existsByEmail("test8@ssafy.com");

        // then
        assertTrue(result > 0);
        assertFalse(exists);
    }

    @Test
    void findAll() {
        // given
        int before = userService.findAll().size();
        userService.register(makeUser("test9@ssafy.com", "pw1234", "010-1000-1009", "테스트9"));
        userService.register(makeUser("test10@ssafy.com", "pw1234", "010-1000-1010", "테스트10"));

        // when
        List<User> users = userService.findAll();

        // then
        assertTrue(users.size() >= before + 2);
    }

//    @Test
//    void updateTemporaryPasswordByEmail() {
//        // given
//        User user = makeUser("test12@ssafy.com", "pw1234", "010-1000-1012", "테스트12");
//        userService.register(user);
//
//        // when
//        int result = userService.updateTemporaryPasswordByEmail("test12@ssafy.com", "newTempPw");
//        Optional<User> loginUser = userService.login("test12@ssafy.com", "newTempPw");
//
//        // then
//        assertTrue(result > 0);
//        assertTrue(loginUser.isPresent());
//    }
}
