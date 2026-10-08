import { useState } from "react";
import { useStore } from "../store";
import type { Producto, TipoProducto } from "../types";
import { dinero } from "../domain";

export function MenuAdmin() {
  const { estado, toggleProducto, guardarProducto } = useStore();

  const [nombre, setNombre] = useState("");
  const [tipo, setTipo] = useState<TipoProducto>("proteina");
  const [precio, setPrecio] = useState("");

  const proteinas = estado.productos.filter((p) => p.tipo === "proteina");
  const acom = estado.productos.filter((p) => p.tipo === "acompanamiento");

  function agregar() {
    const n = nombre.trim();
    if (!n) return;
    const maxId = estado.productos.reduce((m, p) => Math.max(m, p.id), 0);
    guardarProducto({
      id: maxId + 1,
      nombre: n,
      tipo,
      precio: tipo === "proteina" ? Math.max(0, Number(precio) || 0) : 0,
      disponible: true,
    });
    setNombre("");
    setPrecio("");
  }

  return (
    <section>
      <div className="pagina-cab">
        <div>
          <h1>Menú</h1>
          <div className="sub">Administración del menú (RF-02) y stock (RF-07)</div>
        </div>
      </div>

      <div className="caja">
        <div className="seccion-t" style={{ marginBottom: 8 }}>
          Agregar opción
        </div>
        <div className="barra-herramientas">
          <input
            className="input"
            placeholder="Nombre de la opción"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
          <select className="input" value={tipo} onChange={(e) => setTipo(e.target.value as TipoProducto)}>
            <option value="proteina">Proteína</option>
            <option value="acompanamiento">Acompañamiento</option>
          </select>
          {tipo === "proteina" && (
            <input
              className="input"
              style={{ width: 110 }}
              placeholder="Precio"
              value={precio}
              onChange={(e) => setPrecio(e.target.value)}
            />
          )}
          <button className="btn primary" onClick={agregar}>
            Agregar
          </button>
        </div>
      </div>

      <div className="grupo-menu">
        <h2>Proteínas</h2>
        {proteinas.map((p) => (
          <FilaProducto key={p.id} producto={p} onToggle={() => toggleProducto(p.id)} onSave={guardarProducto} />
        ))}
      </div>

      <div className="grupo-menu">
        <h2>Acompañamientos</h2>
        {acom.map((p) => (
          <FilaProducto key={p.id} producto={p} onToggle={() => toggleProducto(p.id)} onSave={guardarProducto} />
        ))}
      </div>
    </section>
  );
}

function FilaProducto({
  producto,
  onToggle,
  onSave,
}: {
  producto: Producto;
  onToggle: () => void;
  onSave: (p: Producto) => void;
}) {
  const [nombre, setNombre] = useState(producto.nombre);
  const [precio, setPrecio] = useState(String(producto.precio));

  return (
    <div className="prod-fila">
      <span className="pch st-neutro">{producto.tipo === "proteina" ? "Proteína" : "Acompañamiento"}</span>
      <input
        className="nombre"
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        onBlur={() => onSave({ ...producto, nombre: nombre.trim() || producto.nombre })}
      />
      {producto.tipo === "proteina" ? (
        <input
          className="precio"
          value={precio}
          onChange={(e) => setPrecio(e.target.value)}
          onBlur={() => onSave({ ...producto, precio: Math.max(0, Number(precio) || 0) })}
        />
      ) : (
        <span className="sub" style={{ width: 84, textAlign: "right" }}>Incluido</span>
      )}
      <span className="precio mut2" style={{ fontWeight: 700, width: 70, textAlign: "right" }}>
        {producto.tipo === "proteina" ? dinero(producto.precio) : ""}
      </span>
      <button className="btn small ghost" onClick={onToggle}>
        {producto.disponible ? "Agotar" : "Disponible"}
      </button>
      {producto.disponible ? (
        <span className="pch st-entregado">Disponible</span>
      ) : (
        <span className="pch st-cancelado">Agotado</span>
      )}
    </div>
  );
}