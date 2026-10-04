import { useState } from 'react'
import { api, FASES, hora } from './api.js'
import { useSondeo } from './useSondeo.js'

const PESTANAS = [
  ['partida', 'Partida'],
  ['jugadores', 'Jugadores'],
  ['pistas', 'Pistas'],
  ['mensajes', 'Mensajes'],
]

const CAMPOS_FICHA = [
  ['profesion', 'Profesión', false],
  ['pareja', 'Pareja', false],
  ['personalidad', 'Personalidad', true],
  ['relaciones', 'Relaciones', true],
  ['contexto', 'Contexto', true],
  ['loQueSabeMaster', 'Lo que sabe el Máster (privado)', true],
  ['motivoPotencial', 'Motivo potencial (privado)', true],
]

export default function PanelMaster({ sesion, alSalir }) {
  const { datos, error, recargar } = useSondeo('/master/estado', 6000, alSalir)
  const [pestana, setPestana] = useState('partida')
  const [aviso, setAviso] = useState(null)

  if (!datos) return <p className="centrado">{error || 'Cargando…'}</p>

  // Ejecuta una acción del Máster, muestra el error si falla y refresca el estado.
  async function hacer(metodo, ruta, cuerpo) {
    setAviso(null)
    try {
      await api(metodo, `/master${ruta}`, cuerpo)
      return true
    } catch (e) {
      setAviso(e.message)
      return false
    } finally {
      recargar()
    }
  }

  return (
    <div className="app master">
      <header className="cabecera">
        <div>
          <h1>{datos.partida.nombre}</h1>
          <p className="tenue">
            Máster · {sesion.nombre}
            {datos.partida.casa ? ` · ${datos.partida.casa}` : ''}
          </p>
        </div>
        <span className="fase">{FASES[datos.partida.fase]}</span>
      </header>

      {(aviso || error) && <p className="error">{aviso || error}</p>}

      <main className="contenido">
        {pestana === 'partida' && <Partida datos={datos} hacer={hacer} alSalir={alSalir} />}
        {pestana === 'jugadores' && <Jugadores datos={datos} hacer={hacer} />}
        {pestana === 'pistas' && <Pistas datos={datos} hacer={hacer} />}
        {pestana === 'mensajes' && <Mensajes datos={datos} hacer={hacer} />}
      </main>

      <nav className="pestanas" aria-label="Secciones">
        {PESTANAS.map(([clave, titulo]) => (
          <button key={clave} className={pestana === clave ? 'activa' : ''} onClick={() => setPestana(clave)}>
            {titulo}
          </button>
        ))}
      </nav>
    </div>
  )
}

// ---------------------------------------------------------------- Partida

function Partida({ datos, hacer, alSalir }) {
  const [clave, setClave] = useState('')
  const [valor, setValor] = useState('')

  async function guardarVariable(evento) {
    evento.preventDefault()
    if (!clave.trim()) return
    if (await hacer('PUT', `/variables/${encodeURIComponent(clave.trim())}`, { valor })) {
      setClave('')
      setValor('')
    }
  }

  return (
    <section>
      <h2>Fase</h2>
      <div className="botonera">
        {Object.entries(FASES).map(([fase, titulo]) => (
          <button
            key={fase}
            className={datos.partida.fase === fase ? 'primario' : ''}
            onClick={() => hacer('PUT', '/fase', { fase })}
          >
            {titulo}
          </button>
        ))}
      </div>

      <h2>Asesino activo</h2>
      <select
        value={datos.asesinoId ?? ''}
        onChange={(e) => hacer('PUT', '/asesino', { jugadorId: e.target.value ? Number(e.target.value) : null })}
        aria-label="Asesino activo"
      >
        <option value="">Sin decidir</option>
        {datos.jugadores.map((j) => (
          <option key={j.id} value={j.id}>
            {j.nombre}
          </option>
        ))}
      </select>
      <p className="tenue">La persona elegida lo verá en su pestaña de secretos. Nadie más.</p>

      <h2>Variables narrativas</h2>
      {datos.variables.map((v) => (
        <article key={v.clave} className="tarjeta fila">
          <p>
            <strong>{v.clave}</strong> = {v.valor}
          </p>
          <button className="enlace" onClick={() => hacer('DELETE', `/variables/${encodeURIComponent(v.clave)}`)}>
            Borrar
          </button>
        </article>
      ))}
      <form className="fila" onSubmit={guardarVariable}>
        <input value={clave} onChange={(e) => setClave(e.target.value)} placeholder="clave" aria-label="Clave" />
        <input value={valor} onChange={(e) => setValor(e.target.value)} placeholder="valor" aria-label="Valor" />
        <button>Guardar</button>
      </form>

      <h2>Registro</h2>
      {datos.eventos.map((e) => (
        <p key={e.id} className="registro">
          <span className="tenue">{hora(e.creadoEn)}</span> {e.descripcion}
        </p>
      ))}

      <button className="enlace salir" onClick={alSalir}>
        Salir
      </button>
    </section>
  )
}

// ---------------------------------------------------------------- Jugadores

function Jugadores({ datos, hacer }) {
  const [nombre, setNombre] = useState('')
  const [abierto, setAbierto] = useState(null)

  async function crear(evento) {
    evento.preventDefault()
    if (!nombre.trim()) return
    if (await hacer('POST', '/jugadores', { nombre })) setNombre('')
  }

  return (
    <section>
      <h2>Jugadores ({datos.jugadores.length})</h2>
      {datos.jugadores.map((j) => (
        <article key={j.id} className="tarjeta">
          <button className="plegable" onClick={() => setAbierto(abierto === j.id ? null : j.id)}>
            <span>
              {j.nombre}
              {datos.asesinoId === j.id && <span className="etiqueta fallido">Asesino</span>}
            </span>
            <span className="tenue">
              {j.secretos.length} secretos · {j.objetivos.length} objetivos
              {j.mensajesSinLeer > 0 ? ` · ${j.mensajesSinLeer} sin leer` : ''}
            </span>
          </button>
          {abierto === j.id && <Ficha key={j.id} jugador={j} hacer={hacer} />}
        </article>
      ))}

      <form className="fila" onSubmit={crear}>
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre del nuevo jugador" aria-label="Nombre del nuevo jugador" />
        <button>Añadir</button>
      </form>
    </section>
  )
}

function Ficha({ jugador, hacer }) {
  const [ficha, setFicha] = useState({
    nombre: jugador.nombre,
    edad: jugador.edad ?? '',
    ...Object.fromEntries(CAMPOS_FICHA.map(([campo]) => [campo, jugador[campo] ?? ''])),
  })
  const [guardado, setGuardado] = useState(false)
  const [copiado, setCopiado] = useState(false)
  const enlace = `${window.location.origin}/?codigo=${jugador.codigoAcceso}`

  function cambiar(campo, valor) {
    setFicha({ ...ficha, [campo]: valor })
    setGuardado(false)
  }

  async function guardar(evento) {
    evento.preventDefault()
    const ok = await hacer('PUT', `/jugadores/${jugador.id}`, {
      ...ficha,
      edad: ficha.edad === '' ? null : Number(ficha.edad),
    })
    setGuardado(ok)
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(enlace)
      setCopiado(true)
    } catch {
      setCopiado(false)
    }
  }

  function borrar() {
    hacer('DELETE', `/jugadores/${jugador.id}`)
  }

  return (
    <div className="ficha">
      <p>
        Código: <code>{jugador.codigoAcceso}</code>{' '}
        <button className="enlace" onClick={copiar}>
          {copiado ? 'Enlace copiado' : 'Copiar enlace'}
        </button>
      </p>

      <form onSubmit={guardar}>
        <label>
          Nombre
          <input value={ficha.nombre} onChange={(e) => cambiar('nombre', e.target.value)} />
        </label>
        <label>
          Edad
          <input type="number" min="0" value={ficha.edad} onChange={(e) => cambiar('edad', e.target.value)} />
        </label>
        {CAMPOS_FICHA.map(([campo, titulo, largo]) => (
          <label key={campo}>
            {titulo}
            {largo ? (
              <textarea rows={3} value={ficha[campo]} onChange={(e) => cambiar(campo, e.target.value)} />
            ) : (
              <input value={ficha[campo]} onChange={(e) => cambiar(campo, e.target.value)} />
            )}
          </label>
        ))}
        <button className="primario">{guardado ? 'Ficha guardada' : 'Guardar ficha'}</button>
      </form>

      <Lista
        titulo="Secretos"
        elementos={jugador.secretos}
        alCrear={(texto) => hacer('POST', `/jugadores/${jugador.id}/secretos`, { texto })}
        alBorrar={(s) => hacer('DELETE', `/secretos/${s.id}`)}
        acciones={(s) => (
          <button className="enlace" onClick={() => hacer('PUT', `/secretos/${s.id}`, { revelado: !s.revelado })}>
            {s.revelado ? 'Ocultar' : 'Revelar al grupo'}
          </button>
        )}
        marca={(s) => s.revelado && <span className="etiqueta fallido">Revelado</span>}
      />

      <Lista
        titulo="Objetivos"
        elementos={jugador.objetivos}
        alCrear={(texto) => hacer('POST', `/jugadores/${jugador.id}/objetivos`, { texto })}
        alBorrar={(o) => hacer('DELETE', `/objetivos/${o.id}`)}
        acciones={(o) => (
          <select
            value={o.estado}
            onChange={(e) => hacer('PUT', `/objetivos/${o.id}`, { estado: e.target.value })}
            aria-label="Estado del objetivo"
          >
            <option value="ACTIVO">Activo</option>
            <option value="CUMPLIDO">Cumplido</option>
            <option value="FALLIDO">Fallido</option>
          </select>
        )}
      />

      <Confirmar texto="Eliminar jugador" pregunta="¿Eliminar y borrar todo lo suyo?" alConfirmar={borrar} />
    </div>
  )
}

function Lista({ titulo, elementos, alCrear, alBorrar, acciones, marca }) {
  const [texto, setTexto] = useState('')

  async function crear(evento) {
    evento.preventDefault()
    if (!texto.trim()) return
    if (await alCrear(texto)) setTexto('')
  }

  return (
    <div className="lista">
      <h3>{titulo}</h3>
      {elementos.map((e) => (
        <div key={e.id} className="elemento">
          <p>
            {e.texto} {marca && marca(e)}
          </p>
          <div className="fila">
            {acciones(e)}
            <button className="enlace" onClick={() => alBorrar(e)}>
              Borrar
            </button>
          </div>
        </div>
      ))}
      <form className="fila" onSubmit={crear}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder={`Añadir a ${titulo.toLowerCase()}`} aria-label={`Añadir a ${titulo.toLowerCase()}`} />
        <button>Añadir</button>
      </form>
    </div>
  )
}

function Confirmar({ texto, pregunta, alConfirmar }) {
  const [preguntando, setPreguntando] = useState(false)
  if (!preguntando) {
    return (
      <button className="enlace peligro" onClick={() => setPreguntando(true)}>
        {texto}
      </button>
    )
  }
  return (
    <p className="fila">
      <span>{pregunta}</span>
      <button className="enlace peligro" onClick={alConfirmar}>
        Sí, eliminar
      </button>
      <button className="enlace" onClick={() => setPreguntando(false)}>
        Cancelar
      </button>
    </p>
  )
}

// ---------------------------------------------------------------- Pistas

function Pistas({ datos, hacer }) {
  const [titulo, setTitulo] = useState('')
  const [contenido, setContenido] = useState('')

  async function crear(evento) {
    evento.preventDefault()
    if (!titulo.trim() || !contenido.trim()) return
    if (await hacer('POST', '/pistas', { titulo, contenido })) {
      setTitulo('')
      setContenido('')
    }
  }

  function alternar(pista, jugadorId) {
    const ids = pista.jugadorIds.includes(jugadorId)
      ? pista.jugadorIds.filter((id) => id !== jugadorId)
      : [...pista.jugadorIds, jugadorId]
    hacer('PUT', `/pistas/${pista.id}/visibilidad`, { jugadorIds: ids })
  }

  const todos = datos.jugadores.map((j) => j.id)

  return (
    <section>
      <h2>Pistas ({datos.pistas.length})</h2>
      {datos.pistas.map((p) => (
        <article key={p.id} className="tarjeta">
          <h3>{p.titulo}</h3>
          <p>{p.contenido}</p>
          <p className="tenue">
            {p.jugadorIds.length === 0 ? 'Bloqueada: nadie la ve' : `La ven ${p.jugadorIds.length} de ${todos.length}`}
          </p>
          <div className="casillas">
            {datos.jugadores.map((j) => (
              <label key={j.id}>
                <input type="checkbox" checked={p.jugadorIds.includes(j.id)} onChange={() => alternar(p, j.id)} />
                {j.nombre}
              </label>
            ))}
          </div>
          <div className="fila">
            <button className="enlace" onClick={() => hacer('PUT', `/pistas/${p.id}/visibilidad`, { jugadorIds: todos })}>
              Desbloquear a todos
            </button>
            <button className="enlace" onClick={() => hacer('PUT', `/pistas/${p.id}/visibilidad`, { jugadorIds: [] })}>
              Bloquear
            </button>
            <Confirmar texto="Borrar" pregunta="¿Borrar la pista?" alConfirmar={() => hacer('DELETE', `/pistas/${p.id}`)} />
          </div>
        </article>
      ))}

      <h2>Nueva pista</h2>
      <form onSubmit={crear}>
        <input value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="Título" aria-label="Título" />
        <textarea rows={3} value={contenido} onChange={(e) => setContenido(e.target.value)} placeholder="Contenido" aria-label="Contenido" />
        <button className="primario">Crear pista (bloqueada)</button>
      </form>
    </section>
  )
}

// ---------------------------------------------------------------- Mensajes

function Mensajes({ datos, hacer }) {
  const [texto, setTexto] = useState('')
  const [destinos, setDestinos] = useState([])

  function alternar(id) {
    setDestinos(destinos.includes(id) ? destinos.filter((d) => d !== id) : [...destinos, id])
  }

  async function enviar(evento) {
    evento.preventDefault()
    if (!texto.trim()) return
    if (await hacer('POST', '/mensajes', { texto, jugadorIds: destinos })) {
      setTexto('')
      setDestinos([])
    }
  }

  return (
    <section>
      <h2>Enviar mensaje</h2>
      <form onSubmit={enviar}>
        <textarea rows={3} value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Mensaje del Máster" aria-label="Mensaje" />
        <div className="casillas">
          {datos.jugadores.map((j) => (
            <label key={j.id}>
              <input type="checkbox" checked={destinos.includes(j.id)} onChange={() => alternar(j.id)} />
              {j.nombre}
            </label>
          ))}
        </div>
        <button className="primario">
          {destinos.length === 0 ? 'Enviar a todos' : `Enviar a ${destinos.length}`}
        </button>
      </form>

      <h2>Enviados</h2>
      {datos.mensajes.length === 0 && <p className="vacio">Todavía no has enviado mensajes.</p>}
      {datos.mensajes.map((m) => (
        <article key={m.id} className="tarjeta">
          <p>{m.texto}</p>
          <p className="tenue">
            {m.jugador} · {hora(m.enviadoEn)} · {m.leido ? 'Leído' : 'Sin leer'}
          </p>
        </article>
      ))}
    </section>
  )
}
