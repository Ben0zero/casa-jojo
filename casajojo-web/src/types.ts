export type EstadoPedido = "recibido" | "preparacion" | "listo" | "entregado" | "cancelado";
export type EstadoPago = "pagado" | "pendiente" | "no_pagado";
export type Origen = "app" | "whatsapp" | "llamada" | "presencial";

export interface Plato {
  nombreProteina: string;
  precio: number;
  solo: boolean;
  acompanamientos: string[];
}

export interface Pedido {
  id: number;
  numero: number;
  cliente: string;
  origen: Origen;
  horaRetiro: string;
  formaPago: string;
  indicaciones: string;
  estadoPedido: EstadoPedido;
  estadoPago: EstadoPago;
  total: number;
  fecha: number;
  platos: Plato[];
}

export type TipoProducto = "proteina" | "acompanamiento";

export interface Producto {
  id: number;
  nombre: string;
  precio: number;
  tipo: TipoProducto;
  disponible: boolean;
}

export interface Estado {
  productos: Producto[];
  pedidos: Pedido[];
}

export interface PlatosNuevo {
  idProteina: number;
  solo: boolean;
  acompanamientos: string[];
}