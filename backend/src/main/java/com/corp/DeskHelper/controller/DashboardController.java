package com.corp.DeskHelper.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corp.DeskHelper.dto.DashboardDTO;
import com.corp.DeskHelper.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'IT')")
    public DashboardDTO consultar() {
        return service.consultar();
    }
}
