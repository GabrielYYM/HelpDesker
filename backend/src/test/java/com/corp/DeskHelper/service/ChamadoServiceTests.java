package com.corp.DeskHelper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.dto.ChamadoDTO.Abertura;
import com.corp.DeskHelper.dto.ChamadoDTO.Edicao;
import com.corp.DeskHelper.dto.ChamadoDTO.Nota;
import com.corp.DeskHelper.dto.ChamadoDTO.Transicao;
import com.corp.DeskHelper.model.Chamado;
import com.corp.DeskHelper.model.Chamado.Acao;
import com.corp.DeskHelper.model.Chamado.Alteracao;
import com.corp.DeskHelper.model.Chamado.Evento;
import com.corp.DeskHelper.model.enums.StatusChamado;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.ChamadoRepository;
import com.corp.DeskHelper.service.AuthService;
import com.corp.DeskHelper.service.ChamadoService;

class ChamadoServiceTests {

    private final ChamadoRepository repository = mock(ChamadoRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final AuthService authService = mock(AuthService.class);
    private final ChamadoService service = new ChamadoService(repository, mongoTemplate, authService);

    @BeforeEach
    void configureRepository() {
        when(repository.insert(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            ReflectionTestUtils.setField(chamado, "versao", 0L);
            when(repository.findById(chamado.getProtocolo())).thenReturn(Optional.of(chamado));
            return chamado;
        });
        when(repository.save(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            ReflectionTestUtils.setField(chamado, "versao", chamado.getVersao() + 1);
            return chamado;
        });
        autenticar("0010", UserRole.USER);
    }

    @Test
    void fluxoDoTecnicoRegistraEdicaoComentariosTransicoesEAutor() {
        var abertura = service.abrir(new Abertura("Tela", "Sem imagem"));
        String protocolo = abertura.resumo().protocolo();
        assertThat(protocolo).matches("CH-[0-9A-F]{12}");
        assertThat(abertura.resumo().status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(abertura.acoes()).containsExactlyInAnyOrder(Acao.EDICAO, Acao.COMENTARIO, Acao.CANCELAMENTO);

        var editado = service.editar(protocolo, new Edicao("Monitor", "Sem imagem", abertura.versao()));
        assertThat(editado.historico().getLast().alteracoes())
            .containsExactly(new Alteracao("Título", "Tela", "Monitor"));

        autenticar("0020", UserRole.IT);
        var atendimento = service.transicionar(protocolo,
            new Transicao(StatusChamado.EM_ANDAMENTO, "Testar a conexão."));
        assertThat(atendimento.resumo().tecnico().codigo()).isEqualTo("0020");
        assertThat(atendimento.acoes()).contains(Acao.RESOLUCAO);
        assertThat(atendimento.historico().getLast().statusAnterior()).isEqualTo(StatusChamado.ABERTO);
        assertThatThrownBy(() -> service.transicionar(protocolo,
            new Transicao(StatusChamado.EM_ANDAMENTO, "Assumir novamente.")))
            .isInstanceOf(IllegalStateException.class);

        autenticar("0030", UserRole.IT);
        assertThatThrownBy(() -> service.transicionar(protocolo,
            new Transicao(StatusChamado.RESOLVIDO, "Não sou o responsável.")))
            .isInstanceOf(AccessDeniedException.class);

        autenticar("0020", UserRole.IT);
        service.comentar(protocolo, new Nota("Identifiquei cabo defeituoso."));
        var resolvido = service.transicionar(protocolo, new Transicao(StatusChamado.RESOLVIDO, "Cabo substituído."));
        assertThat(resolvido.resumo().status()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(resolvido.historico()).extracting(Evento::acao).containsExactly(
            Acao.ABERTURA, Acao.EDICAO, Acao.INICIO_ATENDIMENTO, Acao.COMENTARIO, Acao.RESOLUCAO);
        assertThat(resolvido.historico().getLast().comentario()).isEqualTo("Cabo substituído.");
        assertThat(resolvido.historico().getLast().autor().codigo()).isEqualTo("0020");
        assertThat(resolvido.acoes()).containsExactly(Acao.COMENTARIO);
    }

    @Test
    void solicitantePodeCancelarEContinuarComentandoSemReabrir() {
        var chamado = service.abrir(new Abertura("Impressão", "Preciso de ajuda"));
        String protocolo = chamado.resumo().protocolo();

        var cancelado = service.transicionar(protocolo, new Transicao(StatusChamado.CANCELADO, "Resolvi por conta própria."));
        assertThat(cancelado.resumo().status()).isEqualTo(StatusChamado.CANCELADO);
        assertThat(cancelado.historico().getLast().comentario()).isEqualTo("Resolvi por conta própria.");
        assertThat(service.comentar(protocolo, new Nota("Pode arquivar.")).historico().getLast().status())
            .isEqualTo(StatusChamado.CANCELADO);
        assertThatThrownBy(() -> service.transicionar(protocolo, new Transicao(StatusChamado.ABERTO, "Reabrir")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void solicitanteNaoPodeConsultarChamadosDeOutroUsuario() {
        String protocolo = service.abrir(new Abertura("Impressão", "Preciso de ajuda")).resumo().protocolo();
        autenticar("0099", UserRole.USER);

        assertThatThrownBy(() -> service.buscar(protocolo))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                erro -> assertThat(erro.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void listaDeUsuarioFiltraSolicitanteEStatusNoMongo() {
        var consulta = ArgumentCaptor.forClass(Query.class);
        when(mongoTemplate.find(consulta.capture(), eq(Chamado.class))).thenReturn(List.of());

        assertThat(service.listar(StatusChamado.RESOLVIDO, 1).chamados()).isEmpty();
        assertThat(consulta.getValue().getQueryObject())
            .containsEntry("solicitante.codigo", "0010")
            .containsEntry("status", StatusChamado.RESOLVIDO);
        assertThat(consulta.getValue().getSkip()).isEqualTo(20);
        assertThat(consulta.getValue().getLimit()).isEqualTo(21);
        assertThat(consulta.getValue().getFieldsObject()).containsEntry("historico", 0);
    }

    @Test
    void edicaoDesatualizadaEGravacaoConcorrenteSaoRejeitadas() {
        String protocolo = service.abrir(new Abertura("Rede", "Sem conexão")).resumo().protocolo();
        assertThatThrownBy(() -> service.editar(protocolo, new Edicao("Outro", "Texto", 1L)))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Recarregue");

        when(repository.save(any(Chamado.class))).thenThrow(new OptimisticLockingFailureException("teste"));
        assertThatThrownBy(() -> service.comentar(protocolo, new Nota("Atualização")))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Recarregue");
    }

    private void autenticar(String codigo, UserRole perfil) {
        when(authService.getCurrentUser()).thenReturn(new UserResponseDTO(codigo, codigo, "Pessoa " + codigo, perfil, false, true));
    }
}
