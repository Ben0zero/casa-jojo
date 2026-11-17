package cl.elvolcan.inventario.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import cl.elvolcan.inventario.controller.InventarioControllerV2;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class InventarioModelAssembler implements RepresentationModelAssembler<StockResponseDTO, EntityModel<StockResponseDTO>> {

    @Override
    public EntityModel<StockResponseDTO> toModel(StockResponseDTO dto) {
        return EntityModel.of(dto,
            // Link a sí mismo (detalle por SKU)
            linkTo(methodOn(InventarioControllerV2.class).obtenerPorSku(dto.getSku())).withSelfRel(),
            // Link al catálogo completo de inventario
            linkTo(methodOn(InventarioControllerV2.class).listarTodo()).withRel("inventario-completo"),
            // Link directo a la consulta de stock crítico
            linkTo(methodOn(InventarioControllerV2.class).obtenerStockCritico()).withRel("ver-stock-critico")
        );
    }
}