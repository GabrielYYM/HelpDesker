package com.corp.DeskHelper.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.corp.DeskHelper.dto.DashboardDTO;
import com.corp.DeskHelper.dto.ChamadoDTO.Resumo;
import com.corp.DeskHelper.dto.DashboardDTO.CargaTecnico;
import com.corp.DeskHelper.dto.DashboardDTO.VolumeDiario;
import com.corp.DeskHelper.model.Chamado;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.Chamado.Acao;
import com.corp.DeskHelper.model.Chamado.Evento;
import com.corp.DeskHelper.model.Chamado.Pessoa;
import com.corp.DeskHelper.model.enums.StatusChamado;
import com.corp.DeskHelper.model.enums.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS = 30;
    private static final int MAX_RECENTES = 5;
    private static final Comparator<Chamado> ORDEM_RECENTES = Comparator.comparing(Chamado::getCriadoEm)
        .thenComparing(Chamado::getProtocolo);

    private final MongoTemplate mongoTemplate;

    public DashboardDTO consultar() {
        Query usuarios = Query.query(Criteria.where("active").is(true));
        usuarios.fields().include("code", "name", "role").exclude("_id");
        var tecnicos = mongoTemplate.find(usuarios, User.class).stream()
            .filter(user -> user.getRole() == UserRole.IT || user.getRole() == UserRole.ADM)
            .map(user -> new Pessoa(user.getCode(), user.getName(), user.getRole()))
            .toList();

        Query chamados = new Query();
        chamados.fields().include("status", "tecnico", "solicitante", "titulo", "criadoEm",
            "atualizadoEm", "historico.acao", "historico.data");
        try (Stream<Chamado> registros = mongoTemplate.stream(chamados, Chamado.class)) {
            return calcular(Instant.now(), tecnicos, registros);
        }
    }

    static DashboardDTO calcular(Instant agora, List<Pessoa> tecnicosAtivos, Stream<Chamado> chamados) {
        LocalDate fim = dia(agora);
        LocalDate inicio = fim.minusDays(DIAS - 1);
        Map<LocalDate, ContagemDiaria> volume = new LinkedHashMap<>();
        inicio.datesUntil(fim.plusDays(1)).forEach(data -> volume.put(data, new ContagemDiaria()));

        Map<StatusChamado, Integer> porStatus = new EnumMap<>(StatusChamado.class);
        for (StatusChamado status : StatusChamado.values()) {
            porStatus.put(status, 0);
        }

        Map<String, Carga> cargas = new HashMap<>();
        tecnicosAtivos.forEach(tecnico -> cargas.put(tecnico.codigo(), new Carga(tecnico)));
        TempoMedio tma = new TempoMedio();
        var recentes = new PriorityQueue<>(ORDEM_RECENTES);

        chamados.forEach(chamado -> {
            porStatus.merge(chamado.getStatus(), 1, Integer::sum);
            recentes.add(chamado);
            if (recentes.size() > MAX_RECENTES) {
                recentes.poll();
            }

            var abertura = volume.get(dia(chamado.getCriadoEm()));
            if (abertura != null) {
                abertura.abertos++;
            }

            Pessoa tecnico = chamado.getTecnico();
            if (chamado.getStatus() == StatusChamado.EM_ANDAMENTO && tecnico != null) {
                cargas.computeIfAbsent(tecnico.codigo(), codigo -> new Carga(tecnico)).emAndamento++;
            }

            if (chamado.getStatus() == StatusChamado.RESOLVIDO) {
                registrarResolucao(chamado, tecnico, volume, cargas, tma);
            }
        });

        var serie = volume.entrySet().stream().map(item ->
            new VolumeDiario(item.getKey(), item.getValue().abertos, item.getValue().resolvidos)).toList();
        var porTecnico = cargas.values().stream().map(Carga::resumo)
            .sorted(Comparator.comparingInt(CargaTecnico::emAndamento).reversed()
                .thenComparing(CargaTecnico::nome, String.CASE_INSENSITIVE_ORDER).thenComparing(CargaTecnico::codigo))
            .toList();
        var ultimos = recentes.stream().sorted(ORDEM_RECENTES.reversed()).map(Resumo::de).toList();
        int total = porStatus.values().stream().mapToInt(Integer::intValue).sum();
        int resolvidos = serie.stream().mapToInt(VolumeDiario::resolvidos).sum();
        return new DashboardDTO(inicio, fim, total, porStatus, serie, tma.minutos(), resolvidos, porTecnico, ultimos);
    }

    private static void registrarResolucao(Chamado chamado, Pessoa tecnico, Map<LocalDate, ContagemDiaria> volume,
            Map<String, Carga> cargas, TempoMedio tma) {
        Instant inicio = null;
        for (Evento evento : chamado.getHistorico()) {
            if (evento.acao() == Acao.INICIO_ATENDIMENTO) {
                inicio = evento.data();
            } else if (evento.acao() == Acao.RESOLUCAO) {
                var diaResolucao = volume.get(dia(evento.data()));
                if (diaResolucao != null) {
                    diaResolucao.resolvidos++;
                    Carga carga = tecnico == null ? null :
                        cargas.computeIfAbsent(tecnico.codigo(), codigo -> new Carga(tecnico));
                    if (carga != null) {
                        carga.resolvidos++;
                    }
                    if (inicio != null && !evento.data().isBefore(inicio)) {
                        Duration duracao = Duration.between(inicio, evento.data());
                        tma.adicionar(duracao);
                        if (carga != null) {
                            carga.tma.adicionar(duracao);
                        }
                    }
                }
                return;
            }
        }
    }

    private static LocalDate dia(Instant instante) {
        return instante.atZone(FUSO).toLocalDate();
    }

    private static final class ContagemDiaria {
        int abertos;
        int resolvidos;
    }

    private static final class TempoMedio {
        double segundos;
        int total;

        void adicionar(Duration duracao) {
            segundos += duracao.toMillis() / 1000.0;
            total++;
        }

        Integer minutos() {
            return total == 0 ? null : (int) Math.round(segundos / total / 60);
        }
    }

    private static final class Carga {
        final Pessoa tecnico;
        final TempoMedio tma = new TempoMedio();
        int emAndamento;
        int resolvidos;

        Carga(Pessoa tecnico) {
            this.tecnico = tecnico;
        }

        CargaTecnico resumo() {
            return new CargaTecnico(tecnico.codigo(), tecnico.nome(), emAndamento, resolvidos, tma.minutos());
        }
    }
}
