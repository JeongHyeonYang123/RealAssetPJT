package com.ssafy.home.dto;

import com.ssafy.home.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security에서 사용되는 UserDetails 인터페이스를 정의한 DTO 클래스입니다.
 */
@Slf4j
@Getter
@AllArgsConstructor
public class UserDetailsDTO implements UserDetails {
    // @Delegate 어노테이션을 사용하여 UserDto 객체의 메서드를 이 클래스에서 직접 사용할 수 있게 합니다.
    @Delegate
    private User user;

    private Collection<? extends GrantedAuthority> authorities;

    // 사용자의 권한 목록을 반환합니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 사용자의 비밀번호를 반환합니다.
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 사용자의 이름을 반환합니다.
    @Override
    public String getUsername() {
        return user.getName();
    }

    /**
     * 계정이 만료되지 않았는지 여부를 반환합니다.
     * 현재 항상 true를 반환하므로, 모든 계정이 만료되지 않은 것으로 처리됩니다.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정이 잠기지 않았는지 여부를 반환합니다.
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 자격 증명(비밀번호)이 만료되지 않았는지 여부를 반환합니다.
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정이 활성화되어 있는지 여부를 반환합니다.
    @Override
    public boolean isEnabled() {
        return true;
    }
}
