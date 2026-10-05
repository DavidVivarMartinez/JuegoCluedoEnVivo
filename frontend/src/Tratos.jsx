import { useState } from 'react'
import { api, hora } from './api.js'

const ESTADOS = { PROPUESTO: 'Pendiente', ACEPTADO: 'Aceptado', RECHAZADO: 'Rechazado', CANCELADO: 'Retirado' }

/**
 * Tratos con otros jugadores: ofrezco una pista y/o monedas a cambio de una de sus pistas
 * (la elige él al aceptar) y/o monedas. Las pistas se comparten: quien da una la conserva.
 */
export default function Tratos({ datos, otros, recargar }) {
  const yoId = datos.personaje.id
  const tratos = datos.tratos ?? []
  const recibidos = tratos.filter((t) => t.estado === 'PROPUESTO' && t.paraId === yoId)
  const enviados = tratos.filter((t) => t.estado === 'PROPUESTO' && t.deId === yoId)
  const cerrados = tratos.filter((t) => t.estado !== 'PROPUESTO')

  return (
    <>
      <h2>Tratos</h2>
      <p className="tenue">Cambia pistas por pistas o por monedas. Nada se mueve hasta que el otro acepta; quien da una pista la conserva.</p>
      {recibidos.map((t) => (
        <Recibido key={t.id} trato={t} saldo={datos.dinero ?? 0} recargar={recargar} />
      ))}
      {enviados.map((t) => (
        <article key={t.id} className="tarjeta trato">
          <p className="publicacion-cabecera">
            <span>
              Propuesto a <strong>{t.para}</strong>
            </span>
            <span className="tenue">{hora(t.creadoEn)}</span>
          </p>
          <Condiciones trato={t} soyQuienPropone />
          <AccionTrato texto="Retirar" ruta={`/yo/tratos/${t.id}/cancelar`} recargar={recargar} />
        </article>
      ))}
      <NuevoTrato datos={datos} otros={otros.filter((o) => !o.esMaster)} recargar={recargar} />
      {cerrados.length > 0 && (
        <details className="historial">
          <summary>Tratos cerrados ({cerrados.length})</summary>
          {cerrados.map((t) => (
            <article key={t.id} className="tarjeta trato cerrado">
              <p className="publicacion-cabecera">
                <span>{t.deId === yoId ? `Con ${t.para}` : `De ${t.de}`}</span>
                <span className={`etiqueta ${t.estado === 'ACEPTADO' ? 'cumplido' : ''}`}>{ESTADOS[t.estado]}</span>
              </p>
              <Condiciones trato={t} soyQuienPropone={t.deId === yoId} />
              {t.pistaRecibida && <p className="tenue">A cambio dio «{t.pistaRecibida.titulo}».</p>}
            </article>
          ))}
        </details>
      )}
    </>
  )
}

/** Qué da cada uno, dicho desde mi punto de vista. */
function Condiciones({ trato: t, soyQuienPropone }) {
  const ofrece = [t.pistaOfrecida && `la pista «${t.pistaOfrecida.titulo}»`, t.dineroOfrecido > 0 && `${t.dineroOfrecido} monedas`].filter(Boolean)
  const pide = [t.pidePista && 'una pista', t.dineroPedido > 0 && `${t.dineroPedido} monedas`].filter(Boolean)
  return (
    <>
      <p className="trato-linea">
        <span className="trato-flecha sale">{soyQuienPropone ? 'Das' : 'Te da'}</span> {ofrece.join(' y ')}
      </p>
      <p className="trato-linea">
        <span className="trato-flecha entra">{soyQuienPropone ? 'Pides' : 'Te pide'}</span> {pide.join(' y ')}
        {t.pidePista && !soyQuienPropone ? ' (tú eliges cuál)' : ''}
      </p>
      {t.mensaje && <p className="tenue">«{t.mensaje}»</p>}
    </>
  )
}

function Recibido({ trato: t, saldo, recargar }) {
  const [pistaId, setPistaId] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const sinPistas = t.pidePista && t.puedoDar.length === 0
  const sinDinero = t.dineroPedido > saldo

  async function aceptar() {
    setError(null)
    setEnviando(true)
    try {
      await api('POST', `/yo/tratos/${t.id}/aceptar`, { pistaId: pistaId ? Number(pistaId) : null })
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <article className="tarjeta trato nuevo">
      <p className="publicacion-cabecera">
        <span>
          <strong>{t.de}</strong> te propone un trato
        </span>
        <span className="tenue">{hora(t.creadoEn)}</span>
      </p>
      <Condiciones trato={t} soyQuienPropone={false} />
      {error && <p className="error redondeado">{error}</p>}
      {t.pidePista && !sinPistas && (
        <select value={pistaId} onChange={(e) => setPistaId(e.target.value)} aria-label="Pista que das a cambio">
          <option value="">¿Qué pista le das?</option>
          {t.puedoDar.map((p) => (
            <option key={p.id} value={p.id}>
              {p.titulo}
            </option>
          ))}
        </select>
      )}
      {sinPistas && <p className="tenue">No tienes ninguna pista que {t.de} no tenga ya.</p>}
      {sinDinero && <p className="tenue">No te llega el dinero que pide.</p>}
      <div className="botonera">
        <button className="primario" onClick={aceptar} disabled={enviando || sinPistas || sinDinero || (t.pidePista && !pistaId)}>
          Aceptar
        </button>
        <AccionTrato texto="Rechazar" ruta={`/yo/tratos/${t.id}/rechazar`} recargar={recargar} />
      </div>
    </article>
  )
}

function AccionTrato({ texto, ruta, recargar }) {
  const [enviando, setEnviando] = useState(false)
  async function hacer() {
    setEnviando(true)
    try {
      await api('POST', ruta)
    } finally {
      setEnviando(false)
      recargar()
    }
  }
  return (
    <button type="button" onClick={hacer} disabled={enviando}>
      {texto}
    </button>
  )
}

function NuevoTrato({ datos, otros, recargar }) {
  const [abierto, setAbierto] = useState(false)
  const [paraId, setParaId] = useState('')
  const [pistaId, setPistaId] = useState('')
  const [dinero, setDinero] = useState('')
  const [pidePista, setPidePista] = useState(true)
  const [dineroPedido, setDineroPedido] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const saldo = datos.dinero ?? 0
  const da = Number(dinero) || 0
  const pide = Number(dineroPedido) || 0
  const valido = paraId && (pistaId || da > 0) && (pidePista || pide > 0) && da <= saldo

  async function proponer(evento) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await api('POST', '/yo/tratos', {
        paraId: Number(paraId), pistaId: pistaId ? Number(pistaId) : null, dinero: da, pidePista, dineroPedido: pide, mensaje,
      })
      setAbierto(false)
      setParaId('')
      setPistaId('')
      setDinero('')
      setDineroPedido('')
      setMensaje('')
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  if (!abierto) {
    return (
      <button type="button" className="ancho" onClick={() => setAbierto(true)}>
        Proponer un trato
      </button>
    )
  }
  return (
    <form className="tarjeta" onSubmit={proponer}>
      <h3>Nuevo trato</h3>
      {error && <p className="error redondeado">{error}</p>}
      <select value={paraId} onChange={(e) => setParaId(e.target.value)} aria-label="Con quién">
        <option value="">¿Con quién?</option>
        {otros.map((o) => (
          <option key={o.id} value={o.id}>
            {o.nombre}
          </option>
        ))}
      </select>
      <p className="subtitulo">Ofreces</p>
      <select value={pistaId} onChange={(e) => setPistaId(e.target.value)} aria-label="Pista que ofreces">
        <option value="">Ninguna pista</option>
        {datos.pistas.map((p) => (
          <option key={p.id} value={p.id}>
            {p.titulo}
          </option>
        ))}
      </select>
      <input type="number" inputMode="numeric" min="0" max={saldo} value={dinero} onChange={(e) => setDinero(e.target.value)} placeholder={`Monedas (tienes ${saldo})`} aria-label="Monedas que ofreces" />
      <p className="subtitulo">Pides</p>
      <label className="casilla">
        <input type="checkbox" checked={pidePista} onChange={(e) => setPidePista(e.target.checked)} />
        Una de sus pistas (la elige al aceptar)
      </label>
      <input type="number" inputMode="numeric" min="0" value={dineroPedido} onChange={(e) => setDineroPedido(e.target.value)} placeholder="Monedas que pides" aria-label="Monedas que pides" />
      <input value={mensaje} onChange={(e) => setMensaje(e.target.value)} maxLength={300} placeholder="Mensaje (opcional)" aria-label="Mensaje" />
      <div className="botonera">
        <button className="primario" disabled={!valido || enviando}>
          Proponer
        </button>
        <button type="button" onClick={() => setAbierto(false)}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
