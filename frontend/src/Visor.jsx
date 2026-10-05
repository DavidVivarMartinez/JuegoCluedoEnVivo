import { useEffect, useRef } from 'react'

/**
 * Pantalla completa para una visión (o cualquier contenido que deba verse solo).
 * Se cierra tocando fuera, con la equis o con Escape.
 */
export default function Visor({ children, alCerrar, etiqueta }) {
  const cerrar = useRef(alCerrar)
  cerrar.current = alCerrar

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

  return (
    <div className="visor" role="dialog" aria-modal="true" aria-label={etiqueta} onClick={alCerrar}>
      <button type="button" className="visor-cerrar" onClick={alCerrar} aria-label="Cerrar">
        ×
      </button>
      <div className="visor-contenido" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  )
}
