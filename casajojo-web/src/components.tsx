import type { EstadoPago, EstadoPedido, Origen, Plato } from "./types";
import { CLS_ESTADO, CLS_PAGO, TXT_ESTADO, TXT_ORIGEN, TXT_PAGO } from "./domain";

export function BadgeEstado({ e }: { e: EstadoPedido }) {
  return <span className={`pch ${CLS_ESTADO[e]}`}>{TXT_ESTADO[e]}</span>;
}

export function BadgePago({ p }: { p: EstadoPago }) {
  return <span className={`pch ${CLS_PAGO[p]}`}>{TXT_PAGO[p]}</span>;
}

export function BadgeOrigen({ o }: { o: Origen }) {
  return <span className="pch origen">{TXT_ORIGEN[o]}</span>;
}

export function PlatoTexto({ plato }: { plato: Plato }) {
  if (plato.solo) {
    return <>{plato.nombreProteina} <span className="acom">(solo proteína)</span></>;
  }
  return (
    <>
      {plato.nombreProteina}
      <span className="acom"> — {plato.acompanamientos.join(" · ")}</span>
    </>
  );
}