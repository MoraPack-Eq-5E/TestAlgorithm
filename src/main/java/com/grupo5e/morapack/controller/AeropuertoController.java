package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.service.AeropuertoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aeropuertos")
public class AeropuertoController {
    private final AeropuertoService service;

    public AeropuertoController(AeropuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Aeropuerto> listar() {
        return service.listar();
    }

    @PostMapping
    public Long insertar(@RequestBody Aeropuerto aeropuerto) {
        return service.insertar(aeropuerto);
    }

    @GetMapping("/{id}")
    public Aeropuerto obtener(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
