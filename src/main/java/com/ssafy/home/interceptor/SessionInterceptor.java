package com.ssafy.home.interceptor;

import com.ssafy.home.domain.User;
import com.ssafy.home.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    private static final String LOGIN_COOKIE = "loginEmail";

    private final UserService userService;

    public SessionInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("loginUser");

        // 세션에 사용자 정보가 없으면 쿠키 확인
        if (user == null) {
            Cookie cookie = WebUtils.getCookie(request, LOGIN_COOKIE);
            String email = cookie != null ? cookie.getValue() : null;
            if (email != null) {
                // 서비스 호출하여 사용자 정보 조회
                user = userService.findActiveUserByEmail(email).orElse(null);
                if (user != null) {
                    session.setAttribute("loginUser", user);
                }
            }
        }

        // 로그인 여부 확인
        user = (User) session.getAttribute("loginUser");
        if (user == null) {
            session.setAttribute("alertMsg", "로그인 후 사용하세요.");
            response.sendRedirect(request.getContextPath() + "/user/login");
            return false;
        }

        String uri = request.getRequestURI();
        // 관리자 권한 필요한 경로 처리
        if (uri.startsWith(request.getContextPath() + "/user/admin")) {
            if (!"admin".equals(user.getRole())) {
                session.setAttribute("alertMsg", "관리자만 접근할 수 있습니다.");
                response.sendRedirect(request.getContextPath() + "/");
                return false;
            }
        }

        return true;
    }
}
