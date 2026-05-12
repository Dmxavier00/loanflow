package com.api.loanflow.notificacao.infrastructure.persistence;

import com.api.loanflow.notificacao.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
	List<Notificacao> findByUsuarioIdOrderByDataEnvioDesc(Long usuarioId);
}
