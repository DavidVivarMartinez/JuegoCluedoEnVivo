import { useState } from 'react'
import { cuentaAtras, hora } from './api.js'

/** Primera línea de la nota = nombre de la visión (momento o título); el resto, la explicación. */
function partesNota(nota) {
  const [primera = '', ...resto] = (nota ?? '').split('\n')
  return { nombre: primera.trim(), detalle: resto.join('\n').trim() }
}

function tituloDe(e) {
  if (e.tipo === 'PISTA') return e.pistaTitulo ?? 'Sobre'
  return partesNota(e.nota).nombre || 'Visión'
}

function cuerpoDe(e, cambios = {}) {
  return {
    tipo: e.tipo, pistaId: e.pistaId, imagenUrl: e.imagenUrl, nota: e.nota, jugadorIds: e.jugadorIds,
    programadaPara: e.programadaPara, anunciar: e.anunciar, ...cambios,
  }
}

/**
 * Sobres programados y visiones de la víctima. Lo programado se entrega solo a su hora;
 * las visiones de reserva se mandan a mano a quien haga falta.
 */
export default function EnviosMaster({ datos, hacer }) {
  const [filtro, setFiltro] = useState('')
  const nombre = new Map(datos.jugadores.map((j) => [j.id, j.nombre]))
  const deFiltro = (e) => !filtro || e.jugadorIds.includes(Number(filtro))
  const envios = datos.envios ?? []
  const programados = envios
    .filter((e) => !e.enviadoEn && e.programadaPara && deFiltro(e))
    .sort((a, b) => new Date(a.programadaPara) - new Date(b.programadaPara))
  const reserva = envios.filter((e) => e.tipo === 'VISION' && !e.enviadoEn && !e.programadaPara)
  const manuales = envios.filter((e) => e.tipo === 'PISTA' && !e.enviadoEn && !e.programadaPara && deFiltro(e))
  const enviados = envios.filter((e) => e.enviadoEn && deFiltro(e)).sort((a, b) => new Date(b.enviadoEn) - new Date(a.enviadoEn))
  const sinImagen = envios.filter((e) => e.tipo === 'VISION' && !e.enviadoEn && !e.imagenUrl).length

  return (
    <section>
      <h2>Envíos</h2>
      <p className="tenue">
        Los sobres abren una pista en el móvil de sus destinatarios a su hora; las visiones les mandan una imagen sin texto. Lo programado sale solo.
      </p>
      {sinImagen > 0 && (
        <p className="error redondeado">
          {sinImagen} {sinImagen === 1 ? 'visión no tiene' : 'visiones no tienen'} imagen todavía: sin imagen no se envían. Pega el enlace en cada una.
        </p>
      )}
      <select value={filtro} onChange={(e) => setFiltro(e.target.value)} aria-label="Ver los de">
        <option value="">Todos los jugadores</option>
        {datos.jugadores.map((j) => (
          <option key={j.id} value={j.id}>
            Solo los de {j.nombre}
          </option>
        ))}
      </select>

      <h2>Programados ({programados.length})</h2>
      {programados.length === 0 && <p className="vacio">No hay nada programado.</p>}
      {programados.map((e) => (
        <Envio key={e.id} envio={e} nombre={nombre} hacer={hacer} />
      ))}

      {manuales.length > 0 && (
        <>
          <h2>Sobres sin hora</h2>
          {manuales.map((e) => (
            <Envio key={e.id} envio={e} nombre={nombre} hacer={hacer} />
          ))}
        </>
      )}

      <h2>Visiones de reserva ({reserva.length})</h2>
      <p className="tenue">Para mandar a quien quieras según vaya la partida. Se pueden mandar varias veces.</p>
      {reserva.map((e) => (
        <Envio key={e.id} envio={e} nombre={nombre} hacer={hacer} jugadores={datos.jugadores} reserva />
      ))}

      <NuevoEnvio datos={datos} hacer={hacer} />

      {enviados.length > 0 && (
        <details className="historial">
          <summary>Ya entregados ({enviados.length})</summary>
          {enviados.map((e) => (
            <article key={e.id} className="tarjeta envio enviado">
              <p className="publicacion-cabecera">
                <span>
                  <TipoEnvio tipo={e.tipo} /> {tituloDe(e)}
                </span>
                <span className="tenue">{hora(e.enviadoEn)}</span>
              </p>
              <p className="tenue">Para {e.jugadorIds.map((id) => nombre.get(id)).join(', ')}</p>
            </article>
          ))}
        </details>
      )}
    </section>
  )
}

function TipoEnvio({ tipo }) {
  return <span className={`etiqueta tipo-envio ${tipo === 'PISTA' ? 'principal' : 'recompensa'}`}>{tipo === 'PISTA' ? 'Sobre' : 'Visión'}</span>
}

function Envio({ envio: e, nombre, hacer, jugadores = [], reserva = false }) {
  const [imagen, setImagen] = useState(e.imagenUrl ?? '')
  const [destinos, setDestinos] = useState([])
  const [borrando, setBorrando] = useState(false)
  const [cambiando, setCambiando] = useState(false)
  const { detalle } = partesNota(e.nota)
  const atrasado = e.programadaPara && new Date(e.programadaPara) < new Date()
  const faltaImagen = e.tipo === 'VISION' && !e.imagenUrl

  function guardarImagen() {
    const limpia = imagen.trim()
    setCambiando(false)
    if (limpia === (e.imagenUrl ?? '')) return
    hacer('PUT', `/envios/${e.id}`, cuerpoDe(e, { imagenUrl: limpia || null }))
  }

  async function mandar() {
    if (await hacer('POST', `/envios/${e.id}/enviar`, { jugadorIds: destinos })) setDestinos([])
  }

  return (
    <article className={`tarjeta envio ${atrasado ? 'atrasado' : ''}`}>
      <div className="envio-cabecera">
        {e.tipo === 'VISION' && (
          <span className="envio-miniatura">{e.imagenUrl ? <img src={e.imagenUrl} alt="" loading="lazy" referrerPolicy="no-referrer" /> : '?'}</span>
        )}
        <span className="envio-texto">
          <span>
            <TipoEnvio tipo={e.tipo} /> <strong>{tituloDe(e)}</strong>
          </span>
          {!reserva && (
            <span className="tenue">
              Para {e.jugadorIds.length ? e.jugadorIds.map((id) => nombre.get(id)).join(', ') : 'nadie'}
              {e.programadaPara && ` · ${hora(e.programadaPara)} (${atrasado ? 'atrasado' : cuentaAtras(e.programadaPara)})`}
              {e.anunciar && ' · anunciado'}
            </span>
          )}
        </span>
      </div>
      {faltaImagen && <p className="aviso-falta">Falta la imagen: no se enviará hasta que la pongas.</p>}
      {detalle && (
        <details className="envio-nota">
          <summary>Qué es</summary>
          <p>{detalle}</p>
        </details>
      )}
      {e.tipo === 'VISION' && e.imagenUrl && !cambiando && (
        <button className="enlace" onClick={() => setCambiando(true)}>
          Cambiar imagen
        </button>
      )}
      {e.tipo === 'VISION' && (!e.imagenUrl || cambiando) && (
        <input
          value={imagen}
          onChange={(ev) => setImagen(ev.target.value)}
          onBlur={guardarImagen}
          onKeyDown={(ev) => ev.key === 'Enter' && ev.currentTarget.blur()}
          placeholder="Enlace de la imagen (https://…)"
          inputMode="url"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          aria-label="Enlace de la imagen"
        />
      )}
      {reserva && (
        <div className="casillas">
          {jugadores.map((j) => (
            <label key={j.id}>
              <input
                type="checkbox"
                checked={destinos.includes(j.id)}
                onChange={() => setDestinos(destinos.includes(j.id) ? destinos.filter((d) => d !== j.id) : [...destinos, j.id])}
              />
              {j.nombre}
            </label>
          ))}
        </div>
      )}
      <div className="fila">
        {reserva ? (
          <button className="primario" disabled={destinos.length === 0 || faltaImagen} onClick={mandar}>
            {destinos.length ? `Mandar a ${destinos.length}` : 'Elige a quién'}
          </button>
        ) : (
          <button className="primario" disabled={faltaImagen || e.jugadorIds.length === 0} onClick={() => hacer('POST', `/envios/${e.id}/enviar`, {})}>
            Enviar ya
          </button>
        )}
        {borrando ? (
          <span className="botonera">
            <button className="enlace peligro" onClick={() => hacer('DELETE', `/envios/${e.id}`)}>
              Sí, borrar
            </button>
            <button className="enlace" onClick={() => setBorrando(false)}>
              No
            </button>
          </span>
        ) : (
          <button className="enlace peligro" onClick={() => setBorrando(true)}>
            Borrar
          </button>
        )}
      </div>
    </article>
  )
}

function NuevoEnvio({ datos, hacer }) {
  const [tipo, setTipo] = useState('PISTA')
  const [pistaId, setPistaId] = useState('')
  const [imagenUrl, setImagenUrl] = useState('')
  const [nota, setNota] = useState('')
  const [destinos, setDestinos] = useState([])
  const [cuando, setCuando] = useState('')
  const [anunciar, setAnunciar] = useState(true)
  const valido = tipo === 'PISTA' ? pistaId : imagenUrl.trim() || nota.trim()

  async function crear(evento) {
    evento.preventDefault()
    const ok = await hacer('POST', '/envios', {
      tipo,
      pistaId: tipo === 'PISTA' ? Number(pistaId) : null,
      imagenUrl: tipo === 'VISION' ? imagenUrl.trim() || null : null,
      nota: nota.trim() || null,
      jugadorIds: destinos,
      programadaPara: cuando ? new Date(cuando).toISOString() : null,
      anunciar: tipo === 'PISTA' && anunciar,
    })
    if (ok) {
      setPistaId('')
      setImagenUrl('')
      setNota('')
      setDestinos([])
      setCuando('')
    }
  }

  return (
    <>
      <h2>Nuevo envío</h2>
      <form className="tarjeta" onSubmit={crear}>
        <div className="segmentos">
          {[['PISTA', 'Sobre (pista)'], ['VISION', 'Visión (imagen)']].map(([valor, titulo]) => (
            <button key={valor} type="button" className={tipo === valor ? 'activa' : ''} aria-pressed={tipo === valor} onClick={() => setTipo(valor)}>
              {titulo}
            </button>
          ))}
        </div>
        {tipo === 'PISTA' ? (
          <select value={pistaId} onChange={(e) => setPistaId(e.target.value)} aria-label="Pista del sobre">
            <option value="">¿Qué pista va dentro?</option>
            {datos.pistas.filter((p) => !p.falsa || p.estadoFalsa === 'APROBADA').map((p) => (
              <option key={p.id} value={p.id}>
                {p.titulo}
              </option>
            ))}
          </select>
        ) : (
          <input value={imagenUrl} onChange={(e) => setImagenUrl(e.target.value)} placeholder="Enlace de la imagen (https://…)" inputMode="url" autoCapitalize="none" aria-label="Imagen" />
        )}
        <textarea
          rows={2}
          value={nota}
          onChange={(e) => setNota(e.target.value)}
          placeholder={tipo === 'VISION' ? 'Nombre en la primera línea y, debajo, qué significa (solo lo ves tú)' : 'Nota para ti (opcional)'}
          aria-label="Nota"
        />
        <p className="subtitulo">Para</p>
        <div className="casillas">
          {datos.jugadores.map((j) => (
            <label key={j.id}>
              <input type="checkbox" checked={destinos.includes(j.id)} onChange={() => setDestinos(destinos.includes(j.id) ? destinos.filter((d) => d !== j.id) : [...destinos, j.id])} />
              {j.nombre}
            </label>
          ))}
        </div>
        <label>
          Cuándo (vacío = lo mandas tú a mano; una visión sin hora ni destinatarios queda de reserva)
          <input type="datetime-local" value={cuando} onChange={(e) => setCuando(e.target.value)} />
        </label>
        {tipo === 'PISTA' && (
          <label className="casilla">
            <input type="checkbox" checked={anunciar} onChange={(e) => setAnunciar(e.target.checked)} />
            Que vean el sobre sellado y la hora a la que se abre
          </label>
        )}
        <button className="primario" disabled={!valido}>
          Crear
        </button>
      </form>
    </>
  )
}

