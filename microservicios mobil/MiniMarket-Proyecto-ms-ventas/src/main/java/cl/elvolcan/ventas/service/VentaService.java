package cl.elvolcan.ventas.service;

import cl.elvolcan.ventas.assembler.VentaModelAssembler;
import cl.elvolcan.ventas.client.CatalogoClient;
import cl.elvolcan.ventas.client.InventarioClient;
import cl.elvolcan.ventas.dto.DetalleVentaRequestDTO;
import cl.elvolcan.ventas.dto.ProductoDTO;
import cl.elvolcan.ventas.dto.StockRequestDTO;
import cl.elvolcan.ventas.dto.StockResponseDTO;
import cl.elvolcan.ventas.dto.VentaRequestDTO;
import cl.elvolcan.ventas.dto.VentaResponseDTO;
import cl.elvolcan.ventas.exception.InsufficientStockException;
import cl.elvolcan.ventas.exception.ResourceNotFoundException;
import cl.elvolcan.ventas.model.DetalleVenta;
import cl.elvolcan.ventas.model.Venta;
import cl.elvolcan.ventas.repository.VentaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final CatalogoClient catalogoClient;
    private final InventarioClient inventarioClient;
    private final VentaModelAssembler ventaModelAssembler;

    private final String SECRET_STRING = "clave_secreta_super_segura_de_32_caracteres_minimo";
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    private String getToken() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String auth = attr.getRequest().getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                return auth;
            }
        } catch (Exception e) {
        }
        return generateInternalToken();
    }

    private String getInternalToken() {
        return generateInternalToken();
    }

    private String generateInternalToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "ROLE_ADMIN");
        return "Bearer " + Jwts.builder()
                .setClaims(claims)
                .setSubject("ms-ventas")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SECRET_KEY, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    @Transactional
    public List<VentaResponseDTO> listarTodas() {
        return ventaRepository.findAll().stream().map(ventaModelAssembler::toModel).collect(Collectors.toList());
    }

    @Transactional
    public List<VentaResponseDTO> listarMisCompras() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String usuario = auth != null ? auth.getName() : null;
            if (usuario == null || "anonymousUser".equals(usuario)) {
                throw new IllegalArgumentException("Usuario no autenticado");
            }
            return ventaRepository.findByUsuarioOrderByFechaVentaDesc(usuario).stream()
                    .map(ventaModelAssembler::toModel)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
    }

    @Transactional
    public VentaResponseDTO crearPedido(VentaRequestDTO requestDTO) {
        String token = getToken();
        String tipoCliente = requestDTO.getTipoCliente() == null ? "RESIDENCIAL" : requestDTO.getTipoCliente().toUpperCase(Locale.ROOT);
        Venta venta = new Venta();
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                venta.setUsuario(auth.getName());
            }
        } catch (Exception ignored) {
        }
        venta.setEstado(Venta.EstadoVenta.PENDIENTE_PAGO);
        double totalVenta = 0.0;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (DetalleVentaRequestDTO detalleDTO : requestDTO.getDetalles()) {
            if (detalleDTO.getProductoId() == null || detalleDTO.getCantidad() == null) {
                throw new IllegalArgumentException("Cada detalle debe indicar productoId y cantidad");
            }
            ProductoDTO producto = catalogoClient.obtenerProductoPorId(token, detalleDTO.getProductoId());
            if (producto == null) throw new ResourceNotFoundException("Producto no encontrado");
            StockResponseDTO stock = inventarioClient.obtenerStock(token, producto.getSku());
            if (stock == null || stock.getStockActual() < detalleDTO.getCantidad()) {
                throw new InsufficientStockException("Stock insuficiente para: " + producto.getNombre());
            }

            Integer precio = "COMERCIAL".equals(tipoCliente) && producto.getPrecioComercial() != null
                    ? producto.getPrecioComercial()
                    : producto.getPrecioResidencial() != null ? producto.getPrecioResidencial() : producto.getPrecio();

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProductoId(producto.getId());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(precio.doubleValue());
            detalle.setSubtotal(precio.doubleValue() * detalleDTO.getCantidad());
            detalles.add(detalle);
            totalVenta += detalle.getSubtotal();
        }

        venta.setTotal(totalVenta);
        venta.setDetalles(detalles);
        return ventaModelAssembler.toModel(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponseDTO confirmarPago(Long ventaId) {
        String token = getToken();
        String internalToken = getInternalToken();
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + ventaId));
        if (venta.getEstado() == Venta.EstadoVenta.PAGADA) return ventaModelAssembler.toModel(venta);

        for (DetalleVenta detalle : venta.getDetalles()) {
            ProductoDTO producto = catalogoClient.obtenerProductoPorId(token, detalle.getProductoId());
            StockRequestDTO request = new StockRequestDTO();
            request.setSku(producto.getSku());
            request.setStockActual(detalle.getCantidad().doubleValue());
            inventarioClient.disminuirStock(internalToken, request);
        }
        venta.setEstado(Venta.EstadoVenta.PAGADA);
        return ventaModelAssembler.toModel(ventaRepository.save(venta));
    }
}
