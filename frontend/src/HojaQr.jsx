import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import QRCode from 'qrcode'
import { enlaceQr } from './api.js'

/**
 * Hoja para imprimir los QR de las pistas y esconderlos por la casa. Cada tarjeta lleva
 * solo un número y el código (nada del contenido); al final va un índice para el Máster.
 */
export default function HojaQr({ pistas, alCerrar }) {
  const conQr = pistas.filter((p) => p.codigoQr)
  const [imagenes, setImagenes] = useState({})

  useEffect(() => {
    let vivo = true
    Promise.all(
      conQr.map(async (p) => [p.codigoQr, await QRCode.toDataURL(enlaceQr(p.codigoQr), { margin: 1, width: 360, errorCorrectionLevel: 'M' })]),
    ).then((pares) => vivo && setImagenes(Object.fromEntries(pares)))
    return () => {
      vivo = false
    }
  }, [conQr.map((p) => p.codigoQr).join()])

  useEffect(() => {
    document.body.classList.add('imprimiendo-qr')
    return () => document.body.classList.remove('imprimiendo-qr')
  }, [])

  // Fuera de #root: al imprimir se oculta la app entera y solo sale la hoja.
  return createPortal(
    <div className="hoja-qr" role="dialog" aria-modal="true" aria-label="QR para imprimir">
      <div className="hoja-qr-barra">
        <strong>{conQr.length} QR</strong>
        <span className="botonera">
          <button className="primario" onClick={() => window.print()} disabled={Object.keys(imagenes).length < conQr.length}>
            Imprimir
          </button>
          <button onClick={alCerrar}>Cerrar</button>
        </span>
      </div>
      {conQr.length === 0 ? (
        <p className="vacio">Ninguna pista tiene QR todavía. Créalos en cada pista con «Crear QR».</p>
      ) : (
        <>
          <div className="qr-rejilla">
            {conQr.map((p, i) => (
              <div key={p.id} className="qr-tarjeta">
                <span className="qr-marca">Misterio en Vivo</span>
                {imagenes[p.codigoQr] ? <img src={imagenes[p.codigoQr]} alt={`QR ${p.codigoQr}`} /> : <span className="qr-cargando">…</span>}
                <span className="qr-instruccion">Escanéalo con la cámara</span>
                <span className="qr-codigo">
                  #{i + 1} · {p.codigoQr}
                </span>
              </div>
            ))}
          </div>
          <div className="qr-indice">
            <h2>Índice (solo para el Máster)</h2>
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Código</th>
                  <th>Pista</th>
                  <th>Dónde</th>
                </tr>
              </thead>
              <tbody>
                {conQr.map((p, i) => (
                  <tr key={p.id}>
                    <td>{i + 1}</td>
                    <td>{p.codigoQr}</td>
                    <td>{p.titulo}</td>
                    <td>{p.nota ?? ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>,
    document.body,
  )
}
