package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteId(Long clienteId);
    List<Pedido> findByEstado(EstadoPedido estado);
}
