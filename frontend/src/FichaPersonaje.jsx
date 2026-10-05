import { useEffect, useRef, useState } from 'react'
import Nota from './Nota.jsx'
import { colorDe, iniciales } from './Avatar.jsx'

/**
 * Ficha de otro personaje a pantalla completa: foto grande, datos públicos, lo que
 * se le haya revelado y tu cuaderno. Aquí irán también las acciones sobre él
 * (mensajes entre personajes, etc.) cuando existan.
 */
export default function FichaPersonaje({ personaje, secretosRevelados, alGuardar, alCerrar }) {
  const [fallo, setFallo] = useState(false)
  const cerrar = useRef(alCerrar)
  cerrar.current = alCerrar

  // Mientras está abierta, la página de detrás no se desplaza y Escape la cierra.
  useEffect(() => {
    const alTecla = (evento) => {
      if (evento.key === 'Escape') cerrar.current()
    }
    const anterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', alTecla)
    return () => {
      document.body.style.overflow = anterior
      document.removeEventListener('keydown', alTecla)
    }
  }, [])

  const datos = [personaje.edad && `${personaje.edad} años`, personaje.profesion, personaje.pareja && `Pareja: ${personaje.pareja}`].filter(Boolean)

  return (
    <div className="ficha-personaje" role="dialog" aria-modal="true" aria-label={`Ficha de ${personaje.nombre}`}>
      <div className="ficha-cabecera">
        {personaje.imagenUrl && !fallo ? (
          <img src={personaje.imagenUrl} alt="" referrerPolicy="no-referrer" onError={() => setFallo(true)} />
        ) : (
          <div className="ficha-iniciales" style={{ backgroundColor: colorDe(personaje.nombre) }} aria-hidden="true">
            {iniciales(personaje.nombre)}
          </div>
        )}
        <button type="button" className="ficha-volver" onClick={alCerrar} aria-label="Volver">
          ‹
        </button>
        <div className="ficha-titulo">
          <h2>{personaje.nombre}</h2>
          {datos.length > 0 && <p>{datos.join(' · ')}</p>}
          {personaje.esMaster && <span className="etiqueta">Máster</span>}
        </div>
      </div>

      <div className="ficha-cuerpo">
        {personaje.herramienta && (
          <>
            <h2>Herramienta de su oficio</h2>
            <article className="tarjeta">
              <p>{personaje.herramienta}</p>
            </article>
          </>
        )}
        {secretosRevelados.length > 0 && (
          <>
            <h2>Al descubierto</h2>
            {secretosRevelados.map((s, i) => (
              <article key={i} className="tarjeta">
                <p>{s.texto}</p>
              </article>
            ))}
          </>
        )}

        <h2>Tu cuaderno</h2>
        <p className="tenue">Solo lo ves tú. Se guarda solo.</p>
        <Nota personaje={personaje} alGuardar={alGuardar} />
      </div>
    </div>
  )
}
