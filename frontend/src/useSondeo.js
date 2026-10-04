import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './api.js'

/**
 * Carga una ruta y la vuelve a pedir cada pocos segundos (el documento base
 * prevé REST primero; SSE/WebSocket más adelante). Devuelve también "recargar"
 * para refrescar justo después de una acción.
 */
export function useSondeo(ruta, cadaMs, alPerderSesion) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const salir = useRef(alPerderSesion)
  salir.current = alPerderSesion

  const recargar = useCallback(async () => {
    try {
      setDatos(await api('GET', ruta))
      setError(null)
    } catch (e) {
      if (e.estado === 401 || e.estado === 403) salir.current()
      else setError(e.message)
    }
  }, [ruta])

  useEffect(() => {
    recargar()
    const temporizador = setInterval(() => {
      if (!document.hidden) recargar()
    }, cadaMs)
    const alVolver = () => {
      if (!document.hidden) recargar()
    }
    document.addEventListener('visibilitychange', alVolver)
    return () => {
      clearInterval(temporizador)
      document.removeEventListener('visibilitychange', alVolver)
    }
  }, [recargar, cadaMs])

  return { datos, error, recargar }
}
