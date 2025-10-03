package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.repository.ClienteRepository;
import com.grupo5e.morapack.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Override
    public Long insertar(Cliente cliente) {
        return clienteRepository.save(cliente).getId();
    }

    @Override
    public Optional<Cliente> findByNumeroDocumento(String numeroDocumento) {
        // 1. Recuperar todos los clientes
        List<Cliente> clientes = clienteRepository.findAll();

        // 2. Buscar manualmente el que coincida
        return clientes.stream()
                .filter(c -> c.getNumeroDocumento().equals(numeroDocumento))
                .findFirst();
    }

    @Override
    public void eliminar(Long id) {
        clienteRepository.deleteById(id);
    }
}
