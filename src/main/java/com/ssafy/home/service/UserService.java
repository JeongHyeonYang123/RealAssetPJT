package com.ssafy.home.service;

import com.ssafy.home.domain.User;
import com.ssafy.home.mapper.UserMapper;
import com.ssafy.home.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // 회원 가입
    public void register(User user) {
        user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        user.setRole("USER");

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24)); // 24시간 후 만료
        user.setVerified(false);

        userMapper.insert(user);

        try {
            emailService.sendVerificationEmail(user);
        } catch (Exception e) {
            // 이메일 발송 실패 시에도 회원가입은 성공으로 처리
            log.error("이메일 발송 실패: {}", e.getMessage());
        }
    }

    public boolean verifyUser(String token) {
        User user = userMapper.findByVerificationToken(token);

        if (user == null) {
            return false; // 토큰이 존재하지 않음
        }

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return false; // 토큰 만료
        }

        // 인증 완료 처리
        user.setVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiryDate(null);

        userMapper.update(user, user.getMno());
        return true;
    }

    public void resendVerificationEmail(User user) {
        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        userMapper.update(user, user.getMno());
        emailService.resendVerificationEmail(user);
    }

    // 로그인 (email + password)
    public Optional<User> login(User user) {
        if (userMapper.isUserDeleted(user.getEmail())) {
            return Optional.empty(); // 탈퇴한 계정이면 로그인 차단
        }

        Optional<String> hashedPasswordOpt = userMapper.findPasswordByEmail(user.getEmail());
        if (hashedPasswordOpt.isPresent() && PasswordUtil.checkPassword(user.getPassword(), hashedPasswordOpt.get())) {
            // 이메일과 암호화된 비밀번호로 사용자 조회
            return userMapper.findByEmailAndPassword(user.getEmail(), hashedPasswordOpt.get());
        }

        return Optional.empty();
    }

    // 이메일 존재 여부 확인
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email).isPresent();
    }

    // 활성 사용자 조회 (탈퇴 사용자는 제외)
    public Optional<User> findActiveUserByEmail(String email) {
        if (userMapper.isUserDeleted(email)) {
            return Optional.empty();
        }
        return userMapper.findByEmail(email);
    }

    // 이메일로만 유저 조회
    public Optional<User> findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    // 비밀번호 찾기 (이메일 기준)
    public Optional<String> findPasswordByEmail(String email) {
        return userMapper.findPasswordByEmail(email);
    }

    // 회원 검색 (이메일/이름)
    public List<User> searchByKeyword(String keyword) {
        return userMapper.searchByKeyword(keyword);
    }

    // 회원 정보 수정 (이메일 기준)
    public int update(User user, int mno) {
        if (user.getEmail() == null) {
            throw new IllegalArgumentException("이메일(email)은 필수입니다.");
        }

        // 비밀번호가 있으면 암호화 여부 확인 후 처리
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 암호화된 비밀번호인지 확인 (암호화된 비밀번호는 보통 $2a$로 시작)
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
            }
            // 이미 암호화되어 있으면 그대로 사용
        }

        return userMapper.update(user, mno);
    }

    // 회원 삭제
    public int deleteByEmail(String email) {
        userMapper.updateDeletedUserByEmail(email);
        return userMapper.insertDeletedUserByEmail(email);
    }

    public int deleteByMno(int mno) {
        String email = userMapper.findEmailByMno(mno).get();
        userMapper.updateDeletedUserByEmail(email);
        return userMapper.deleteByMno(mno);
    }

    public Optional<User> findByMno(int mno) {
        return userMapper.findByMno(mno);
    }

    // 전체 회원 조회
    public List<User> findAll() {
        return userMapper.selectAll();
    }

    // 이름, 이메일, 전화번호로 회원 찾기
    public Optional<User> findByNameAndEmail(String name, String email) {
        return userMapper.findByNameAndEmail(name, email);
    }

    // 임시 비밀번호 업데이트
    public int updateTemporaryPasswordByEmail(String email, String tempPassword) {
        String hashedTempPassword = PasswordUtil.hashPassword(tempPassword);
        return userMapper.updateTemporaryPasswordByEmail(email, hashedTempPassword);
    }

    // 비밀번호 초기화 및 임시 비밀번호 이메일 발송
    public boolean resetPasswordAndSendEmail(String name, String email) {
        // 1. 이름, 이메일, 전화번호가 모두 일치하는 유저가 있는지 확인
        Optional<User> userOpt = findByNameAndEmail(name, email);
        if (userOpt.isEmpty())
            return false;

        // 2. 임시 비밀번호 생성
        String tempPassword = generateRandomPassword();

        // 3. 비밀번호 암호화 후 DB 업데이트
        int updated = updateTemporaryPasswordByEmail(email, tempPassword);
        if (updated != 1)
            return false;

        // 4. 이메일 발송
        sendTempPasswordEmail(email, tempPassword);
        return true;
    }

    // 이메일 발송
    private void sendTempPasswordEmail(String to, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 초기화 안내");
        message.setText("임시 비밀번호: " + tempPassword + "\n로그인 후 반드시 비밀번호를 변경하세요.");
        mailSender.send(message);
    }

    // 임시 비밀번호 생성 (더 안전한 랜덤)
    private String generateRandomPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public boolean changePassword(int mno, String currentPassword, String newPassword) {
        Optional<User> userOpt = findByMno(mno);
        if (userOpt.isEmpty())
            return false;
        User user = userOpt.get();
        // 현재 비밀번호 확인
        if (!PasswordUtil.checkPassword(currentPassword, user.getPassword())) {
            return false;
        }
        // 새 비밀번호 암호화 후 저장
        String hashedNewPassword = PasswordUtil.hashPassword(newPassword);
        user.setPassword(hashedNewPassword);
        return userMapper.update(user, mno) == 1;
    }
}
