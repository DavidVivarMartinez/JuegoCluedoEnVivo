import { useEffect, useState } from 'react'
import { api, leerCodigoInicial, usarCodigo } from './api.js'
import Acceso from './Acceso.jsx'
import PanelJugador from './PanelJugador.jsx'
import PanelMaster from './PanelMaster.jsx'

export default function App() {
  const [sesion, setSesion] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  async function entrar(codigo) {
    const limpio = codigo.trim().toUpperCase()
    setError(null)
    try {
      const info = await api('POST', '/sesion', { codigo: limpio })
      usarCodigo(limpio)
      setSesion(info)
    } catch (e) {
      // Solo olvidamos el código si el servidor lo rechaza, no si falla la red.
      if (e.estado === 401) usarCodigo(null)
      setError(e.message)
    }
  }

  function salir() {
    usarCodigo(null)
    setSesion(null)
  }

  useEffect(() => {
    const inicial = leerCodigoInicial()
    if (!inicial) {
      setCargando(false)
      return
    }
    entrar(inicial).finally(() => setCargando(false))
  }, [])

  if (cargando) return <p className="centrado">Cargando…</p>
  if (!sesion) return <Acceso alEntrar={entrar} error={error} />
  return sesion.rol === 'MASTER' ? (
    <PanelMaster sesion={sesion} alSalir={salir} />
  ) : (
    <PanelJugador sesion={sesion} alSalir={salir} />
  )
}
