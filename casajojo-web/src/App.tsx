import { NavLink, Route, Routes } from "react-router-dom";
import { Comandas } from "./pages/Comandas";
import { Cocina } from "./pages/Cocina";
import { MenuAdmin } from "./pages/MenuAdmin";
import { Resumen } from "./pages/Resumen";

export function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          La Casa <b>JoJo</b> <span className="accent">Panel</span>
          <span className="tag">Administración</span>
        </div>
        <nav>
          <NavLink to="/" end>
            Comandas
          </NavLink>
          <NavLink to="/cocina">Cocina</NavLink>
          <NavLink to="/menu">Menú</NavLink>
          <NavLink to="/resumen">Resumen</NavLink>
        </nav>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Comandas />} />
          <Route path="/cocina" element={<Cocina />} />
          <Route path="/menu" element={<MenuAdmin />} />
          <Route path="/resumen" element={<Resumen />} />
        </Routes>
      </main>
    </div>
  );
}