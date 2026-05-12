package com.api.loanflow.notificacao.infraestrutura.persistencia;

import com.api.loanflow.notificacao.dominio.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
	List<Notificacao> findByUsuarioIdOrderByDataEnvioDesc(Long usuarioId);
}
