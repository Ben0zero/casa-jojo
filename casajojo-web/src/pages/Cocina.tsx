import { useStore } from "../store";
import { dinero, minutosParaRetiro, retiroUrgente, TXT_ORIGEN } from "../domain";
import { BadgeEstado, BadgePago, PlatoTexto } from "../components";

export function Cocina() {
  const { estado, cambiarEstado } = useStore();

  const activos = estado.pedidos
    .filter((p) => p.estadoPedido === "recibido" || p.estadoPedido === "preparacion")
    .slice()
    .sort((a, b) => a.horaRetiro.localeCompare(b.horaRetiro) || a.numero - b.numero);

  const urgentes = activos.filter((p) => retiroUrgente(p.horaRetiro)).length;

  return (
    <section>
      <div className="pagina-cab">
        <div>
          <h1>Cocina</h1>
          <div className="sub">Comandas ordenadas por hora de retiro (RF-10)</div>
        </div>
        <div className="barra-herramientas">
          <span className="pch st-neutro">En fila: {activos.length}</span>
          {urgentes > 0 && <span className="pch st-cancelado">Retiran pronto: {urgentes}</span>}
        </div>
      </div>

      {activos.length === 0 && (
        <div className="estado-vacio">No hay comandas por preparar.</div>
      )}

      <div className="grid-cocina">
        {activos.map((p) => {
          const urgente = retiroUrgente(p.horaRetiro);
          const min = minutosParaRetiro(p.horaRetiro);
          return (
            <div className={`caja ${urgente ? "urgente" : ""}`} key={p.id}>
              <div className="caja-cab">
                <span className="numero">#{p.numero}</span>
                <span className="hora">{p.horaRetiro} h</span>
                <span className="espacio" />
                <BadgeEstado e={p.estadoPedido} />
                <BadgePago p={p.estadoPago} />
              </div>

              {urgente ? (
                <div className="urg">
                  <span className="urg-dot" />
                  Retira pronto (≤15 min)
                </div>
              ) : (
                <div className="sub">Retira en {min} min</div>
              )}

              <div className="sub">
                {p.cliente} · {TXT_ORIGEN[p.origen]} · {dinero(p.total)}
              </div>

              <div className="platos" style={{ marginTop: 6 }}>
                {p.platos.map((pl, i) => (
                  <div className="plato" key={i}>
                    <PlatoTexto plato={pl} />
                  </div>
                ))}
              </div>

              {p.indicaciones && (
                <div className="indicaciones">Indicaciones: {p.indicaciones}</div>
              )}

              <div className="botones-caja">
                {p.estadoPedido === "recibido" && (
                  <button
                    className="btn primary small"
                    onClick={() => cambiarEstado(p.numero, "preparacion")}
                  >
                    Iniciar preparación
                  </button>
                )}
                {p.estadoPedido === "preparacion" && (
                  <button
                    className="btn primary small"
                    onClick={() => cambiarEstado(p.numero, "listo")}
                  >
                    Marcar listo
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}