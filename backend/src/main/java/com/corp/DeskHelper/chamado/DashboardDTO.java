package com.corp.DeskHelper.chamado;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.corp.DeskHelper.chamado.ChamadoDTO.Resumo;

public record DashboardDTO(
    LocalDate inicio, LocalDate fim, int total, Map<StatusChamado, Integer> porStatus,
    List<VolumeDiario> volume, Integer tmaMinutos, int resolvidosNoPeriodo,
    List<CargaTecnico> tecnicos, List<Resumo> recentes
) {
    public record VolumeDiario(LocalDate data, int abertos, int resolvidos) {}
    public record CargaTecnico(String codigo, String nome, int emAndamento,
            int resolvidosNoPeriodo, Integer tmaMinutos) {}
}
