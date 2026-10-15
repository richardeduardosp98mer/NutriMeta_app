package com.nutrimeta.backend.security;

import com.nutrimeta.backend.entity.Administrador;
import com.nutrimeta.backend.repository.AdministradorRepositorio;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioDetallesAdminisgrador implements UserDetailsService{

    private final AdministradorRepositorio repositorio;

    public ServicioDetallesAdminisgrador(AdministradorRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Administrador admin = repositorio.findByIdCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Correo o contraseña incorrectas"));

        return User.withUsername(admin.getCorreo())
                .password(admin.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .disabled(!admin.getActivo())
                .build();
    }
}
