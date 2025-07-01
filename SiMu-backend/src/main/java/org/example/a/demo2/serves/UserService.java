package org.example.a.demo2.serves;

import org.example.a.demo2.DAO.UserRepository;
import org.example.a.demo2.entity.User;
import org.example.a.demo2.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException(String.format("User '%s' not found", username)));

        return UserDetailsImpl.build(user);

    }

    /*@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Загрузите пользователя из базы данных
        // Пример:
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Добавьте нужные роли
        return new org.springframework.security.core.userdetails.User(
                username,
                "encodedPassword", // Пароль (может быть пустым для JWT)
                authorities
        );
    }*/

}
