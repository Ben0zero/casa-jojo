import { describe, expect, it } from "vitest";
import type { Pedido, Plato } from "./types";
import {
  calcularResumen,
  dinero,
  minutosParaRetiro,
  retiroUrgente,
  siguienteEstado,
  totalPlato,
} from "./domain";

function plato(p: Plato) {
  return p;
}

function pedido(estadoPedido: Pedido["estadoPedido"], estadoPago: Pedido["estadoPago"], total: number): Pedido {
  return {
    id: 1,
    numero: 1,
    cliente: "Demo",
    origen: "app",
    horaRetiro: "13:00",
    formaPago: "Transferencia",
    indicaciones: "",
    estadoPedido,
    estadoPago,
    total,
    fecha: Date.now(),
    platos: [plato({ nombreProteina: "Pollo asado", precio: total, solo: false, acompanamientos: ["Arroz"] })],
  };
}

describe("siguienteEstado", () => {
  it("recorre la secuencia de estados", () => {
    expect(siguienteEstado("recibido")).toBe("preparacion");
    expect(siguienteEstado("preparacion")).toBe("listo");
    expect(siguienteEstado("listo")).toBe("entregado");
    expect(siguienteEstado("entregado")).toBeNull();
    expect(siguienteEstado("cancelado")).toBeNull();
  });
});

describe("totalPlato", () => {
  it("aplica $3.000 a la proteína sola", () => {
    expect(totalPlato(4800, true)).toBe(3000);
    expect(totalPlato(4800, false)).toBe(4800);
  });
});

describe("dinero", () => {
  it("formatea con separador de miles", () => {
    expect(dinero(4800)).toBe("$4.800");
    expect(dinero(0)).toBe("$0");
  });
});

describe("minutosParaRetiro", () => {
  it("calcula minutos hasta la hora de retiro del mismo día", () => {
    const ahora = new Date(2026, 9, 8, 12, 0, 0);
    expect(minutosParaRetiro("13:00", ahora)).toBe(60);
    expect(minutosParaRetiro("12:10", ahora)).toBe(10);
  });

  it("pasa al día siguiente si la hora ya pasó", () => {
    const ahora = new Date(2026, 9, 8, 13, 30, 0);
    expect(minutosParaRetiro("13:00", ahora)).toBe(1410);
  });

  it("marca urgente cuando el retiro es en menos de 15 minutos", () => {
    const ahora = new Date(2026, 9, 8, 12, 50, 0);
    expect(retiroUrgente("13:00", ahora)).toBe(true);
    expect(retiroUrgente("13:20", ahora)).toBe(false);
  });
});

describe("calcularResumen", () => {
  it("suma total vendido sin incluir cancelados", () => {
    const pedidos = [
      pedido("recibido", "pendiente", 4800),
      pedido("entregado", "pagado", 7800),
      pedido("cancelado", "no_pagado", 4600),
    ];
    const r = calcularResumen(pedidos);
    expect(r.totalPedidos).toBe(2);
    expect(r.totalVendido).toBe(12600);
    expect(r.platosVendidos).toBe(2);
    expect(r.ticketPromedio).toBe(6300);
    expect(r.conteoPago.pendiente).toBe(1);
    expect(r.conteoPago.pagado).toBe(1);
    expect(r.conteoPago.no_pagado).toBe(1);
    expect(r.conteoEstado.cancelado).toBe(1);
  });
});