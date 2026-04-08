package com.shin.chat.jwt;

import com.shin.chat.domain.entity.UserEntity;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security에서 인증된 사용자는 UserDetails 인터페이스를 통해 관리됨. 해당 인터페이스를 구현하여 UserEntity에 맞추어 사용.
@Getter
public class CustomUserDetails implements UserDetails {

    private final UserEntity user;

    public CustomUserDetails(UserEntity user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        /**
         * Spring Security 에서는 사용자 권한을 GrantedAuthority 인터페이스를 구현한 객체로 표현함이 요구된다.
         * SimpleGrantedAuthority 클래스는 GrantedAuthority 인터페이스를 구현한 클래스 중 하나로,
         * 권한을 문자열로 표현하는 역할을 한다.
        **/
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
