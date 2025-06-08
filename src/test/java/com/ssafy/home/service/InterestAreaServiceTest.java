//package com.ssafy.home.service;
//
//import com.ssafy.home.domain.InterestArea;
//import com.ssafy.home.domain.User;
//import com.ssafy.home.mapper.InterestAreaMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@Transactional
//class InterestAreaServiceTest {
//    @Autowired
//    private InterestAreaService interestAreaService;
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private InterestAreaMapper interestAreaMapper;
//
//    private User testUser;
//
//    @BeforeEach
//    void setUp() {
//        interestAreaMapper.deleteAll();
//
//        User user = User.builder()
//                .email("test@ssafy.com")
//                .password("pw1234")
//                .phone("010-1000-1000")
//                .name("테스트 유저")
//                .build();
//        userService.register(user);
//        testUser = user;
//    }
//
//    @Test
//    void save() {
//        //given
//        int id = testUser.getMno();
//        InterestArea interestArea = InterestArea.builder()
//                .userId(id)
//                .dongCode("4145010100")
//                .build();
//
//        // when
//        int row = interestAreaService.save(interestArea);
//
//        // then
//        assertThat(row).isGreaterThan(0);
//    }
//
//    @Test
//    void findAll() {
//        // given
//        int id = testUser.getMno();
//        InterestArea interestArea = InterestArea.builder()
//                .userId(id)
//                .dongCode("4145010100")
//                .build();
//
//        InterestArea interestArea2 = InterestArea.builder()
//                .userId(id)
//                .dongCode("2714014100")
//                .build();
//
//        // when
//        interestAreaService.save(interestArea);
//        interestAreaService.save(interestArea2);
//
//        // then
//        assertThat(interestAreaService.findAll().size()).isEqualTo(2);
//    }
//
//    @Test
//    void delete() {
//        // given
//        int id = testUser.getMno();
//        InterestArea interestArea = InterestArea.builder()
//                .userId(id)
//                .dongCode("4145010100")
//                .build();
//
//        // when
//        interestAreaService.save(interestArea);
//        int row = interestAreaService.delete(interestArea.getNo());
//
//        // then
//        assertThat(row).isGreaterThan(0);
//    }
//}