package minimarket.produccion.service;

import java.util.List;

import minimarket.produccion.dto.request.ProduccionRequestDTO;
import minimarket.produccion.dto.response.ProduccionResponseDTO;

public interface ProduccionService {

    ProduccionResponseDTO crearLote(ProduccionRequestDTO dto);

    List<ProduccionResponseDTO> obtenerTodos();

    ProduccionResponseDTO buscarPorId(Long id);

    void eliminar(Long id);
}