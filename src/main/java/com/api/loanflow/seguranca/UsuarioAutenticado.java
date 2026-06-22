package com.api.loanflow.seguranca;

import com.api.loanflow.usuario.dominio.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UsuarioAutenticado implements UserDetails {
	private final Long id;
	private final String email;
	private final String senhaHash;
	private final Role papel;
	private final boolean ativo;

	public UsuarioAutenticado(Long id, String email, String senhaHash, Role papel, boolean ativo) {
		this.id = id;
		this.email = email;
		this.senhaHash = senhaHash;
		this.papel = papel;
		this.ativo = ativo;
	}

	public Long getId() {
		return id;
	}

	public Role getPapel() {
		return papel;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (papel == null) {
			return List.of();
		}
		return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return ativo;
	}
}
