package minimarket.produccion.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import minimarket.produccion.controller.ProduccionControllerV2;
import minimarket.produccion.dto.response.ProduccionResponseDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ProduccionModelAssembler implements RepresentationModelAssembler<ProduccionResponseDTO, EntityModel<ProduccionResponseDTO>> {

    @Override
    public EntityModel<ProduccionResponseDTO> toModel(ProduccionResponseDTO dto) {
        return EntityModel.of(dto,
            // Link para ver el detalle de este lote en específico por su ID
            linkTo(methodOn(ProduccionControllerV2.class).buscarPorId(dto.getId())).withSelfRel(),
            // Link para volver a ver la lista completa de todos los lotes de producción
            linkTo(methodOn(ProduccionControllerV2.class).listar()).withRel("produccion-completa")
        );
    }
}