import { useStore } from "../store";
import { calcularResumen, dinero, TXT_ESTADO } from "../domain";
import { BadgeEstado, BadgePago, BadgeOrigen } from "../components";

export function Resumen() {
  const { estado, resetDemo } = useStore();
  const res = calcularResumen(estado.pedidos);

  const ultimos = estado.pedidos.slice().sort((a, b) => b.fecha - a.fecha);

  return (
    <section>
      <div className="pagina-cab">
        <div>
          <h1>Resumen del día</h1>
          <div className="sub">Ventas y estado de pagos (RF-15)</div>
        </div>
        <button
          className="btn ghost"
          onClick={() => {
            if (window.confirm("¿Reiniciar los datos demo del navegador?")) resetDemo();
          }}
        >
          Reiniciar demo
        </button>
      </div>

      <div className="stats">
        <div className="stat">
          <div className="valor">{res.totalPedidos}</div>
          <div className="etiqueta">Pedidos (no cancelados)</div>
        </div>
        <div className="stat">
          <div className="valor ac">{dinero(res.totalVendido)}</div>
          <div className="etiqueta">Total vendido</div>
        </div>
        <div className="stat">
          <div className="valor">{res.platosVendidos}</div>
          <div className="etiqueta">Platos vendidos</div>
        </div>
        <div className="stat">
          <div className="valor">{dinero(res.ticketPromedio)}</div>
          <div className="etiqueta">Ticket promedio</div>
        </div>
      </div>

      <div className="barra-pagos">
        <div className="caja">
          <div className="valor" style={{ color: "var(--ok)" }}>{res.conteoPago.pagado}</div>
          <div className="etiqueta">Pagos realizado</div>
        </div>
        <div className="caja">
          <div className="valor" style={{ color: "var(--amber)" }}>{res.conteoPago.pendiente}</div>
          <div className="etiqueta">Pagos pendientes</div>
        </div>
        <div className="caja">
          <div className="valor" style={{ color: "var(--bad)" }}>{res.conteoPago.no_pagado}</div>
          <div className="etiqueta">No pagado</div>
        </div>
        <div className="caja">
          <div className="valor">{res.conteoEstado.cancelado}</div>
          <div className="etiqueta">Cancelado</div>
        </div>
      </div>

      <div className="caja">
        <div className="seccion-t" style={{ marginBottom: 8 }}>Estado de los pedidos</div>
        {(Object.keys(TXT_ESTADO) as (keyof typeof TXT_ESTADO)[]).map((k) => (
          <span key={k} style={{ marginRight: 8 }}>
            <BadgeEstado e={k} /> <b>{res.conteoEstado[k]}</b>
          </span>
        ))}
      </div>

      <div className="caja">
        <div className="seccion-t" style={{ marginBottom: 8 }}>Historial (RF-14)</div>
        <table className="tabla">
          <thead>
            <tr>
              <th>N.º</th>
              <th>Cliente</th>
              <th>Origen</th>
              <th>Retiro</th>
              <th>Platos</th>
              <th>Total</th>
              <th>Estado</th>
              <th>Pago</th>
            </tr>
          </thead>
          <tbody>
            {ultimos.map((p) => (
              <tr key={p.id}>
                <td>
                  <b>#{p.numero}</b>
                </td>
                <td>{p.cliente}</td>
                <td>
                  <BadgeOrigen o={p.origen} />
                </td>
                <td>{p.horaRetiro} h</td>
                <td>
                  {p.platos.map((pl, i) => (
                    <div key={i}>
                      {pl.nombreProteina}
                      {pl.solo ? " (solo)" : ""}
                    </div>
                  ))}
                </td>
                <td>{dinero(p.total)}</td>
                <td>
                  <BadgeEstado e={p.estadoPedido} />
                </td>
                <td>
                  <BadgePago p={p.estadoPago} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}