package com.epiis.projectcasaketteler.service;

import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private RepositoryAdmin repositoryAdmin;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Buscar primero en usuarios
        Optional<EntityUser> userOptional = repositoryUser.findByEmail(email);

        if (userOptional.isPresent()) {
            EntityUser user = userOptional.get();
            if (!user.getActive()) {
                throw new UsernameNotFoundException("Usuario desactivado");
            }
            return new User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority(user.getRole().name())));
        }

        // Buscar en admins
        Optional<EntityAdmin> adminOptional = repositoryAdmin.findByEmail(email);

        if (adminOptional.isPresent()) {
            EntityAdmin admin = adminOptional.get();
            if (!admin.getActive()) {
                throw new UsernameNotFoundException("Administrador desactivado");
            }
            return new User(
                    admin.getEmail(),
                    admin.getPassword(),
                    List.of(new SimpleGrantedAuthority(admin.getRole().name())));
        }

        throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
    }
}