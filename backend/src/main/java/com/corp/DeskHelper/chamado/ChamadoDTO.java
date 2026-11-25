package com.corp.DeskHelper.chamado;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.corp.DeskHelper.chamado.Chamado.Acao;
import com.corp.DeskHelper.chamado.Chamado.Evento;
import com.corp.DeskHelper.chamado.Chamado.Pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ChamadoDTO {

    private ChamadoDTO() {}

    public record Abertura(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 4000) String descricao
    ) {}

    public record Edicao(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 4000) String descricao,
        @NotNull @PositiveOrZero Long versao
    ) {}

    public record Nota(@NotBlank @Size(max = 4000) String comentario) {}

    public record Transicao(
        @NotNull StatusChamado status,
        @NotBlank @Size(max = 4000) String comentario
    ) {}

    public record Resumo(String protocolo, String titulo, StatusChamado status, Pessoa solicitante,
            Pessoa tecnico, Instant criadoEm, Instant atualizadoEm) {
        static Resumo de(Chamado chamado) {
            return new Resumo(chamado.getProtocolo(), chamado.getTitulo(), chamado.getStatus(),
                chamado.getSolicitante(), chamado.getTecnico(), chamado.getCriadoEm(), chamado.getAtualizadoEm());
        }
    }

    public record Detalhe(Resumo resumo, String descricao, Long versao, List<Evento> historico, Set<Acao> acoes) {}

    public record Pagina(List<Resumo> chamados, boolean temProximaPagina) {}
}
