package com.corp.DeskHelper.chamado;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corp.DeskHelper.chamado.ChamadoDTO.Abertura;
import com.corp.DeskHelper.chamado.ChamadoDTO.Detalhe;
import com.corp.DeskHelper.chamado.ChamadoDTO.Edicao;
import com.corp.DeskHelper.chamado.ChamadoDTO.Nota;
import com.corp.DeskHelper.chamado.ChamadoDTO.Pagina;
import com.corp.DeskHelper.chamado.ChamadoDTO.Transicao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService service;

    @GetMapping
    public Pagina listar(@RequestParam(required = false) StatusChamado status,
            @RequestParam(defaultValue = "0") @PositiveOrZero int pagina) {
        return service.listar(status, pagina);
    }

    @PostMapping
    public ResponseEntity<Detalhe> abrir(@Valid @RequestBody Abertura dados) {
        Detalhe detalhe = service.abrir(dados);
        return ResponseEntity.created(URI.create("/api/chamados/" + detalhe.resumo().protocolo())).body(detalhe);
    }

    @GetMapping("/{protocolo}")
    public Detalhe buscar(@PathVariable String protocolo) {
        return service.buscar(protocolo);
    }

    @PatchMapping("/{protocolo}")
    public Detalhe editar(@PathVariable String protocolo, @Valid @RequestBody Edicao dados) {
        return service.editar(protocolo, dados);
    }

    @PostMapping("/{protocolo}/comentarios")
    public Detalhe comentar(@PathVariable String protocolo, @Valid @RequestBody Nota dados) {
        return service.comentar(protocolo, dados);
    }

    @PostMapping("/{protocolo}/transicoes")
    public Detalhe transicionar(@PathVariable String protocolo, @Valid @RequestBody Transicao dados) {
        return service.transicionar(protocolo, dados);
    }
}
