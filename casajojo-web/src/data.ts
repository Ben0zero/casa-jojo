import type { Estado, Pedido, Plato, Producto } from "./types";
import { PRECIO_SOLO_PROTEINA } from "./domain";

const T_PROT = "proteina" as const;
const T_ACOM = "acompanamiento" as const;

export const MENU_SEED: Producto[] = [
  { id: 1, nombre: "Pollo asado", precio: 4800, tipo: T_PROT, disponible: true },
  { id: 2, nombre: "Carne al jugo", precio: 4800, tipo: T_PROT, disponible: true },
  { id: 3, nombre: "Guatitas", precio: 4600, tipo: T_PROT, disponible: true },
  { id: 4, nombre: "Cerdo mongoliano", precio: 4600, tipo: T_PROT, disponible: false },
  { id: 5, nombre: "Filete de pollo", precio: 4600, tipo: T_PROT, disponible: true },
  { id: 6, nombre: "Tortilla de porotos verdes", precio: 4600, tipo: T_PROT, disponible: true },
  { id: 7, nombre: "Arroz", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 8, nombre: "Arroz integral", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 9, nombre: "Verduras salteadas", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 10, nombre: "Fideos blancos", precio: 0, tipo: T_ACOM, disponible: false },
  { id: 11, nombre: "Fideos al pesto", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 12, nombre: "Papas mayo", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 13, nombre: "Puré", precio: 0, tipo: T_ACOM, disponible: true },
  { id: 14, nombre: "Ensalada", precio: 0, tipo: T_ACOM, disponible: true },
];

const precioProteina = (nombre: string): number =>
  MENU_SEED.find((p) => p.nombre === nombre)?.precio ?? 4600;

function plato(
  nombreProteina: string,
  acompanamientos: string[],
  solo = false,
): Plato {
  return {
    nombreProteina,
    solo,
    acompanamientos,
    precio: solo ? PRECIO_SOLO_PROTEINA : precioProteina(nombreProteina),
  };
}

const HOY = Date.now();
const hoyA = new Date(HOY);

function hace(min: number): number {
  const d = new Date(hoyA);
  d.setMinutes(d.getMinutes() - min);
  return d.getTime();
}

function pedido(
  id: number,
  numero: number,
  cliente: string,
  origen: Pedido["origen"],
  horaRetiro: string,
  estadoPedido: Pedido["estadoPedido"],
  estadoPago: Pedido["estadoPago"],
  platos: Plato[],
  fecha: number,
  indicaciones = "",
): Pedido {
  return {
    id,
    numero,
    cliente,
    origen,
    horaRetiro,
    formaPago: "Transferencia",
    indicaciones,
    estadoPedido,
    estadoPago,
    total: platos.reduce((s, p) => s + p.precio, 0),
    fecha,
    platos,
  };
}

function demoPedidos(): Pedido[] {
  return [
    pedido(1, 1, "Benjamín Rojas", "app", "13:00", "recibido", "pendiente", [
      plato("Pollo asado", ["Arroz", "Ensalada"]),
    ], hace(6), "Sin ají"),
    pedido(2, 2, "Camila Soto", "whatsapp", "13:00", "recibido", "pendiente", [
      plato("Carne al jugo", ["Puré", "Verduras salteadas"]),
    ], hace(4)),
    pedido(3, 3, "Ignacio Molina", "presencial", "12:00", "preparacion", "pendiente", [
      plato("Guatitas", ["Papas mayo", "Arroz"]),
    ], hace(28)),
    pedido(4, 4, "Francisca Díaz", "app", "12:30", "preparacion", "pagado", [
      plato("Filete de pollo", ["Arroz integral", "Ensalada"]),
      plato("Tortilla de porotos verdes", ["Ensalada"]),
    ], hace(35)),
    pedido(5, 5, "Matías Sanhueza", "llamada", "12:00", "listo", "pendiente", [
      plato("Pollo asado", ["Fideos al pesto"]),
      plato("Carne al jugo", [], true),
    ], hace(52), "Cocido"),
    pedido(6, 6, "Valentina Paredes", "app", "11:45", "entregado", "pagado", [
      plato("Filete de pollo", ["Arroz", "Puré", "Ensalada"]),
    ], hace(70)),
    pedido(7, 7, "Cristóbal Vera", "whatsapp", "14:30", "cancelado", "no_pagado", [
      plato("Pollo asado", ["Ensalada"]),
    ], hace(15), "Retiro cancelado por el cliente"),
  ];
}

export function seedEstado(): Estado {
  return {
    productos: MENU_SEED,
    pedidos: demoPedidos(),
  };
}