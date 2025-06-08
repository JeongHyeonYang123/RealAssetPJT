package com.ssafy.home.dto;

import com.ssafy.home.domain.User;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(@NonNull User user) {
        this.user = user;
        this.authorities = user.getRole() != null ? 
            java.util.Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())) :
            java.util.Collections.emptySet();
    }

    public CustomUserDetails(@NonNull User user, @NonNull Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Integer getMno() {
        return user.getMno();
    }

    public String getName() {
        return user.getName();
    }
}
