package cl.elvolcan.auth.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import cl.elvolcan.auth.controller.UsuarioControllerV2;
import cl.elvolcan.auth.model.Usuario;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UsuarioModelAssembler implements RepresentationModelAssembler<Usuario, EntityModel<Usuario>> {

    @Override
    public EntityModel<Usuario> toModel(Usuario usuario) {
        return EntityModel.of(usuario,
            linkTo(methodOn(UsuarioControllerV2.class).listarUsuarios()).withRel("usuarios"),
            linkTo(methodOn(UsuarioControllerV2.class).actualizarUsuario(usuario.getId(), null)).withRel("actualizar"),
            linkTo(methodOn(UsuarioControllerV2.class).eliminarUsuario(usuario.getId())).withRel("eliminar")
        );
    }
}
