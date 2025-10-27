package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.service.ProductoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Producto producto) {
        return productoRepository.save(producto).getId();
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Producto", "id", id);
        }
        producto.setId(id);
        return productoRepository.save(producto);
    }

    @Override
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    @Override
    public List<Producto> buscarPorPedido(Long pedidoId) {
        return productoRepository.findByPedidoId(pedidoId);
    }

    @Override
    public List<Producto> buscarPorEstado(EstadoProducto estado) {
        return productoRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Producto", "id", id);
        }
        productoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return productoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Producto> insertarBulk(List<Producto> productos) {
        return productoRepository.saveAll(productos).stream().collect(Collectors.toList());
    }
}
