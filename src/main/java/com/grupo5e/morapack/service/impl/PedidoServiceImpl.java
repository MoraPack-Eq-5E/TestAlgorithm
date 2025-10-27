package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.repository.PedidoRepository;
import com.grupo5e.morapack.service.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Pedido pedido) {
        return pedidoRepository.save(pedido).getId();
    }

    @Override
    @Transactional
    public Pedido actualizar(Long id, Pedido pedido) {
        Pedido existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedido.setId(id);
        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    @Override
    public List<Pedido> buscarPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public Pedido actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        if (pedido == null) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedidoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return pedidoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Pedido> insertarBulk(List<Pedido> pedidos) {
        return pedidoRepository.saveAll(pedidos).stream().collect(Collectors.toList());
    }
}
