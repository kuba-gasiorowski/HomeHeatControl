package com.sasieczno.homeheat.manager.security;

import com.sasieczno.homeheat.manager.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ManagerUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    AppConfig appConfig;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("admin".equals(username)) {
            return getUserDetails(username, appConfig.adminPassword);
        } else if ("user".equals(username)) {
            return getUserDetails(username, appConfig.userPassword);
        } else {
            return null;
        }
    }

    private UserDetails getUserDetails(String username, String password) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return null;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getUsername() {
                return username;
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
        };
    }
}
