import { useMemo, useState } from "react";
import { useStore } from "../store";
import type { EstadoPago, EstadoPedido } from "../types";
import { dinero, siguienteEstado, TXT_PAGO } from "../domain";
import { BadgeEstado, BadgeOrigen, PlatoTexto } from "../components";
import { ModalRegistrar } from "./RegistrarPedido";

const FILTROS: { valor: EstadoPedido | "all"; texto: string }[] = [
  { valor: "all", texto: "Todos" },
  { valor: "recibido", texto: "Recibidos" },
  { valor: "preparacion", texto: "En preparación" },
  { valor: "listo", texto: "Listos" },
  { valor: "entregado", texto: "Entregados" },
  { valor: "cancelado", texto: "Cancelados" },
];

export function Comandas() {
  const { estado, cambiarEstado, cambiarPago } = useStore();
  const [busq, setBusq] = useState("");
  const [filtro, setFiltro] = useState<EstadoPedido | "all">("all");
  const [abierto, setAbierto] = useState(false);

  const pedidos = useMemo(() => {
    const q = busq.trim().toLowerCase();
    return estado.pedidos
      .filter((p) => filtro === "all" || p.estadoPedido === filtro)
      .filter(
        (p) =>
          q === "" ||
          String(p.numero).includes(q) ||
          p.cliente.toLowerCase().includes(q) ||
          p.platos.some((pl) => pl.nombreProteina.toLowerCase().includes(q)),
      )
      .slice()
      .sort((a, b) => a.horaRetiro.localeCompare(b.horaRetiro) || a.numero - b.numero);
  }, [estado.pedidos, busq, filtro]);

  return (
    <section>
      <div className="pagina-cab">
        <div>
          <h1>Comandas</h1>
          <div className="sub">Gestión de pedidos · Personal de atención</div>
        </div>
        <div className="barra-herramientas">
          <input
            className="input buscar"
            placeholder="Buscar n.º, cliente o plato…"
            value={busq}
            onChange={(e) => setBusq(e.target.value)}
          />
          <select
            className="input"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value as EstadoPedido | "all")}
          >
            {FILTROS.map((f) => (
              <option key={f.valor} value={f.valor}>
                {f.texto}
              </option>
            ))}
          </select>
          <button className="btn primary" onClick={() => setAbierto(true)}>
            Registrar pedido
          </button>
        </div>
      </div>

      <div className="aviso-demo">
        Demo local: los pedidos se guardan en este navegador. La app móvil aún no sincroniza datos.
      </div>

      {pedidos.length === 0 && <div className="estado-vacio">No hay pedidos que coincidan.</div>}

      {pedidos.map((p) => {
        const sig = siguienteEstado(p.estadoPedido);
        return (
          <div className="caja" key={p.id}>
            <div className="caja-cab">
              <span className="numero">#{p.numero}</span>
              <BadgeEstado e={p.estadoPedido} />
              <BadgeOrigen o={p.origen} />
              <span className="espacio" />
              <span className="hora">Retiro {p.horaRetiro} h</span>
              <span className="tot">{dinero(p.total)}</span>
            </div>
            <div>
              <b>Cliente:</b> {p.cliente} · {TXT_PAGO[p.estadoPago]} · {p.formaPago}
            </div>
            <div className="platos">
              {p.platos.map((pl, i) => (
                <div className="plato" key={i}>
                  {i + 1}. <PlatoTexto plato={pl} />
                </div>
              ))}
            </div>
            {p.indicaciones && <div className="indicaciones">Indicaciones: {p.indicaciones}</div>}

            <div className="botones-caja">
              <span style={{ alignSelf: "center", color: "var(--mut)", fontSize: 13 }}>Pago:</span>
              {(Object.keys(TXT_PAGO) as EstadoPago[]).map((pg) => (
                <button
                  key={pg}
                  className={`ch ${p.estadoPago === pg ? "act" : ""}`}
                  onClick={() => cambiarPago(p.numero, pg)}
                >
                  {TXT_PAGO[pg]}
                </button>
              ))}
              {sig &&
                (p.estadoPedido === "recibido" ? (
                  <button
                    className="btn primary small"
                    onClick={() => cambiarEstado(p.numero, sig)}
                  >
                    Iniciar preparación
                  </button>
                ) : p.estadoPedido === "preparacion" ? (
                  <button
                    className="btn primary small"
                    onClick={() => cambiarEstado(p.numero, sig)}
                  >
                    Marcar listo
                  </button>
                ) : (
                  <button
                    className="btn primary small"
                    onClick={() => cambiarEstado(p.numero, sig)}
                  >
                    Entregar
                  </button>
                ))}
              {p.estadoPedido === "recibido" && (
                <button
                  className="btn danger-ghost small"
                  onClick={() => {
                    if (window.confirm(`¿Cancelar el pedido #${p.numero}?`)) {
                      cambiarEstado(p.numero, "cancelado");
                    }
                  }}
                >
                  Cancelar
                </button>
              )}
            </div>
          </div>
        );
      })}

      {abierto && <ModalRegistrar onClose={() => setAbierto(false)} />}
    </section>
  );
}