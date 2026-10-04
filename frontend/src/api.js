// Cliente de la API. El código de acceso identifica al participante en cada petición.
const CLAVE = 'misterio.codigo'

let codigo = null

export function leerCodigoInicial() {
  const deUrl = new URLSearchParams(window.location.search).get('codigo')
  if (deUrl) {
    // Quitamos el código de la barra de direcciones para que no quede a la vista.
    window.history.replaceState({}, '', window.location.pathname)
    return deUrl
  }
  try {
    return window.localStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

export function usarCodigo(nuevo) {
  codigo = nuevo
  try {
    if (nuevo) window.localStorage.setItem(CLAVE, nuevo)
    else window.localStorage.removeItem(CLAVE)
  } catch {
    // Sin almacenamiento (modo privado): la sesión dura lo que dure la pestaña.
  }
}

export async function api(metodo, ruta, cuerpo) {
  const cabeceras = {}
  if (codigo) cabeceras.Authorization = `Bearer ${codigo}`
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'

  let respuesta
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new ErrorApi(0, 'Sin conexión con el servidor')
  }

  const texto = await respuesta.text()
  let datos = null
  if (texto) {
    try {
      datos = JSON.parse(texto)
    } catch {
      datos = null
    }
  }
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, (datos && datos.message) || `Error ${respuesta.status}`)
  }
  return datos
}

export class ErrorApi extends Error {
  constructor(estado, mensaje) {
    super(mensaje)
    this.estado = estado
  }
}

export const FASES = {
  PREPARACION: 'Preparación',
  CONVIVENCIA: 'Convivencia',
  INVESTIGACION: 'Investigación',
  RESOLUCION: 'Resolución',
  FINALIZADA: 'Finalizada',
}

export function hora(instante) {
  return new Date(instante).toLocaleString('es-ES', {
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}
