import type { EstadoPago, EstadoPedido, Origen, Pedido } from "./types";

export const PRECIO_SOLO_PROTEINA = 3000;

export const ORDEN_ESTADOS: EstadoPedido[] = [
  "recibido",
  "preparacion",
  "listo",
  "entregado",
];

export const TXT_ESTADO: Record<EstadoPedido, string> = {
  recibido: "Recibido",
  preparacion: "En preparación",
  listo: "Listo",
  entregado: "Entregado",
  cancelado: "Cancelado",
};

export const TXT_PAGO: Record<EstadoPago, string> = {
  pagado: "Pagado",
  pendiente: "Pendiente",
  no_pagado: "No pagado",
};

export const TXT_ORIGEN: Record<Origen, string> = {
  app: "App",
  whatsapp: "WhatsApp",
  llamada: "Llamada",
  presencial: "Presencial",
};

export const CLS_ESTADO: Record<EstadoPedido, string> = {
  recibido: "st-neutro",
  preparacion: "st-preparacion",
  listo: "st-listo",
  entregado: "st-entregado",
  cancelado: "st-cancelado",
};

export const CLS_PAGO: Record<EstadoPago, string> = {
  pagado: "st-entregado",
  pendiente: "st-preparacion",
  no_pagado: "st-cancelado",
};

export function siguienteEstado(e: EstadoPedido): EstadoPedido | null {
  const i = ORDEN_ESTADOS.indexOf(e);
  if (i < 0 || i === ORDEN_ESTADOS.length - 1) return null;
  return ORDEN_ESTADOS[i + 1];
}

export function dinero(n: number): string {
  return "$" + n.toLocaleString("es-CL");
}

export function totalPlato(precioProteina: number, solo: boolean): number {
  return solo ? PRECIO_SOLO_PROTEINA : precioProteina;
}

export interface Resumen {
  totalPedidos: number;
  platosVendidos: number;
  totalVendido: number;
  ticketPromedio: number;
  conteoEstado: Record<EstadoPedido, number>;
  conteoPago: Record<EstadoPago, number>;
}

export function calcularResumen(pedidos: Pedido[]): Resumen {
  const vendidos = pedidos.filter((p) => p.estadoPedido !== "cancelado");
  const conteoEstado: Record<EstadoPedido, number> = {
    recibido: 0,
    preparacion: 0,
    listo: 0,
    entregado: 0,
    cancelado: 0,
  };
  const conteoPago: Record<EstadoPago, number> = {
    pagado: 0,
    pendiente: 0,
    no_pagado: 0,
  };
  pedidos.forEach((p) => {
    conteoEstado[p.estadoPedido] += 1;
    conteoPago[p.estadoPago] += 1;
  });
  const totalVendido = vendidos.reduce((s, p) => s + p.total, 0);
  const platosVendidos = vendidos.reduce((s, p) => s + p.platos.length, 0);
  return {
    totalPedidos: vendidos.length,
    platosVendidos,
    totalVendido,
    ticketPromedio: vendidos.length ? Math.round(totalVendido / vendidos.length) : 0,
    conteoEstado,
    conteoPago,
  };
}

export function minutosParaRetiro(hora: string, ahora: Date = new Date()): number {
  const [h, m] = hora.split(":").map(Number);
  const t = new Date(ahora);
  t.setHours(h, m, 0, 0);
  let diff = (t.getTime() - ahora.getTime()) / 60000;
  if (diff < 0) diff += 1440;
  return Math.round(diff);
}

export function retiroUrgente(hora: string, ahora: Date = new Date()): boolean {
  const r = minutosParaRetiro(hora, ahora);
  return r >= 0 && r <= 15;
}