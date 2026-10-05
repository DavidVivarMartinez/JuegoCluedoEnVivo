import { useState } from 'react'
import { api, hora } from './api.js'

// Estas habilidades necesitan elegir a alguien aunque la ficha no lo diga (el motor lo exige).
const CON_PERSONA = ['VER_PISTAS', 'VER_MOVIMIENTOS']

/** Habilidades de oficio del jugador: un solo uso cada una. */
export default function Habilidades({ habilidades, otros, recargar }) {
  if (!habilidades?.length) return null
  return (
    <>
      <h2>{habilidades.length > 1 ? 'Tus habilidades' : 'Tu habilidad'}</h2>
      {habilidades.map((h) => (
        <Habilidad key={h.id} habilidad={h} otros={otros.filter((o) => !o.esMaster)} recargar={recargar} />
      ))}
    </>
  )
}

function Habilidad({ habilidad: h, otros, recargar }) {
  const [personaId, setPersonaId] = useState('')
  const [texto, setTexto] = useState('')
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const pidePersona = h.pidePersona || CON_PERSONA.includes(h.tipo)
  const pideTexto = h.pideTexto || (h.tipo === 'RUMOR_ANONIMO' ? '¿Qué rumor publicas?' : null)
  const listo = (!pidePersona || personaId) && (!pideTexto || texto.trim())

  async function usar() {
    setError(null)
    setEnviando(true)
    try {
      await api('POST', `/yo/habilidades/${h.id}`, { personaId: personaId ? Number(personaId) : null, texto })
      await recargar()
    } catch (e) {
      setError(e.message)
      setConfirmando(false)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <article className={`tarjeta habilidad ${h.estado.toLowerCase()}`}>
      <div className="habilidad-cabecera">
        <h3>{h.nombre}</h3>
        <span className={`etiqueta ${h.estado === 'DISPONIBLE' ? 'principal' : h.estado === 'RESUELTA' ? 'cumplido' : 'entregado'}`}>
          {h.estado === 'DISPONIBLE' ? 'Un solo uso' : h.estado === 'SOLICITADA' ? 'Esperando al Máster' : 'Usada'}
        </span>
      </div>
      <p>{h.descripcion}</p>

      {h.estado === 'DISPONIBLE' && (
        <div className="habilidad-uso">
          {error && <p className="error redondeado">{error}</p>}
          {pidePersona && (
            <select value={personaId} onChange={(e) => setPersonaId(e.target.value)} aria-label="Sobre quién" disabled={confirmando}>
              <option value="">¿Sobre quién?</option>
              {otros.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nombre}
                </option>
              ))}
            </select>
          )}
          {pideTexto && (
            <textarea rows={2} value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={2000} placeholder={pideTexto} aria-label={pideTexto} disabled={confirmando} />
          )}
          {confirmando ? (
            <div className="botonera">
              <button className="primario" onClick={usar} disabled={enviando}>
                Sí, usarla ya
              </button>
              <button onClick={() => setConfirmando(false)} disabled={enviando}>
                Mejor luego
              </button>
            </div>
          ) : (
            <button className="primario ancho" disabled={!listo} onClick={() => setConfirmando(true)}>
              Usar habilidad
            </button>
          )}
          {confirmando && <p className="tenue">Solo la puedes usar una vez en todo el fin de semana.</p>}
        </div>
      )}

      {h.estado !== 'DISPONIBLE' && (
        <div className="habilidad-resultado">
          <p className="tenue">
            Usada {hora(h.usadaEn)}
            {h.persona ? ` sobre ${h.persona}` : ''}
            {h.peticion ? `: «${h.peticion}»` : ''}
          </p>
          {h.respuesta ? <p className="respuesta-habilidad">{h.respuesta}</p> : <p className="tenue">El Máster te contestará aquí en cuanto pueda.</p>}
        </div>
      )}
    </article>
  )
}
