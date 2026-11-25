package com.corp.DeskHelper.chamado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.corp.DeskHelper.chamado.Chamado.Pessoa;
import com.corp.DeskHelper.chamado.DashboardDTO.CargaTecnico;
import com.corp.DeskHelper.chamado.DashboardDTO.VolumeDiario;
import com.corp.DeskHelper.model.enums.UserRole;

class DashboardServiceTests {

    private static final Instant AGORA = Instant.parse("2026-11-25T12:00:00Z");
    private static final Pessoa SOLICITANTE = new Pessoa("0001", "Solicitante", UserRole.USER);
    private static final Pessoa TECNICA = new Pessoa("0002", "Ana", UserRole.IT);
    private static final Pessoa ADMIN = new Pessoa("0003", "Bruno", UserRole.ADM);

    @Test
    void contabilizaEventosNaDataLocalEUsaSomenteInicioEResolucaoNoTma() {
        var anterior = chamado("1", "2026-10-26T23:00:00Z");
        anterior.iniciar(TECNICA, "Atender", Instant.parse("2026-10-27T02:00:00Z"));
        anterior.resolver(TECNICA, "Resolvido", Instant.parse("2026-10-27T03:30:00Z"));
        anterior.comentar(SOLICITANTE, "Obrigado", AGORA);

        var resolvido = chamado("2", "2026-11-24T22:00:00Z");
        resolvido.iniciar(ADMIN, "Atender", Instant.parse("2026-11-25T00:00:00Z"));
        resolvido.resolver(ADMIN, "Resolvido", Instant.parse("2026-11-25T00:30:00Z"));

        var andamento = chamado("3", "2026-11-25T04:00:00Z");
        andamento.iniciar(TECNICA, "Atender", AGORA);

        var cancelado = chamado("4", "2026-11-25T05:00:00Z");
        cancelado.cancelar(SOLICITANTE, "Dispensado", AGORA);

        var dashboard = DashboardService.calcular(AGORA, List.of(TECNICA, ADMIN),
            Stream.of(anterior, resolvido, andamento, cancelado));

        assertThat(dashboard.inicio()).isEqualTo(LocalDate.parse("2026-10-27"));
        assertThat(dashboard.fim()).isEqualTo(LocalDate.parse("2026-11-25"));
        assertThat(dashboard.volume()).hasSize(30);
        assertThat(dashboard.volume().getFirst()).isEqualTo(new VolumeDiario(dashboard.inicio(), 0, 1));
        assertThat(dashboard.volume().get(28)).isEqualTo(new VolumeDiario(LocalDate.parse("2026-11-24"), 1, 1));
        assertThat(dashboard.volume().getLast()).isEqualTo(new VolumeDiario(dashboard.fim(), 2, 0));
        assertThat(dashboard.total()).isEqualTo(4);
        assertThat(dashboard.porStatus()).containsEntry(StatusChamado.ABERTO, 0)
            .containsEntry(StatusChamado.EM_ANDAMENTO, 1)
            .containsEntry(StatusChamado.CANCELADO, 1)
            .containsEntry(StatusChamado.RESOLVIDO, 2);
        assertThat(dashboard.resolvidosNoPeriodo()).isEqualTo(2);
        assertThat(dashboard.tmaMinutos()).isEqualTo(60);
        assertThat(dashboard.tecnicos()).containsExactly(
            new CargaTecnico("0002", "Ana", 1, 1, 90),
            new CargaTecnico("0003", "Bruno", 0, 1, 30));
        assertThat(dashboard.recentes()).extracting(ChamadoDTO.Resumo::protocolo).containsExactly("4", "3", "2", "1");
    }

    @Test
    void chamadosAnterioresAoPeriodoNaoAfetamOTmaETecnicosSemCargaSaoExibidos() {
        var antigo = chamado("1", "2026-10-01T09:00:00Z");
        antigo.iniciar(TECNICA, "Atender", Instant.parse("2026-10-01T10:00:00Z"));
        antigo.resolver(TECNICA, "Resolvido", Instant.parse("2026-10-01T11:00:00Z"));

        var dashboard = DashboardService.calcular(AGORA, List.of(TECNICA), Stream.of(antigo));

        assertThat(dashboard.tmaMinutos()).isNull();
        assertThat(dashboard.resolvidosNoPeriodo()).isZero();
        assertThat(dashboard.porStatus()).containsEntry(StatusChamado.RESOLVIDO, 1);
        assertThat(dashboard.tecnicos()).containsExactly(new CargaTecnico("0002", "Ana", 0, 0, null));
        assertThat(dashboard.volume()).allSatisfy(dia -> {
            assertThat(dia.abertos()).isZero();
            assertThat(dia.resolvidos()).isZero();
        });
    }

    private Chamado chamado(String protocolo, String criadoEm) {
        return Chamado.abrir(protocolo, "Título", "Descrição", SOLICITANTE, Instant.parse(criadoEm));
    }
}
