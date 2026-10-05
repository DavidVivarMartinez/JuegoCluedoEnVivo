import { useCallback, useEffect, useState } from 'react'
import { api, cuentaAtras, FASES, hora, tomarQrPendiente } from './api.js'
import { useSondeo } from './useSondeo.js'
import Pestanas from './Pestanas.jsx'
import Avatar from './Avatar.jsx'
import CarruselPersonajes from './CarruselPersonajes.jsx'
import FichaPersonaje from './FichaPersonaje.jsx'
import Cuaderno from './Cuaderno.jsx'
import Tablon from './Tablon.jsx'
import Tratos from './Tratos.jsx'
import Habilidades from './Habilidades.jsx'
import Visor from './Visor.jsx'

const PESTANAS = [
  ['personaje', 'Personaje'],
  ['objetivos', 'Objetivos'],
  ['pistas', 'Pistas'],
  ['tablon', 'Tablón'],
  ['dinero', 'Dinero'],
  ['cuaderno', 'Cuaderno'],
]

const ESTADOS = { ACTIVO: 'Activo', ENTREGADO: 'Pendiente de validar', CUMPLIDO: 'Cumplido', FALLIDO: 'Fallido' }

export default function PanelJugador({ sesion, alSalir }) {
  const { datos, error, recargar } = useSondeo('/yo', 8000, alSalir)
  const [pestana, setPestana] = useState('personaje')
  const [fichaAbierta, setFichaAbierta] = useState(null) // id del personaje cuya ficha está abierta
  const [hallazgo, setHallazgo] = useState(null) // pista recién encontrada con un QR (o error al canjearlo)
  const { globos, nuevas } = useNovedades(sesion.jugadorId, datos, pestana)

  // La ficha abierta se apunta en el historial: el botón o gesto "atrás" del móvil la cierra.
  useEffect(() => {
    if (window.location.hash === '#personaje') window.history.replaceState(null, '', window.location.pathname)
    const alVolver = () => setFichaAbierta(null)
    window.addEventListener('popstate', alVolver)
    return () => window.removeEventListener('popstate', alVolver)
  }, [])

  // Si se entró escaneando un QR de la casa, se canjea nada más entrar.
  useEffect(() => {
    const codigo = tomarQrPendiente()
    if (codigo) canjear(codigo)
  }, [])

  const abrirFicha = useCallback((personaje) => {
    window.history.pushState({ ficha: personaje.id }, '', '#personaje')
    setFichaAbierta(personaje.id)
  }, [])

  const cerrarFicha = useCallback(() => {
    if (window.history.state?.ficha) window.history.back()
    else setFichaAbierta(null)
  }, [])

  async function canjear(codigo) {
    try {
      const pista = await api('POST', `/yo/qr/${encodeURIComponent(codigo.trim())}`)
      setHallazgo({ pista })
      setPestana('pistas')
      recargar()
      return true
    } catch (e) {
      setHallazgo({ error: e.message })
      return false
    }
  }

  if (!datos) return <p className="centrado">{error || 'Cargando…'}</p>

  // Si el backend en marcha es anterior a este frontend, faltará algún campo: mejor vacío que roto.
  const otros = datos.otrosPersonajes ?? []
  const personajeAbierto = fichaAbierta == null ? null : (otros.find((o) => o.id === fichaAbierta) ?? null)

  // Acciones que pueden fallar con un mensaje útil (sin saldo, arma bloqueada...): el error sube al formulario.
  async function accion(metodo, ruta, cuerpo) {
    try {
      return await api(metodo, ruta, cuerpo)
    } finally {
      recargar()
    }
  }

  async function guardarAnotacion(jugadorId, texto) {
    const respuesta = await api('PUT', `/yo/anotaciones/${jugadorId}`, { texto })
    recargar()
    return respuesta
  }

  async function entregar(objetivoId, datos) {
    try {
      await api('POST', `/yo/objetivos/${objetivoId}/entregas`, datos)
    } finally {
      recargar()
    }
  }

  async function borrarEntrega(entregaId) {
    try {
      await api('DELETE', `/yo/entregas/${entregaId}`)
    } finally {
      recargar()
    }
  }

  return (
    <div className="app">
      <header className="cabecera">
        <div className="cabecera-fila">
          <div className="cabecera-texto">
            <h1>{datos.partida.nombre}</h1>
            <p className="tenue">
              {datos.partida.casa ? `${datos.partida.casa} · ` : ''}
              {datos.personaje.nombre}
            </p>
          </div>
          <button type="button" className="saldo" onClick={() => setPestana('dinero')} aria-label={`Tienes ${datos.dinero ?? 0} monedas`}>
            {datos.dinero ?? 0}
            <Moneda />
          </button>
          <span className="fase">{FASES[datos.partida.fase]}</span>
        </div>
        <Pestanas pestanas={PESTANAS} activa={pestana} alCambiar={setPestana} globos={globos} />
      </header>

      {error && <p className="error">{error}</p>}

      <main className="contenido">
        {pestana === 'personaje' && <Personaje datos={datos} otros={otros} alAbrir={abrirFicha} alSalir={alSalir} recargar={recargar} />}
        {pestana === 'objetivos' && (
          <Objetivos objetivos={datos.objetivos} otros={otros} alEntregar={entregar} alBorrarEntrega={borrarEntrega} />
        )}
        {pestana === 'pistas' && <PistasYSecretos datos={datos} otros={otros} accion={accion} nuevas={nuevas} alCanjear={canjear} />}
        {pestana === 'tablon' && <Tablon datos={datos} otros={otros} recargar={recargar} nuevas={nuevas} />}
        {pestana === 'dinero' && <Dinero datos={datos} otros={otros} accion={accion} recargar={recargar} />}
        {pestana === 'cuaderno' && (
          <Cuaderno datos={datos} otros={otros} alAbrir={abrirFicha} alGuardarNota={guardarAnotacion} recargar={recargar} />
        )}
      </main>

      {personajeAbierto && (
        <FichaPersonaje
          personaje={personajeAbierto}
          secretosRevelados={datos.secretosRevelados.filter((s) => s.jugador === personajeAbierto.nombre)}
          alGuardar={guardarAnotacion}
          alCerrar={cerrarFicha}
        />
      )}

      {hallazgo && (
        <Visor alCerrar={() => setHallazgo(null)} etiqueta="Pista encontrada">
          {hallazgo.pista ? (
            <article className="tarjeta hallazgo">
              <span className="etiqueta recompensa">Has encontrado una pista</span>
              <h3>{hallazgo.pista.titulo}</h3>
              <p>{hallazgo.pista.contenido}</p>
              <p className="tenue">Ya la tienes guardada en Pistas.</p>
            </article>
          ) : (
            <article className="tarjeta">
              <h3>Ese código no abre nada</h3>
              <p>{hallazgo.error}</p>
            </article>
          )}
        </Visor>
      )}
    </div>
  )
}

/**
 * Lo nuevo desde la última vez que se miró cada pestaña. Se recuerda en este móvil
 * (es solo una comodidad): "globos" para la barra y "nuevas" para resaltar lo recién llegado.
 */
function useNovedades(jugadorId, datos, pestana) {
  const clave = `misterio.vistos.${jugadorId}`
  const [vistos, setVistos] = useState(() => {
    try {
      return new Set(JSON.parse(window.localStorage.getItem(clave)) ?? [])
    } catch {
      return new Set()
    }
  })
  const [nuevas, setNuevas] = useState(() => new Set())

  const grupos = !datos ? {} : {
    pistas: [...(datos.pistas ?? []).map((p) => `p${p.id}`), ...(datos.visiones ?? []).map((v) => `v${v.id}`)],
    tablon: (datos.tablon ?? []).filter((t) => t.autorId !== datos.personaje.id).map((t) => `t${t.id}`),
  }

  useEffect(() => {
    const ids = grupos[pestana]
    if (!ids) return
    const sinVer = ids.filter((id) => !vistos.has(id))
    if (sinVer.length === 0) return
    setNuevas((previas) => new Set([...previas, ...sinVer]))
    const todos = new Set([...vistos, ...sinVer])
    setVistos(todos)
    try {
      window.localStorage.setItem(clave, JSON.stringify([...todos]))
    } catch {
      // Sin almacenamiento: los avisos durarán lo que dure la pestaña.
    }
  }, [datos, pestana])

  const sinVer = (grupo) => (grupos[grupo] ?? []).filter((id) => !vistos.has(id)).length
  const globos = !datos ? {} : {
    pistas: pestana === 'pistas' ? 0 : sinVer('pistas'),
    tablon: (pestana === 'tablon' ? 0 : sinVer('tablon'))
      + (datos.tablon ?? []).filter((t) => t.tipo === 'ACUSACION' && t.acusadoId === datos.personaje.id && !t.respuesta).length,
    dinero: (datos.tratos ?? []).filter((t) => t.estado === 'PROPUESTO' && t.paraId === datos.personaje.id).length,
  }
  return { globos, nuevas }
}

// ---------------------------------------------------------------- Personaje

function Personaje({ datos, otros, alAbrir, alSalir, recargar }) {
  const p = datos.personaje
  const [elegido, setElegido] = useState(0)
  const indice = Math.min(elegido, Math.max(0, otros.length - 1))
  return (
    <section>
      <div className="presentacion">
        <Avatar nombre={p.nombre} imagenUrl={p.imagenUrl} tamano="grande" />
        <div>
          <h2>{p.nombre}</h2>
          <Basicos personaje={p} />
        </div>
      </div>
      <Bloque titulo="Personalidad" texto={p.personalidad} />
      <Bloque titulo="Relaciones" texto={p.relaciones} />
      <Bloque titulo="Contexto" texto={p.contexto} />
      <Bloque titulo="Herramienta de tu oficio" texto={p.herramienta} />
      {!p.personalidad && !p.relaciones && !p.contexto && <Vacio>Tu ficha todavía está en construcción.</Vacio>}

      <Habilidades habilidades={datos.habilidades} otros={otros} recargar={recargar} />

      <h2>En la casa</h2>
      {otros.length === 0 ? (
        <Vacio>Todavía no hay más personajes en la partida.</Vacio>
      ) : (
        <CarruselPersonajes personajes={otros} indice={indice} alCambiar={setElegido} alAbrir={alAbrir} />
      )}

      <button className="enlace salir" onClick={alSalir}>
        Salir de la partida
      </button>
    </section>
  )
}

// ---------------------------------------------------------------- Objetivos

function Objetivos({ objetivos, otros, alEntregar, alBorrarEntrega }) {
  const principal = objetivos.find((o) => o.principal)
  const secundarios = objetivos.filter((o) => !o.principal)
  const comunes = { otros, alEntregar, alBorrarEntrega }
  return (
    <section>
      <h2>Objetivo principal</h2>
      {principal ? <Objetivo objetivo={principal} principal {...comunes} /> : <Vacio>Todavía no tienes objetivo principal.</Vacio>}

      <h2>Objetivos secundarios</h2>
      {secundarios.length === 0 ? (
        <Vacio>Todavía no tienes objetivos secundarios.</Vacio>
      ) : (
        <p className="tenue">Toca un objetivo para aportar tus pruebas. El Máster las revisa y lo da por cumplido.</p>
      )}
      {secundarios.map((o) => (
        <Objetivo key={o.id} objetivo={o} {...comunes} />
      ))}
    </section>
  )
}

/** Tarjeta desplegable: arriba el objetivo y su estado; dentro, las pruebas aportadas y el formulario. */
function Objetivo({ objetivo: o, otros, alEntregar, alBorrarEntrega, principal = false }) {
  const abierto = o.estado === 'ACTIVO' || o.estado === 'ENTREGADO'
  const progreso = o.tipo !== 'LOGRO' && o.cantidad > 1 ? `${Math.min(o.entregas.length, o.cantidad)} de ${o.cantidad}` : null
  const admiteMas = abierto && (o.tipo !== 'LOGRO' || o.entregas.length === 0)
  return (
    <details className={`tarjeta objetivo ${principal ? 'objetivo-principal' : ''} ${o.estado.toLowerCase()}`}>
      <summary>
        <p>{o.texto}</p>
        <span className="fila">
          <span className={`etiqueta ${o.estado.toLowerCase()}`}>{ESTADOS[o.estado]}</span>
          {progreso && abierto && <span className="tenue">{progreso}</span>}
          {o.recompensaDinero > 0 && (
            <span className="etiqueta recompensa">
              {o.estado === 'CUMPLIDO' ? 'Cobrado' : '+'} {o.recompensaDinero} monedas
            </span>
          )}
          {o.tieneRecompensa && (
            <span className="etiqueta recompensa">{o.estado === 'CUMPLIDO' ? 'Pista desbloqueada' : 'Desbloquea una pista'}</span>
          )}
        </span>
      </summary>
      <div className="objetivo-detalle">
        {o.entregas.length > 0 && (
          <ul className="entregas">
            {o.entregas.map((e) => (
              <li key={e.id}>
                <span>
                  {e.persona && <strong>{e.persona}</strong>}
                  {e.persona && e.texto ? ': ' : ''}
                  {e.texto || (!e.persona ? 'Marcado como conseguido' : '')}
                </span>
                {abierto && (
                  <button className="enlace" onClick={() => alBorrarEntrega(e.id)}>
                    Quitar
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
        {admiteMas && <FormularioEntrega objetivo={o} otros={otros} alEntregar={alEntregar} />}
        {o.estado === 'ENTREGADO' && <p className="tenue">Entregado. El Máster lo revisará y lo dará por cumplido.</p>}
        {o.estado === 'CUMPLIDO' && (
          <p className="tenue">{o.tieneRecompensa ? 'Cumplido: tienes una pista nueva en Pistas.' : 'Cumplido.'}</p>
        )}
        {o.estado === 'FALLIDO' && <p className="tenue">El Máster lo ha dado por fallido.</p>}
      </div>
    </details>
  )
}

const TEXTOS_ENTREGA = {
  LOGRO: { marcador: 'Cuéntale al Máster cómo lo has conseguido (opcional)', boton: 'Lo he conseguido' },
  TEXTO: { marcador: 'Tu respuesta', boton: 'Enviar respuesta' },
  PERSONA: { marcador: 'Qué te contó, qué hizo o por qué', boton: 'Añadir' },
}

/** Formulario de prueba según el tipo: LOGRO (nota opcional), TEXTO (respuesta) o PERSONA (personaje + texto). */
function FormularioEntrega({ objetivo: o, otros = [], alEntregar }) {
  const [personaId, setPersonaId] = useState('')
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const textos = TEXTOS_ENTREGA[o.tipo] ?? TEXTOS_ENTREGA.LOGRO
  const incompleto = (o.tipo === 'PERSONA' && !personaId) || (o.tipo === 'TEXTO' && !texto.trim())

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await alEntregar(o.id, { personaId: personaId ? Number(personaId) : null, texto })
      setPersonaId('')
      setTexto('')
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar}>
      {error && <p className="error">{error}</p>}
      {o.tipo === 'PERSONA' && (
        <select value={personaId} onChange={(e) => setPersonaId(e.target.value)} aria-label="Personaje">
          <option value="">Elige a un personaje…</option>
          {otros.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nombre}
            </option>
          ))}
        </select>
      )}
      <textarea rows={2} value={texto} onChange={(e) => setTexto(e.target.value)} placeholder={textos.marcador} maxLength={2000} />
      <button className="primario" disabled={enviando || incompleto}>
        {textos.boton}
      </button>
    </form>
  )
}

// ---------------------------------------------------------------- Pistas y secretos

function PistasYSecretos({ datos, otros, accion, nuevas, alCanjear }) {
  const [vision, setVision] = useState(null)
  const visiones = datos.visiones ?? []
  const sellados = datos.sobresSellados ?? []

  return (
    <section>
      <CodigoQr alCanjear={alCanjear} />

      {sellados.length > 0 && (
        <>
          <h2>Sobres por abrir</h2>
          <div className="sobres">
            {sellados.map((s) => (
              <article key={s.id} className="sobre-sellado">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <rect x="3" y="5.5" width="18" height="13" rx="2" />
                  <path d="m3.5 7 8.5 6.5L20.5 7" />
                </svg>
                <span>
                  <strong>Sobre sellado</strong>
                  <span className="tenue">
                    Se abre {hora(s.abreEn)} · {cuentaAtras(s.abreEn)}
                  </span>
                </span>
              </article>
            ))}
          </div>
        </>
      )}

      {visiones.length > 0 && (
        <>
          <h2>Visiones</h2>
          <p className="tenue">Imágenes que te manda la víctima. No dicen nada: interprétalas tú.</p>
          <div className="visiones">
            {visiones.map((v) => (
              <button key={v.id} type="button" className={`vision ${nuevas.has(`v${v.id}`) ? 'nueva' : ''}`} onClick={() => setVision(v)} aria-label={`Visión recibida ${hora(v.recibidaEn)}`}>
                <img src={v.imagenUrl} alt="" loading="lazy" referrerPolicy="no-referrer" />
                <span>{hora(v.recibidaEn)}</span>
              </button>
            ))}
          </div>
        </>
      )}

      <h2>Pistas descubiertas</h2>
      {datos.pistas.length === 0 && <Vacio>Aún no has descubierto ninguna pista.</Vacio>}
      {[...datos.pistas].reverse().map((p) => (
        <article key={p.id} className={`tarjeta ${nuevas.has(`p${p.id}`) ? 'nuevo' : ''}`}>
          <h3>
            {p.titulo}
            {nuevas.has(`p${p.id}`) && <span className="etiqueta principal">Nueva</span>}
          </h3>
          <p>{p.contenido}</p>
        </article>
      ))}
      {(datos.tienda?.length ?? 0) > 0 && (
        <p className="tenue">Hay {datos.tienda.length} pistas a la venta en la pestaña Dinero.</p>
      )}

      <h2>Tus secretos</h2>
      {datos.esAsesino && <TarjetaAsesino datos={datos} otros={otros} accion={accion} />}
      {datos.secretos.length === 0 && !datos.esAsesino && <Vacio>No tienes secretos asignados.</Vacio>}
      {datos.secretos.map((s) => (
        <article key={s.id} className="tarjeta">
          <p>{s.texto}</p>
          {s.revelado && <span className="etiqueta fallido">Revelado al grupo</span>}
        </article>
      ))}

      <h2>Secretos revelados</h2>
      {datos.secretosRevelados.length === 0 && <Vacio>Nadie ha quedado al descubierto todavía.</Vacio>}
      {datos.secretosRevelados.map((s, i) => (
        <article key={i} className="tarjeta">
          <h3>{s.jugador}</h3>
          <p>{s.texto}</p>
        </article>
      ))}

      {vision && (
        <Visor alCerrar={() => setVision(null)} etiqueta="Visión">
          <img className="visor-imagen" src={vision.imagenUrl} alt="Visión de la víctima" referrerPolicy="no-referrer" />
          <p className="visor-pie">Recibida {hora(vision.recibidaEn)}</p>
        </Visor>
      )}
    </section>
  )
}

/** Para los QR escondidos por la casa: con la cámara se abre solo; si no, se teclea el código. */
function CodigoQr({ alCanjear }) {
  const [codigo, setCodigo] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function canjear(evento) {
    evento.preventDefault()
    setEnviando(true)
    if (await alCanjear(codigo)) setCodigo('')
    setEnviando(false)
  }

  return (
    <form className="codigo-qr" onSubmit={canjear}>
      <label htmlFor="codigo-qr">¿Has encontrado un QR? Escanéalo con la cámara o escribe su código.</label>
      <span className="fila-imagen">
        <input
          id="codigo-qr"
          value={codigo}
          onChange={(e) => setCodigo(e.target.value.toUpperCase())}
          placeholder="Código"
          autoCapitalize="characters"
          autoCorrect="off"
          spellCheck={false}
          maxLength={20}
        />
        <button className="primario" disabled={enviando || codigo.trim().length < 4}>
          Abrir
        </button>
      </span>
    </form>
  )
}

/** Solo para el asesino: cómplices (si los hay), la elección del arma y las pistas falsas. */
function TarjetaAsesino({ datos, otros, accion }) {
  const a = datos.asesinato ?? { complices: [], armaDeId: null, armaBloqueada: false, pistasFalsas: [] }
  const [error, setError] = useState(null)
  const [guardando, setGuardando] = useState(false)
  // Opciones: mi herramienta y la de cada otro jugador que tenga una (el Máster no cuenta)
  const opciones = [
    datos.personaje.herramienta && { id: datos.personaje.id, nombre: null, herramienta: datos.personaje.herramienta },
    ...otros.filter((o) => !o.esMaster && o.herramienta).map((o) => ({ id: o.id, nombre: o.nombre, herramienta: o.herramienta })),
  ].filter(Boolean)
  const valorActual = a.armaDeId == null ? '' : String(a.armaDeId)
  const elegida = opciones.find((o) => String(o.id) === valorActual)

  async function elegir(valor) {
    setError(null)
    setGuardando(true)
    try {
      await accion('PUT', '/yo/arma', { jugadorId: valor === '' ? null : Number(valor) })
    } catch (e) {
      setError(e.message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <article className="tarjeta asesino">
      <h3>Eres el asesino</h3>
      {a.complices.length > 0 ? (
        <p>
          No estás solo: tu cómplice es <strong>{a.complices.join(' y ')}</strong>. Nadie más lo sabe.
        </p>
      ) : (
        <p>Solo tú y el Máster lo sabéis.</p>
      )}

      <h3 className="asesino-arma">Tu arma</h3>
      {error && <p className="error">{error}</p>}
      {a.armaBloqueada ? (
        <p>{elegida ? `Usaste ${elegida.herramienta.toLowerCase()}${elegida.nombre ? `, la herramienta de ${elegida.nombre}` : ''}.` : 'No elegiste arma a tiempo.'}</p>
      ) : (
        <>
          <p className="tenue">
            Usa la herramienta de tu oficio o la de otro personaje para que las sospechas caigan sobre él. Puedes cambiarla hasta que empiece la investigación.
          </p>
          <select value={valorActual} onChange={(e) => elegir(e.target.value)} disabled={guardando} aria-label="Arma">
            <option value="">Sin elegir</option>
            {opciones.map((o) => (
              <option key={o.id} value={String(o.id)}>
                {o.nombre ? `${o.herramienta} (la de ${o.nombre}: le incrimina)` : `${o.herramienta} (la tuya)`}
              </option>
            ))}
          </select>
        </>
      )}

      <PistasFalsas asesinato={a} saldo={datos.dinero ?? 0} otros={otros.filter((o) => !o.esMaster)} accion={accion} />
    </article>
  )
}

const ESTADO_FALSA = {
  PENDIENTE: ['entregado', 'El Máster la está revisando'],
  APROBADA: ['cumplido', 'Colada en la partida'],
  RECHAZADA: ['fallido', 'Rechazada: te devolvió el dinero'],
}

/** El asesino paga por inventarse una pista; si el Máster la aprueba, nadie sabrá que es falsa. */
function PistasFalsas({ asesinato: a, saldo, otros, accion }) {
  const precio = a.precioPistaFalsa ?? 200
  const [abierta, setAbierta] = useState(false)
  const [titulo, setTitulo] = useState('')
  const [contenido, setContenido] = useState('')
  const [paraId, setParaId] = useState('')
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function encargar() {
    setError(null)
    setEnviando(true)
    try {
      await accion('POST', '/yo/pistas-falsas', { titulo, contenido, paraId: paraId ? Number(paraId) : null })
      setTitulo('')
      setContenido('')
      setParaId('')
      setAbierta(false)
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
      setConfirmando(false)
    }
  }

  return (
    <>
      <h3 className="asesino-arma">Pistas falsas</h3>
      <p className="tenue">
        Invéntate una pista y el Máster la colará como si fuera de verdad: en la tienda o directamente en el móvil de quien elijas. Cuesta {precio} monedas.
      </p>
      {(a.pistasFalsas ?? []).map((f) => (
        <div key={f.id} className="falsa">
          <p>
            <strong>{f.titulo}</strong>
            <span className={`etiqueta ${ESTADO_FALSA[f.estado][0]}`}>{ESTADO_FALSA[f.estado][1]}</span>
          </p>
          <p className="tenue">{f.para ? `Para ${f.para}` : 'Para la tienda'}</p>
        </div>
      ))}
      {error && <p className="error">{error}</p>}
      {!abierta ? (
        <button type="button" className="ancho" disabled={saldo < precio} onClick={() => setAbierta(true)}>
          {saldo < precio ? `Necesitas ${precio} monedas` : 'Falsificar una pista'}
        </button>
      ) : (
        <div className="falsa-formulario">
          <input value={titulo} onChange={(e) => setTitulo(e.target.value)} maxLength={200} placeholder="Título (como el de una pista de verdad)" aria-label="Título" disabled={confirmando} />
          <textarea rows={3} value={contenido} onChange={(e) => setContenido(e.target.value)} maxLength={4000} placeholder="Lo que dice la pista" aria-label="Contenido" disabled={confirmando} />
          <select value={paraId} onChange={(e) => setParaId(e.target.value)} aria-label="Dónde la cuelas" disabled={confirmando}>
            <option value="">En la tienda</option>
            {otros.map((o) => (
              <option key={o.id} value={o.id}>
                En el móvil de {o.nombre}
              </option>
            ))}
          </select>
          {confirmando ? (
            <div className="botonera">
              <button type="button" className="primario" onClick={encargar} disabled={enviando}>
                Pagar {precio} y encargarla
              </button>
              <button type="button" onClick={() => setConfirmando(false)}>
                No
              </button>
            </div>
          ) : (
            <div className="botonera">
              <button type="button" className="primario" disabled={!titulo.trim() || !contenido.trim()} onClick={() => setConfirmando(true)}>
                Encargar ({precio})
              </button>
              <button type="button" onClick={() => setAbierta(false)}>
                Cancelar
              </button>
            </div>
          )}
        </div>
      )}
    </>
  )
}

// ---------------------------------------------------------------- Dinero

function Dinero({ datos, otros, accion, recargar }) {
  const [paraId, setParaId] = useState('')
  const [cantidad, setCantidad] = useState('')
  const [concepto, setConcepto] = useState('')
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [comprando, setComprando] = useState(null) // id de la pista pendiente de confirmar
  const saldo = datos.dinero ?? 0
  const tienda = datos.tienda ?? []
  const movimientos = datos.movimientos ?? []
  const n = Number(cantidad)
  const valido = paraId && Number.isInteger(n) && n > 0 && n <= saldo

  async function pagar(evento) {
    evento.preventDefault()
    setError(null)
    setOk(null)
    setEnviando(true)
    try {
      await accion('POST', '/yo/pagos', { jugadorId: Number(paraId), cantidad: n, concepto })
      const nombre = otros.find((o) => o.id === Number(paraId))?.nombre
      setOk(`Has pagado ${n} monedas a ${nombre}.`)
      setParaId('')
      setCantidad('')
      setConcepto('')
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  async function comprar(pista) {
    setError(null)
    setOk(null)
    try {
      await accion('POST', `/yo/tienda/${pista.id}`)
      setOk(`Has comprado «${pista.titulo}». La tienes en Pistas.`)
    } catch (e) {
      setError(e.message)
    } finally {
      setComprando(null)
    }
  }

  return (
    <section>
      <div className="monedero">
        <span className="tenue">Tu dinero</span>
        <strong>
          {saldo} <Moneda />
        </strong>
      </div>

      {error && <p className="error redondeado">{error}</p>}
      {ok && <p className="aviso-ok">{ok}</p>}

      <Tratos datos={datos} otros={otros} recargar={recargar} />

      <h2>Tienda del Máster</h2>
      {tienda.length === 0 ? (
        <Vacio>Ahora mismo no hay pistas a la venta.</Vacio>
      ) : (
        <details className="historial tienda" open={tienda.length <= 6}>
          <summary>
            {tienda.length} pistas a la venta · solo ves el título
          </summary>
          {tienda.map((p) => (
            <article key={p.id} className="tarjeta producto">
              <div>
                <h3>{p.titulo}</h3>
                <span className="tenue">
                  {p.precio} monedas{p.precio > saldo ? ' · no te llega' : ''}
                </span>
              </div>
              {comprando === p.id ? (
                <span className="botonera">
                  <button className="primario" onClick={() => comprar(p)}>
                    Confirmar
                  </button>
                  <button onClick={() => setComprando(null)}>No</button>
                </span>
              ) : (
                <button className="primario" disabled={p.precio > saldo} onClick={() => setComprando(p.id)}>
                  Comprar
                </button>
              )}
            </article>
          ))}
        </details>
      )}

      <h2>Pagar a alguien</h2>
      <form onSubmit={pagar} className="tarjeta">
        <select value={paraId} onChange={(e) => setParaId(e.target.value)} aria-label="A quién">
          <option value="">¿A quién?</option>
          {otros.map((o) => (
            <option key={o.id} value={o.id}>
              {o.nombre}
              {o.esMaster ? ' (Máster)' : ''}
            </option>
          ))}
        </select>
        <input
          type="number"
          inputMode="numeric"
          min="1"
          max={saldo}
          value={cantidad}
          onChange={(e) => setCantidad(e.target.value)}
          placeholder="Cantidad"
          aria-label="Cantidad"
        />
        <input value={concepto} onChange={(e) => setConcepto(e.target.value)} placeholder="Concepto (opcional, lo verá quien lo reciba)" maxLength={300} aria-label="Concepto" />
        <button className="primario" disabled={!valido || enviando}>
          {valido ? `Pagar ${n} monedas` : 'Pagar'}
        </button>
      </form>

      <h2>Movimientos</h2>
      {movimientos.length === 0 && <Vacio>Todavía no has movido dinero.</Vacio>}
      <ul className="movimientos">
        {movimientos.map((m) => {
          const entra = m.paraId === datos.personaje.id
          return (
            <li key={m.id}>
              <span className={`importe ${entra ? 'entra' : 'sale'}`}>
                {entra ? '+' : '−'}
                {m.cantidad}
              </span>
              <span className="movimiento-texto">
                <span>{entra ? `De ${m.de ?? 'la banca'}` : `A ${m.para ?? 'la banca'}`}</span>
                {m.concepto && <span className="tenue">{m.concepto}</span>}
              </span>
              <span className="tenue">{hora(m.creadoEn)}</span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function Moneda() {
  return (
    <svg className="moneda" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v10M9.5 9.5c0-1 1-1.6 2.5-1.6s2.5.7 2.5 1.7c0 2.4-5 1.4-5 4 0 1 1 1.7 2.5 1.7s2.5-.6 2.5-1.6" />
    </svg>
  )
}

// ---------------------------------------------------------------- Piezas comunes

function Basicos({ personaje: p }) {
  const datos = [p.edad && `${p.edad} años`, p.profesion, p.pareja && `Pareja: ${p.pareja}`].filter(Boolean)
  if (datos.length === 0) return null
  return <p className="tenue">{datos.join(' · ')}</p>
}

function Bloque({ titulo, texto }) {
  if (!texto) return null
  return (
    <article className="tarjeta">
      <h3>{titulo}</h3>
      <p>{texto}</p>
    </article>
  )
}

function Vacio({ children }) {
  return <p className="vacio">{children}</p>
}
