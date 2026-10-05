import { useState } from 'react'
import { api, subirArchivo } from './api.js'
import { leerPartidaMd } from './partidaMd.js'
import { importarPartida } from './importarPartida.js'

/**
 * Carga la partida desde personajes.md: se elige el fichero, se ve qué contiene y se importa.
 * Se puede repetir cuantas veces haga falta (crea lo nuevo y actualiza lo cambiado).
 * Las imágenes que el fichero nombra por ruta ("visiones/fer-1.jpg") se eligen aparte y se
 * suben solas; se emparejan por nombre de fichero.
 */
export default function ImportarMd({ recargar }) {
  const [leido, setLeido] = useState(null)
  const [imagenes, setImagenes] = useState(() => new Map()) // nombre de fichero → File
  const [registro, setRegistro] = useState([])
  const [estado, setEstado] = useState('inicio') // inicio | leido | importando | hecho | error
  const [error, setError] = useState(null)

  async function elegir(evento) {
    const fichero = evento.target.files?.[0]
    evento.target.value = ''
    if (!fichero) return
    setError(null)
    setRegistro([])
    try {
      const datos = leerPartidaMd(await fichero.text())
      if (datos.personajes.length === 0 && datos.pistas.length === 0) throw new Error('No parece un personajes.md: no hay personajes ni pistas.')
      setLeido({ nombre: fichero.name, datos })
      setEstado('leido')
    } catch (e) {
      setError(e.message)
      setEstado('error')
    }
  }

  function elegirImagenes(evento) {
    const lista = [...(evento.target.files ?? [])].filter((f) => f.type.startsWith('image/'))
    evento.target.value = ''
    setImagenes((previas) => new Map([...previas, ...lista.map((f) => [f.name, f])]))
  }

  async function importar() {
    setEstado('importando')
    setError(null)
    const lineas = []
    try {
      await importarPartida(leido.datos, (metodo, ruta, cuerpo) => api(metodo, `/master${ruta}`, cuerpo), (linea) => {
        lineas.push(linea)
        setRegistro([...lineas])
      }, {
        leerImagen: async (ruta) => imagenes.get(ruta.replace(/\\/g, '/').split('/').pop()) ?? null,
        subirImagen: async (nombre, archivo) => (await subirArchivo('/master/imagenes', archivo, nombre)).url,
      })
      setEstado('hecho')
    } catch (e) {
      setError(e.message)
      setEstado('error')
    } finally {
      recargar()
    }
  }

  const d = leido?.datos
  // Rutas locales que pide el fichero (las https ya son enlaces y no hay que subirlas)
  const locales = !d ? [] : [
    ...d.personajes.map((p) => p.imagenUrl),
    ...d.personajes.flatMap((p) => p.visiones.map((v) => v.imagenUrl)),
    ...d.visionesReserva.map((v) => v.imagenUrl),
  ].filter((r) => r && !/^https?:|^\/api\//i.test(r))
  const encontradas = locales.filter((r) => imagenes.has(r.split('/').pop())).length
  const cuenta = d && {
    personajes: d.personajes.length,
    pistas: d.pistas.length,
    qr: d.pistas.filter((p) => p.qr).length,
    habilidades: d.personajes.reduce((n, p) => n + p.habilidades.length, 0),
    sobres: d.personajes.reduce((n, p) => n + p.sobres.length, 0),
    visiones: d.personajes.reduce((n, p) => n + p.visiones.length, 0) + d.visionesReserva.length,
  }

  return (
    <div className="importar">
      <label className="boton-fichero">
        {leido ? 'Elegir otro fichero' : 'Elegir personajes.md'}
        <input type="file" accept=".md,.markdown,.txt,text/markdown,text/plain" onChange={elegir} />
      </label>
      {error && <p className="error redondeado">{error}</p>}
      {leido && (
        <div className="tarjeta">
          <p>
            <strong>{leido.nombre}</strong>
          </p>
          <p className="tenue">
            {cuenta.personajes} personajes · {cuenta.pistas} pistas ({cuenta.qr} con QR) · {cuenta.habilidades} habilidades · {cuenta.sobres} sobres · {cuenta.visiones} visiones
            {d.partida.lugares ? ` · ${d.partida.lugares.length} estancias` : ''}
          </p>
          {locales.length > 0 && (
            <>
              <label className="boton-fichero secundario">
                {imagenes.size ? `Añadir más imágenes (${imagenes.size} elegidas)` : 'Elegir las imágenes (o la carpeta visiones)'}
                <input type="file" accept="image/*" multiple onChange={elegirImagenes} />
              </label>
              <p className="tenue">
                El fichero usa {locales.length} imágenes; tienes elegidas {encontradas}. Las ya subidas antes no hace falta volver a elegirlas.
              </p>
            </>
          )}
          {d.avisos.length > 0 && (
            <ul className="avisos-importar">
              {d.avisos.map((a) => (
                <li key={a}>{a}</li>
              ))}
            </ul>
          )}
          {estado !== 'hecho' && (
            <button className="primario ancho" onClick={importar} disabled={estado === 'importando'}>
              {estado === 'importando' ? 'Importando…' : 'Importar a la partida'}
            </button>
          )}
          {registro.length > 0 && (
            <ul className="registro-importar">
              {registro.map((l, i) => (
                <li key={i}>{l}</li>
              ))}
            </ul>
          )}
          {estado === 'hecho' && <p className="aviso-ok">Partida actualizada.</p>}
        </div>
      )}
    </div>
  )
}
