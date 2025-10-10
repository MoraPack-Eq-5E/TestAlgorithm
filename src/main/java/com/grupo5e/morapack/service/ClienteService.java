package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    List<Cliente> listar();
    Long insertar(Cliente cliente);
    Cliente  buscarPorId(Long idCliente);
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    void eliminar(Long id);
}
