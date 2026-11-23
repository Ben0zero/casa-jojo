package cl.casajojo.auth.controller;

import cl.casajojo.auth.dto.RegistroRequestDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.exception.CorreoDuplicadoException;
import cl.casajojo.auth.exception.CredencialesInvalidasException;
import cl.casajojo.auth.exception.GlobalExceptionHandler;
import cl.casajojo.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Códigos HTTP del login y registro (M5, M6, A2).
 * MockMvc "standalone": no levanta el contexto de Spring ni necesita Oracle.
 */
class AuthControllerTest {

    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, mock(cl.casajojo.auth.service.RecuperacionService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String registro(String correo, String telefono) {
        return """
                {"nombre":"Camila","correo":%s,"telefono":%s,"password":"Clave123"}
                """.formatted(correo == null ? "null" : "\"" + correo + "\"",
                telefono == null ? "null" : "\"" + telefono + "\"");
    }

    @Test
    @DisplayName("M6: registro con correo inválido responde 400")
    void registroCorreoInvalido400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("hola", "912345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El correo no tiene un formato válido"));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("M6: registro sin correo responde 400")
    void registroSinCorreo400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro(null, "912345678")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("M6: registro con teléfono inválido responde 400")
    void registroTelefonoInvalido400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("camila@casajojo.cl", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El teléfono debe tener 9 dígitos y empezar con 9"));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("A2: un JSON con nombreRol=ADMIN se acepta pero el campo se ignora")
    void registroConNombreRolSeIgnora() throws Exception {
        UsuarioResponseDTO creado = new UsuarioResponseDTO();
        creado.setCorreo("camila@casajojo.cl");
        creado.setNombreRol("CLIENTE");
        when(authService.registrar(any())).thenReturn(creado);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Camila","correo":"camila@casajojo.cl","telefono":"912345678",
                                 "password":"Clave123","nombreRol":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreRol").value("CLIENTE"));

        ArgumentCaptor<RegistroRequestDTO> enviado = ArgumentCaptor.forClass(RegistroRequestDTO.class);
        verify(authService).registrar(enviado.capture());
        assertEquals("camila@casajojo.cl", enviado.getValue().getCorreo());
    }

    @Test
    @DisplayName("M5: correo duplicado responde 409")
    void registroDuplicado409() throws Exception {
        when(authService.registrar(any()))
                .thenThrow(new CorreoDuplicadoException("Error: El nombre de usuario ya está en uso"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("camila@casajojo.cl", "912345678")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("M5: credenciales incorrectas responden 401")
    void loginIncorrecto401() throws Exception {
        when(authService.login(any())).thenThrow(new CredencialesInvalidasException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"a@a.cl\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
    }

    @Test
    @DisplayName("M5: un error inesperado responde 500 sin exponer el mensaje interno")
    void errorInesperado500() throws Exception {
        when(authService.login(any())).thenThrow(new RuntimeException("ORA-12345 detalle interno"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"a@a.cl\",\"password\":\"x\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado en el servicio de autenticación."));
    }

    @Test
    @DisplayName("JSON mal formado responde 400")
    void jsonMalFormado400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest());
    }
}
