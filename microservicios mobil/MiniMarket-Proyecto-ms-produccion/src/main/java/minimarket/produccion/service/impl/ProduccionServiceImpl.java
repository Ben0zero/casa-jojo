package minimarket.produccion.service.impl;

import minimarket.produccion.dto.request.ProduccionRequestDTO;
import minimarket.produccion.dto.response.ProduccionResponseDTO;
import minimarket.produccion.exception.ResourceNotFoundException;
import minimarket.produccion.model.LoteProduccion;
import minimarket.produccion.repository.ProduccionRepository;
import minimarket.produccion.service.ProduccionService;
import minimarket.produccion.client.InventarioClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProduccionServiceImpl implements ProduccionService {

    @Autowired
    private ProduccionRepository repository;

    @Autowired
    private InventarioClient inventarioClient;
    // Crear lote
    @Override
    public ProduccionResponseDTO crearLote(ProduccionRequestDTO dto) {

        LoteProduccion lote = new LoteProduccion();

        lote.setProductoSku(dto.getProductoSku());
        lote.setCantidad(dto.getCantidad());

        repository.save(lote);
        inventarioClient.sumarStock(
        lote.getProductoSku(),
        lote.getCantidad()
        );

        return mapToResponse(lote);
    }

    // Obtener todos
    @Override
    public List<ProduccionResponseDTO> obtenerTodos() {

        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Buscar por ID
    @Override
    public ProduccionResponseDTO buscarPorId(Long id) {

        LoteProduccion lote = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));

        return mapToResponse(lote);
    }

    // Eliminar lote
    @Override
    public void eliminar(Long id) {

        repository.deleteById(id);
    }

    // Convertir Entity → DTO
    private ProduccionResponseDTO mapToResponse(LoteProduccion lote) {

        ProduccionResponseDTO dto = new ProduccionResponseDTO();

        dto.setId(lote.getId());
        dto.setProductoSku(lote.getProductoSku());
        dto.setCantidad(lote.getCantidad());
        dto.setFecha(lote.getFecha());

        return dto;
    }
}
