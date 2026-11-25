package com.corp.DeskHelper.chamado;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.corp.DeskHelper.model.enums.UserRole;

import lombok.Getter;

@Getter
@Document(collection = "chamados")
public class Chamado {

    @Id
    private String protocolo;

    @Version
    private Long versao;

    private String titulo;
    private String descricao;
    private StatusChamado status;
    private Pessoa solicitante;
    private Pessoa tecnico;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private List<Evento> historico = new ArrayList<>();

    static Chamado abrir(String protocolo, String titulo, String descricao, Pessoa solicitante, Instant agora) {
        Chamado chamado = new Chamado();
        chamado.protocolo = protocolo;
        chamado.titulo = titulo;
        chamado.descricao = descricao;
        chamado.status = StatusChamado.ABERTO;
        chamado.solicitante = solicitante;
        chamado.criadoEm = agora;
        chamado.registrar(Acao.ABERTURA, solicitante, null, descricao, List.of(), agora);
        return chamado;
    }

    boolean editar(String novoTitulo, String novaDescricao, Pessoa autor, Instant agora) {
        List<Alteracao> alteracoes = new ArrayList<>();
        if (!titulo.equals(novoTitulo)) {
            alteracoes.add(new Alteracao("Título", titulo, novoTitulo));
        }
        if (!descricao.equals(novaDescricao)) {
            alteracoes.add(new Alteracao("Descrição", descricao, novaDescricao));
        }
        if (alteracoes.isEmpty()) {
            return false;
        }
        titulo = novoTitulo;
        descricao = novaDescricao;
        registrar(Acao.EDICAO, autor, status, null, alteracoes, agora);
        return true;
    }

    void comentar(Pessoa autor, String comentario, Instant agora) {
        registrar(Acao.COMENTARIO, autor, status, comentario, List.of(), agora);
    }

    void iniciar(Pessoa autor, String comentario, Instant agora) {
        tecnico = autor;
        transicionar(Acao.INICIO_ATENDIMENTO, StatusChamado.EM_ANDAMENTO, autor, comentario, agora);
    }

    void resolver(Pessoa autor, String comentario, Instant agora) {
        transicionar(Acao.RESOLUCAO, StatusChamado.RESOLVIDO, autor, comentario, agora);
    }

    void cancelar(Pessoa autor, String comentario, Instant agora) {
        transicionar(Acao.CANCELAMENTO, StatusChamado.CANCELADO, autor, comentario, agora);
    }

    private void transicionar(Acao acao, StatusChamado destino, Pessoa autor, String comentario, Instant agora) {
        StatusChamado anterior = status;
        status = destino;
        registrar(acao, autor, anterior, comentario, List.of(), agora);
    }

    private void registrar(Acao acao, Pessoa autor, StatusChamado anterior, String comentario,
            List<Alteracao> alteracoes, Instant agora) {
        atualizadoEm = agora;
        historico.add(new Evento(acao, autor, anterior, status, comentario, alteracoes, agora));
    }

    public enum Acao {
        ABERTURA, EDICAO, COMENTARIO, INICIO_ATENDIMENTO, RESOLUCAO, CANCELAMENTO
    }

    public record Pessoa(String codigo, String nome, UserRole perfil) {}

    public record Evento(Acao acao, Pessoa autor, StatusChamado statusAnterior, StatusChamado status,
            String comentario, List<Alteracao> alteracoes, Instant data) {}

    public record Alteracao(String campo, String anterior, String atual) {}
}
