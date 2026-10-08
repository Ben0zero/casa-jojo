import { useMemo, useState } from "react";
import { useStore } from "../store";
import type { Origen, PlatosNuevo, Producto } from "../types";
import { dinero, PRECIO_SOLO_PROTEINA } from "../domain";

const HORAS = ["12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00"];

const ORIGENES: { valor: Origen; texto: string }[] = [
  { valor: "whatsapp", texto: "WhatsApp" },
  { valor: "llamada", texto: "Llamada" },
  { valor: "presencial", texto: "Presencial" },
];

export function ModalRegistrar({ onClose }: { onClose: () => void }) {
  const { estado, registrarPedido } = useStore();

  const [cliente, setCliente] = useState("");
  const [origen, setOrigen] = useState<Origen>("whatsapp");
  const [hora, setHora] = useState("13:00");
  const [formaPago, setFormaPago] = useState("Transferencia");
  const [indicaciones, setIndicaciones] = useState("");

  const [platos, setPlatos] = useState<PlatosNuevo[]>([]);
  const [idProt, setIdProt] = useState<number | null>(null);
  const [solo, setSolo] = useState(false);
  const [acoms, setAcoms] = useState<string[]>([]);
  const [error, setError] = useState("");

  const proteinas = estado.productos.filter((p) => p.tipo === "proteina");
  const acomp = estado.productos.filter((p) => p.tipo === "acompanamiento" && p.disponible);
  const protSel = proteinas.find((x) => x.id === idProt);

  const total = useMemo(
    () =>
      platos.reduce((s, d) => {
        const pp = proteinas.find((x) => x.id === d.idProteina)?.precio ?? 4600;
        return s + (d.solo ? PRECIO_SOLO_PROTEINA : pp);
      }, 0),
    [platos, proteinas],
  );

  function toggleProt(p: Producto) {
    if (!p.disponible) return;
    setIdProt(p.id);
    setSolo(false);
    setAcoms([]);
    setError("");
  }

  function toggleAcom(nombre: string) {
    setAcoms((a) => (a.includes(nombre) ? a.filter((x) => x !== nombre) : [...a, nombre]));
  }

  function agregarPlato() {
    if (idProt === null) {
      setError("Elige una proteína.");
      return;
    }
    if (!solo && acoms.length === 0) {
      setError("Elige al menos un acompañamiento (o marca solo la proteína).");
      return;
    }
    setPlatos((p) => [...p, { idProteina: idProt, solo, acompanamientos: acoms.slice().sort() }]);
    setIdProt(null);
    setSolo(false);
    setAcoms([]);
    setError("");
  }

  function quitar(i: number) {
    setPlatos((p) => p.filter((_, idx) => idx !== i));
  }

  function guardar() {
    if (cliente.trim() === "") {
      setError("Escribe el nombre del cliente.");
      return;
    }
    if (platos.length === 0) {
      setError("Agrega al menos un plato.");
      return;
    }
    registrarPedido({
      cliente,
      origen,
      horaRetiro: hora,
      formaPago,
      indicaciones,
      platos,
    });
    onClose();
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Registrar pedido</h2>

        <div className="campos">
          <label>
            Cliente
            <input
              className="input"
              placeholder="Nombre de quien retira"
              value={cliente}
              onChange={(e) => setCliente(e.target.value)}
            />
          </label>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <label>
              Origen (RF-18)
              <select className="input" value={origen} onChange={(e) => setOrigen(e.target.value as Origen)}>
                {ORIGENES.map((o) => (
                  <option key={o.valor} value={o.valor}>
                    {o.texto}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Forma de pago
              <select className="input" value={formaPago} onChange={(e) => setFormaPago(e.target.value)}>
                <option>Transferencia</option>
                <option>Pagar al retirar</option>
              </select>
            </label>
          </div>

          <label>
            Hora de retiro
            <div className="chip-line">
              {HORAS.map((h) => (
                <button key={h} className={`ch ${hora === h ? "act" : ""}`} onClick={() => setHora(h)}>
                  {h}
                </button>
              ))}
            </div>
          </label>

          <label>
            Indicaciones <span className="ayuda">(RF-21, opcional)</span>
            <textarea
              className="input"
              rows={2}
              value={indicaciones}
              onChange={(e) => setIndicaciones(e.target.value)}
            />
          </label>
        </div>

        <div className="secciones">
          <div className="seccion-t">Proteína</div>
          <div className="fila-opciones">
            {proteinas.map((p) => (
              <button
                key={p.id}
                className={`opcion ${idProt === p.id ? "marcada" : ""} ${!p.disponible ? "agotada" : ""}`}
                onClick={() => toggleProt(p)}
              >
                <span className="fila-radio">
                  <span>{idProt === p.id ? "●" : "○"}</span> {p.nombre}
                </span>
                <span className="fila">{p.disponible ? dinero(p.precio) : "Agotado"}</span>
              </button>
            ))}
          </div>

          {protSel && (
            <button
              className={`opcion ${solo ? "marcada" : ""}`}
              onClick={() => {
                setSolo((v) => !v);
                if (!solo) setAcoms([]);
              }}
            >
              <span className="fila-radio">
                <span>{solo ? "☑" : "☐"}</span> Solo la proteína, sin acompañamientos
              </span>
              <span className="fila">{dinero(PRECIO_SOLO_PROTEINA)}</span>
            </button>
          )}

          {protSel && !solo && (
            <>
              <div className="seccion-t" style={{ marginTop: 8 }}>
                Acompañamientos
              </div>
              <div className="fila-opciones">
                {acomp.map((a) => (
                  <button
                    key={a.id}
                    className={`opcion ${acoms.includes(a.nombre) ? "marcada" : ""}`}
                    onClick={() => toggleAcom(a.nombre)}
                  >
                    <span className="fila-radio">
                      <span>{acoms.includes(a.nombre) ? "☑" : "☐"}</span> {a.nombre}
                    </span>
                    <span className="fila">Incluido</span>
                  </button>
                ))}
              </div>
            </>
          )}

          <button className="btn ghost" onClick={agregarPlato}>
            + Agregar plato
          </button>

          {platos.length > 0 && (
            <>
              <div className="seccion-t" style={{ marginTop: 8 }}>
                Platos del pedido
              </div>
              {platos.map((d, i) => {
                const pp = proteinas.find((x) => x.id === d.idProteina)?.precio ?? 4600;
                return (
                  <div className="menu-agregado" key={i}>
                    <div>
                      <div className="nombre">
                        {proteinas.find((x) => x.id === d.idProteina)?.nombre}
                      </div>
                      <div className="detalle">
                        {d.solo
                          ? "Solo proteína"
                          : d.acompanamientos.join(" · ")}
                      </div>
                    </div>
                    <span className="espacio" style={{ flex: 1 }} />
                    <span style={{ fontWeight: 700 }}>{dinero(d.solo ? PRECIO_SOLO_PROTEINA : pp)}</span>
                    <button className="btn danger-ghost small" onClick={() => quitar(i)}>
                      Quitar
                    </button>
                  </div>
                );
              })}
            </>
          )}

          <div style={{ display: "flex", justifyContent: "flex-end", fontWeight: 800 }}>
            Total: {dinero(total)}
          </div>
        </div>

        {error && <div className="error">{error}</div>}

        <div className="modal-pie">
          <button className="btn ghost" onClick={onClose}>
            Cancelar
          </button>
          <button className="btn primary" onClick={guardar}>
            Registrar pedido
          </button>
        </div>
      </div>
    </div>
  );
}