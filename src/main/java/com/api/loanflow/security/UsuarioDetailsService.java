package com.api.loanflow.security;

import com.api.loanflow.usuario.domain.UsuarioStatus;
import com.api.loanflow.usuario.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {
	private final UsuarioRepository usuarioRepository;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var usuario = usuarioRepository.findByEmail(username.toLowerCase())
			.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
		return new UsuarioAutenticado(
			usuario.getId(),
			usuario.getEmail(),
			usuario.getSenhaHash(),
			usuario.getPapel(),
			usuario.getStatus() == UsuarioStatus.ATIVO
		);
	}
}
