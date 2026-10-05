import { useState } from 'react'
import { api, hora } from './api.js'

/**
 * Tablón público: acusaciones a la vista de todos (con la respuesta del acusado y la
 * pista que enseñe), rumores anónimos y avisos del Máster. Al terminar la partida,
 * también las acusaciones finales de cada uno.
 */
export default function Tablon({ datos, otros, recargar, nuevas }) {
  const yoId = datos.personaje.id
  const entradas = datos.tablon ?? []
  const finales = datos.acusacionesFinales ?? []
  const pendientes = entradas.filter((e) => e.tipo === 'ACUSACION' && e.acusadoId === yoId && !e.respuesta)

  return (
    <section>
      {finales.length > 0 && <AcusacionesFinales finales={finales} />}

      {pendientes.length > 0 && (
        <p className="aviso-ok aviso-acento">
          Te han acusado en público{pendientes.length > 1 ? ` ${pendientes.length} veces` : ''}. Responde abajo: todos lo verán.
        </p>
      )}

      <NuevaAcusacion otros={otros.filter((o) => !o.esMaster)} recargar={recargar} />

      <h2>Lo último</h2>
      {entradas.length === 0 && <p className="vacio">Nadie ha dicho nada todavía. Sé el primero en señalar a alguien.</p>}
      {entradas.map((e) => (
        <Entrada key={e.id} entrada={e} nueva={nuevas?.has(`t${e.id}`)} esperaMiRespuesta={e.acusadoId === yoId && !e.respuesta}>
          {e.tipo === 'ACUSACION' && e.acusadoId === yoId && !e.respuesta && (
            <Responder entrada={e} pistas={datos.pistas} recargar={recargar} />
          )}
        </Entrada>
      ))}
    </section>
  )
}

/** Versión del Máster: ve quién escribe los rumores, publica avisos y puede borrar. */
export function TablonMaster({ entradas, hacer }) {
  const [texto, setTexto] = useState('')

  async function publicar(evento) {
    evento.preventDefault()
    if (!texto.trim()) return
    if (await hacer('POST', '/tablon', { texto })) setTexto('')
  }

  return (
    <>
      <h2>Tablón público</h2>
      <form className="tarjeta" onSubmit={publicar}>
        <textarea rows={2} value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={1000} placeholder="Aviso para todo el grupo" aria-label="Aviso" />
        <button className="primario" disabled={!texto.trim()}>
          Publicar aviso
        </button>
      </form>
      {entradas.length === 0 && <p className="vacio">El tablón está vacío.</p>}
      {entradas.map((e) => (
        <Entrada key={e.id} entrada={e} master>
          <button className="enlace peligro" onClick={() => hacer('DELETE', `/tablon/${e.id}`)}>
            Borrar
          </button>
        </Entrada>
      ))}
    </>
  )
}

function Entrada({ entrada: e, nueva, master = false, esperaMiRespuesta = false, children }) {
  if (e.tipo === 'RUMOR') {
    return (
      <article className={`tarjeta publicacion rumor ${nueva ? 'nuevo' : ''}`}>
        <p className="publicacion-cabecera">
          <span>Se rumorea…{master && e.autor ? <span className="tenue"> (lo escribió {e.autor})</span> : null}</span>
          <span className="tenue">{hora(e.creadaEn)}</span>
        </p>
        <p className="rumor-texto">«{e.texto}»</p>
        {children}
      </article>
    )
  }
  if (e.tipo === 'AVISO') {
    return (
      <article className={`tarjeta publicacion aviso ${nueva ? 'nuevo' : ''}`}>
        <p className="publicacion-cabecera">
          <strong>Aviso del Máster</strong>
          <span className="tenue">{hora(e.creadaEn)}</span>
        </p>
        <p>{e.texto}</p>
        {children}
      </article>
    )
  }
  return (
    <article className={`tarjeta publicacion acusacion ${nueva ? 'nuevo' : ''}`}>
      <p className="publicacion-cabecera">
        <span>
          <strong>{e.autor}</strong> acusa a <strong>{e.acusado}</strong>
        </span>
        <span className="tenue">{hora(e.creadaEn)}</span>
      </p>
      <p>{e.texto}</p>
      {e.respuesta ? (
        <div className="respuesta">
          <p className="publicacion-cabecera">
            <strong>Responde {e.acusado}</strong>
            <span className="tenue">{hora(e.respondidaEn)}</span>
          </p>
          <p>{e.respuesta}</p>
          {e.pistaMostrada && (
            <div className="pista-mostrada">
              <span className="etiqueta">Enseña una pista</span>
              <h3>{e.pistaMostrada.titulo}</h3>
              <p>{e.pistaMostrada.contenido}</p>
            </div>
          )}
        </div>
      ) : (
        !esperaMiRespuesta && <p className="tenue sin-respuesta">{e.acusado} todavía no ha respondido.</p>
      )}
      {children}
    </article>
  )
}

function NuevaAcusacion({ otros, recargar }) {
  const [abierta, setAbierta] = useState(false)
  const [acusadoId, setAcusadoId] = useState('')
  const [texto, setTexto] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function acusar(evento) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await api('POST', '/yo/tablon', { acusadoId: Number(acusadoId), texto })
      setAcusadoId('')
      setTexto('')
      setAbierta(false)
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  if (!abierta) {
    return (
      <button type="button" className="primario ancho" onClick={() => setAbierta(true)}>
        Acusar a alguien en público
      </button>
    )
  }
  return (
    <form className="tarjeta" onSubmit={acusar}>
      <h3>Acusación pública</h3>
      <p className="tenue">Todo el grupo la verá con tu nombre, y el acusado podrá defenderse enseñando una de sus pistas.</p>
      {error && <p className="error redondeado">{error}</p>}
      <select value={acusadoId} onChange={(e) => setAcusadoId(e.target.value)} aria-label="A quién acusas">
        <option value="">¿A quién acusas?</option>
        {otros.map((o) => (
          <option key={o.id} value={o.id}>
            {o.nombre}
          </option>
        ))}
      </select>
      <textarea rows={3} value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={1000} placeholder="De qué le acusas y por qué" aria-label="Acusación" />
      <div className="botonera">
        <button className="primario" disabled={enviando || !acusadoId || !texto.trim()}>
          Publicar acusación
        </button>
        <button type="button" onClick={() => setAbierta(false)}>
          Cancelar
        </button>
      </div>
    </form>
  )
}

function Responder({ entrada, pistas, recargar }) {
  const [texto, setTexto] = useState('')
  const [pistaId, setPistaId] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function responder(evento) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await api('POST', `/yo/tablon/${entrada.id}/respuesta`, { texto, pistaId: pistaId ? Number(pistaId) : null })
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="responder" onSubmit={responder}>
      {error && <p className="error redondeado">{error}</p>}
      <textarea rows={2} value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={1000} placeholder="Tu defensa (solo puedes responder una vez)" aria-label="Respuesta" />
      <select value={pistaId} onChange={(e) => setPistaId(e.target.value)} aria-label="Pista que enseñas">
        <option value="">Sin enseñar ninguna pista</option>
        {pistas.map((p) => (
          <option key={p.id} value={p.id}>
            Enseñar: {p.titulo}
          </option>
        ))}
      </select>
      {pistaId && <p className="tenue">Ojo: todo el grupo podrá leer esa pista entera.</p>}
      <button className="primario" disabled={enviando || !texto.trim()}>
        Responder en público
      </button>
    </form>
  )
}

function AcusacionesFinales({ finales }) {
  return (
    <>
      <h2>Acusaciones finales</h2>
      {finales.map((f) => (
        <article key={f.jugadorId} className="tarjeta acusacion-final-publica">
          <p>
            <strong>{f.jugador}</strong> acusa a <strong>{f.sospechoso ?? 'nadie'}</strong>
            {f.arma ? <> con {f.arma.charAt(0).toLowerCase() + f.arma.slice(1)}</> : null}
          </p>
          {f.razon && <p className="tenue">{f.razon}</p>}
        </article>
      ))}
    </>
  )
}
