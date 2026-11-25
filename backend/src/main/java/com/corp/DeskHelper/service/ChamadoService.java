package com.corp.DeskHelper.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.corp.DeskHelper.dto.ChamadoDTO.Abertura;
import com.corp.DeskHelper.dto.ChamadoDTO.Detalhe;
import com.corp.DeskHelper.dto.ChamadoDTO.Edicao;
import com.corp.DeskHelper.dto.ChamadoDTO.Nota;
import com.corp.DeskHelper.dto.ChamadoDTO.Pagina;
import com.corp.DeskHelper.dto.ChamadoDTO.Resumo;
import com.corp.DeskHelper.dto.ChamadoDTO.Transicao;
import com.corp.DeskHelper.model.Chamado;
import com.corp.DeskHelper.model.Chamado.Acao;
import com.corp.DeskHelper.model.Chamado.Pessoa;
import com.corp.DeskHelper.model.enums.StatusChamado;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.ChamadoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private static final int TAMANHO_PAGINA = 20;
    private final ChamadoRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AuthService authService;

    public Pagina listar(StatusChamado status, int pagina) {
        Pessoa autor = pessoaAtual();
        Query query = new Query()
            .with(Sort.by(Sort.Direction.DESC, "atualizadoEm", "protocolo"))
            .skip((long) pagina * TAMANHO_PAGINA)
            .limit(TAMANHO_PAGINA + 1);
        query.fields().exclude("historico").exclude("descricao");
        if (!equipe(autor)) {
            query.addCriteria(Criteria.where("solicitante.codigo").is(autor.codigo()));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        var chamados = mongoTemplate.find(query, Chamado.class);
        return new Pagina(chamados.stream().limit(TAMANHO_PAGINA).map(Resumo::de).toList(),
            chamados.size() > TAMANHO_PAGINA);
    }

    public Detalhe abrir(Abertura dados) {
        Pessoa autor = pessoaAtual();
        DuplicateKeyException colisao = null;
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            String protocolo = "CH-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
            Chamado chamado = Chamado.abrir(protocolo, dados.titulo().trim(), dados.descricao().trim(),
                autor, Instant.now());
            try {
                return detalhe(repository.insert(chamado), autor);
            } catch (DuplicateKeyException ex) {
                colisao = ex;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um protocolo. Tente novamente.", colisao);
    }

    public Detalhe buscar(String protocolo) {
        Pessoa autor = pessoaAtual();
        return detalhe(carregar(protocolo, autor), autor);
    }

    public Detalhe editar(String protocolo, Edicao dados) {
        Pessoa autor = pessoaAtual();
        Chamado chamado = carregar(protocolo, autor);
        exigirAcao(chamado, autor, Acao.EDICAO);
        if (!Objects.equals(chamado.getVersao(), dados.versao())) {
            throw conflito();
        }
        boolean alterado = chamado.editar(dados.titulo().trim(), dados.descricao().trim(), autor, Instant.now());
        return alterado ? salvar(chamado, autor) : detalhe(chamado, autor);
    }

    public Detalhe comentar(String protocolo, Nota dados) {
        Pessoa autor = pessoaAtual();
        Chamado chamado = carregar(protocolo, autor);
        exigirAcao(chamado, autor, Acao.COMENTARIO);
        chamado.comentar(autor, dados.comentario().trim(), Instant.now());
        return salvar(chamado, autor);
    }

    public Detalhe transicionar(String protocolo, Transicao dados) {
        Pessoa autor = pessoaAtual();
        Chamado chamado = carregar(protocolo, autor);
        String comentario = dados.comentario().trim();
        Instant agora = Instant.now();

        switch (dados.status()) {
            case ABERTO -> throw new IllegalStateException("O chamado não pode retornar ao estado aberto.");
            case EM_ANDAMENTO -> {
                exigirAcao(chamado, autor, Acao.INICIO_ATENDIMENTO);
                chamado.iniciar(autor, comentario, agora);
            }
            case RESOLVIDO -> {
                exigirAcao(chamado, autor, Acao.RESOLUCAO);
                chamado.resolver(autor, comentario, agora);
            }
            case CANCELADO -> {
                exigirAcao(chamado, autor, Acao.CANCELAMENTO);
                chamado.cancelar(autor, comentario, agora);
            }
        }
        return salvar(chamado, autor);
    }

    private Detalhe salvar(Chamado chamado, Pessoa autor) {
        try {
            return detalhe(repository.save(chamado), autor);
        } catch (OptimisticLockingFailureException ex) {
            throw conflito();
        }
    }

    private IllegalStateException conflito() {
        return new IllegalStateException("O chamado foi atualizado. Recarregue os dados e tente novamente.");
    }

    private Chamado carregar(String protocolo, Pessoa autor) {
        return repository.findById(protocolo)
            .filter(chamado -> equipe(autor) || chamado.getSolicitante().codigo().equals(autor.codigo()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Pessoa pessoaAtual() {
        var user = authService.getCurrentUser();
        if (!user.active()) {
            throw new AccessDeniedException("Conta inativa.");
        }
        return new Pessoa(user.code(), user.name(), user.role());
    }

    private Detalhe detalhe(Chamado chamado, Pessoa autor) {
        return new Detalhe(Resumo.de(chamado), chamado.getDescricao(), chamado.getVersao(),
            List.copyOf(chamado.getHistorico()), acoes(chamado, autor));
    }

    private Set<Acao> acoes(Chamado chamado, Pessoa autor) {
        Set<Acao> acoes = acoesDoAutor(chamado, autor);
        acoes.removeIf(acao -> !estadoPermite(chamado.getStatus(), acao));
        return acoes;
    }

    private Set<Acao> acoesDoAutor(Chamado chamado, Pessoa autor) {
        Set<Acao> acoes = EnumSet.noneOf(Acao.class);
        boolean solicitante = chamado.getSolicitante().codigo().equals(autor.codigo());
        boolean tecnico = equipe(autor) && chamado.getTecnico() != null
            && chamado.getTecnico().codigo().equals(autor.codigo());
        boolean admin = autor.perfil() == UserRole.ADM;

        if (solicitante || tecnico || admin) {
            acoes.add(Acao.COMENTARIO);
        }
        if (solicitante || admin) {
            acoes.add(Acao.EDICAO);
        }
        if (equipe(autor)) {
            acoes.add(Acao.INICIO_ATENDIMENTO);
        }
        if (tecnico) {
            acoes.add(Acao.RESOLUCAO);
        }
        if (admin || tecnico || (solicitante && chamado.getStatus() == StatusChamado.ABERTO)) {
            acoes.add(Acao.CANCELAMENTO);
        }
        return acoes;
    }

    private boolean estadoPermite(StatusChamado status, Acao acao) {
        return switch (acao) {
            case EDICAO, INICIO_ATENDIMENTO -> status == StatusChamado.ABERTO;
            case RESOLUCAO -> status == StatusChamado.EM_ANDAMENTO;
            case CANCELAMENTO -> status == StatusChamado.ABERTO || status == StatusChamado.EM_ANDAMENTO;
            case COMENTARIO -> true;
            case ABERTURA -> false;
        };
    }

    private void exigirAcao(Chamado chamado, Pessoa autor, Acao acao) {
        if (!estadoPermite(chamado.getStatus(), acao)) {
            throw new IllegalStateException("Essa ação não está disponível no estado atual do chamado.");
        }
        if (!acoesDoAutor(chamado, autor).contains(acao)) {
            throw new AccessDeniedException("Sem permissão para executar esta ação.");
        }
    }

    private boolean equipe(Pessoa pessoa) {
        return pessoa.perfil() == UserRole.IT || pessoa.perfil() == UserRole.ADM;
    }
}
