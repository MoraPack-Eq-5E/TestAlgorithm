package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByPedidoId(Long pedidoId);
    List<Producto> findByEstado(EstadoProducto estado);
}
