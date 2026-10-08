import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type {
  Estado,
  EstadoPago,
  EstadoPedido,
  Origen,
  PlatosNuevo,
  Producto,
} from "./types";
import { seedEstado } from "./data";
import { totalPlato } from "./domain";

const KEY = "casa-jojo-web-v1";

interface NuevoPedido {
  cliente: string;
  origen: Origen;
  horaRetiro: string;
  formaPago: string;
  indicaciones: string;
  platos: PlatosNuevo[];
}

interface Store {
  estado: Estado;
  toggleProducto: (id: number) => void;
  guardarProducto: (p: Producto) => void;
  cambiarEstado: (numero: number, estado: EstadoPedido) => void;
  cambiarPago: (numero: number, pago: EstadoPago) => void;
  registrarPedido: (nuevo: NuevoPedido) => void;
  resetDemo: () => void;
}

const StoreContext = createContext<Store | null>(null);

function cargar(): Estado {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return JSON.parse(raw) as Estado;
  } catch {
    /* seed en el caso de datos corruptos */
  }
  return seedEstado();
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<Estado>(cargar);

  useEffect(() => {
    try {
      localStorage.setItem(KEY, JSON.stringify(estado));
    } catch {
      /* almacenamiento no disponible */
    }
  }, [estado]);

  function mutar(fn: (e: Estado) => Estado) {
    setEstado((e) => fn(e));
  }

  const valor: Store = {
    estado,

    toggleProducto(id) {
      mutar((e) => ({
        ...e,
        productos: e.productos.map((p) =>
          p.id === id ? { ...p, disponible: !p.disponible } : p,
        ),
      }));
    },

    guardarProducto(p) {
      mutar((e) => {
        const existe = e.productos.some((x) => x.id === p.id);
        return {
          ...e,
          productos: existe
            ? e.productos.map((x) => (x.id === p.id ? p : x))
            : [...e.productos, p],
        };
      });
    },

    cambiarEstado(numero, estadoPedido) {
      mutar((e) => ({
        ...e,
        pedidos: e.pedidos.map((ped) =>
          ped.numero === numero ? { ...ped, estadoPedido } : ped,
        ),
      }));
    },

    cambiarPago(numero, estadoPago) {
      mutar((e) => ({
        ...e,
        pedidos: e.pedidos.map((ped) =>
          ped.numero === numero ? { ...ped, estadoPago } : ped,
        ),
      }));
    },

    registrarPedido(nuevo) {
      mutar((e) => {
        const max = e.pedidos.reduce((m, p) => Math.max(m, p.numero), 0);
        const platos = nuevo.platos.map((it) => {
          const prot = e.productos.find((x) => x.id === it.idProteina);
          return {
            nombreProteina: prot?.nombre ?? "Menú",
            solo: it.solo,
            acompanamientos: it.acompanamientos,
            precio: totalPlato(prot?.precio ?? 4600, it.solo),
          };
        });
        const pedido = {
          id: Date.now(),
          numero: max + 1,
          cliente: nuevo.cliente.trim(),
          origen: nuevo.origen,
          horaRetiro: nuevo.horaRetiro,
          formaPago: nuevo.formaPago,
          indicaciones: nuevo.indicaciones.trim(),
          estadoPedido: "recibido" as EstadoPedido,
          estadoPago: "pendiente" as EstadoPago,
          total: platos.reduce((s, p) => s + p.precio, 0),
          fecha: Date.now(),
          platos,
        };
        return { ...e, pedidos: [...e.pedidos, pedido] };
      });
    },

    resetDemo() {
      setEstado(seedEstado());
    },
  };

  return <StoreContext.Provider value={valor}>{children}</StoreContext.Provider>;
}

export function useStore(): Store {
  const s = useContext(StoreContext);
  if (!s) throw new Error("useStore debe usarse dentro de StoreProvider");
  return s;
}