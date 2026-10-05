// Cliente de la API. El código de acceso identifica al participante en cada petición.
const CLAVE = 'misterio.codigo'
const CLAVE_QR = 'misterio.qr'

let qrEnMemoria = null

/** Código de QR escaneado pendiente de canjear (y lo olvida). */
export function tomarQrPendiente() {
  let qr = qrEnMemoria
  qrEnMemoria = null
  try {
    qr = qr || window.sessionStorage.getItem(CLAVE_QR)
    window.sessionStorage.removeItem(CLAVE_QR)
  } catch {
    // Sin almacenamiento: nos quedamos con el de memoria.
  }
  return qr
}

let codigo = null

export function leerCodigoInicial() {
  const parametros = new URLSearchParams(window.location.search)
  // Un QR de la casa lleva ?qr=CODIGO: se guarda hasta que el jugador esté dentro.
  const qr = parametros.get('qr')
  if (qr) {
    try {
      window.sessionStorage.setItem(CLAVE_QR, qr)
    } catch {
      qrEnMemoria = qr
    }
    window.history.replaceState({}, '', window.location.pathname)
  }
  const deUrl = parametros.get('codigo')
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

/** Sube un fichero (campo "archivo") con el código de acceso; devuelve la respuesta JSON. */
export async function subirArchivo(ruta, archivo, nombre) {
  const formulario = new FormData()
  formulario.append('archivo', archivo, nombre)
  let respuesta
  try {
    respuesta = await fetch(`/api${ruta}`, { method: 'POST', headers: codigo ? { Authorization: `Bearer ${codigo}` } : {}, body: formulario })
  } catch {
    throw new ErrorApi(0, 'Sin conexión con el servidor')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) throw new ErrorApi(respuesta.status, (datos && datos.message) || `Error ${respuesta.status}`)
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

/** "dentro de 2 h 10 min" / "en 5 min" hasta un instante futuro. */
export function cuentaAtras(instante, ahora = Date.now()) {
  const minutos = Math.max(0, Math.round((new Date(instante).getTime() - ahora) / 60000))
  if (minutos < 1) return 'ya mismo'
  if (minutos < 60) return `en ${minutos} min`
  const horas = Math.floor(minutos / 60)
  const resto = minutos % 60
  if (horas < 24) return `en ${horas} h${resto ? ` ${resto} min` : ''}`
  return `en ${Math.round(horas / 24)} días`
}

/** Enlace que abre la app y canjea un QR. */
export function enlaceQr(codigo) {
  return `${window.location.origin}/?qr=${codigo}`
}
