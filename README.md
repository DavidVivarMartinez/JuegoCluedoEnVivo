# Misterio en Vivo

Motor para juegos de misterio presenciales en casas rurales: el grupo juega en la
casa y cada persona lleva en el móvil su personaje, sus secretos, sus pistas y los
mensajes del Máster.

La primera partida es **«La Última Noche»**, pero la aplicación no contiene esa
historia: cada partida (grupo + casa + fechas) es un conjunto de datos que el
Máster introduce y controla. La misma instalación sirve para tantas casas y
partidas como haga falta.

> **Regla del proyecto:** no se programa ninguna decisión narrativa. Asesino,
> secretos, pistas y fichas son datos de la partida, nunca código. Las fichas
> reales de los jugadores tampoco se suben a este repositorio (es público).

## Qué hay hecho (v0.1)

- **Partidas independientes**: un administrador crea una partida por casa/grupo y
  recibe el código del Máster. Nada de una partida es visible desde otra.
- **Acceso por código o enlace** (`/?codigo=XXXX`), sin contraseñas.
- **Panel del Máster**: fase de la partida, asesino activo, fichas de jugadores,
  secretos (ocultos o revelados al grupo), objetivos con validación de pruebas,
  pistas con desbloqueo por jugador, variables narrativas y registro de eventos.
  (La mensajería dentro de la app se retiró de la interfaz: el grupo habla por
  WhatsApp. La API de mensajes sigue existiendo por si hiciera falta.)
- **Dinero**: cada jugador tiene monedas; puede pagar a otros y comprar pistas en la
  tienda del Máster (solo ve título y precio). Los objetivos pueden pagar monedas.
- **Uno o varios asesinos**: el Máster los marca; si son varios se ven como cómplices.
  Cada personaje tiene una herramienta de oficio y el asesino elige la suya o la de
  otro para incriminarle; el Máster genera la pista del arma, que señala el oficio.
- **Objetivos con pruebas**: cada jugador tiene un objetivo principal y varios
  secundarios. Según su tipo, el jugador marca que lo ha conseguido, escribe una
  respuesta o elige a un personaje y cuenta qué dijo; el objetivo queda "pendiente
  de validar" y el Máster lo da por cumplido. Al cumplirlo se desbloquea la pista
  que el Máster le haya asociado.
- **Panel del jugador** (pensado para móvil): personaje, objetivos, pistas y
  secretos, tablón, dinero y cuaderno, y los demás personajes (el Máster incluido)
  en un carrusel con su foto. Al tocar uno se abre su ficha: datos públicos,
  secretos que se le hayan revelado y sus notas privadas. Solo recibe lo que puede
  saber: de los demás, únicamente nombre, edad, profesión, pareja y foto.
- **Cuaderno de deducción**: la hoja del Cluedo en el móvil (sospechosos × armas ×
  estancias de la casa, con ✕ ? ✓), notas por personaje y la **acusación final**
  (quién, con qué y por qué), que el Máster ve al momento y el grupo al terminar.
- **Tablón público**: acusaciones a la vista de todos, con la respuesta del acusado
  (que puede enseñar una de sus pistas), rumores anónimos y avisos del Máster.
- **QR por la casa**: el Máster activa un QR en cualquier pista, imprime la hoja y
  los esconde; quien lo escanea (o teclea su código) se queda con la pista.
- **Habilidades de oficio** de un solo uso: unas las resuelve el motor (ver las
  pistas o el dinero de alguien, rastrear pagos, pista gratis al azar, rumor
  anónimo) y otras las contesta el Máster desde su bandeja.
- **Tratos** entre jugadores: pista y/o monedas a cambio de una pista y/o monedas;
  solo se ejecutan si el otro acepta.
- **Pistas falsas**: el asesino paga por inventarse una; si el Máster la aprueba se
  cuela en la tienda o en el móvil de quien elija, sin marca de que sea falsa.
- **Envíos programados**: sobres (pistas) que se abren solos a su hora, con aviso
  de "sobre sellado" antes, y **visiones** de la víctima (una imagen sin texto),
  programadas o de reserva para mandarlas a mano.
- **Ranking** en directo para el Máster: aciertos de la acusación final, objetivos,
  pistas compradas (restan) y dinero, con su desglose.
- El backend es la única autoridad: el frontend no decide ni filtra nada.

## Estructura

```
backend/    Java 21 + Spring Boot 3 (API REST, reglas, permisos)
frontend/   React + Vite (panel de jugador y de Máster)
docker-compose.yml   MySQL para desarrollo
```

## Arrancar en local

### Para jugar o probar: Docker, sin ventanas abiertas

Con Docker Desktop en marcha:

```bash
docker compose up -d --build
```

Construye la imagen (backend + app) y la deja corriendo en segundo plano en
<http://localhost:8080>; desde un móvil de la misma wifi, `http://IP-DEL-PC:8080`.
Los datos se guardan en el volumen `misterio-datos` y sobreviven a reinicios.
`docker compose logs -f app` muestra el registro y `docker compose down` lo para.
Tras cambiar código, repite el `up -d --build`.

Todos los recursos Docker de este proyecto llevan el prefijo `misterio`, de modo que
no se cruzan con los contenedores de otros proyectos de la misma máquina.

### Base de datos en la nube (Prisma Postgres, Neon…)

Para que los datos vivan fuera del PC (y sean los mismos cuando se despliegue en
Render), copia `.env.example` a `.env`, pega la conexión directa de tu base
Postgres y vuelve a lanzar `docker compose up -d --build`. Después crea la partida
y carga las fichas como se explica más abajo.

### Para desarrollar (recarga en caliente)

Requisitos: Java 21, Maven, Node 22.

```bash
cd frontend && npm install && npm run dev      # http://localhost:5173, proxy de /api al 8080
```

El frontend de desarrollo usa el backend que haya en el puerto 8080 (el de Docker
vale). Si prefieres el backend sin Docker:

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local   # H2 en backend/data
```

MySQL local opcional: `docker compose --profile mysql up -d` (puerto 3307) y
`mvn spring-boot:run` sin perfil.

### Crear una partida

```bash
curl -X POST http://localhost:8080/api/admin/partidas \
  -H "X-Admin-Key: cambia-esta-clave" -H "Content-Type: application/json" \
  -d '{"nombre":"La Última Noche","casa":"Nombre de la casa","fechaInicio":"2026-10-23","fechaFin":"2026-10-25","nombreMaster":"David"}'
```

La respuesta incluye `codigoMaster`. Entra con él en la app y da de alta a los
jugadores: cada uno recibe su propio código y enlace.

### Cargar la partida desde personajes.md

Todos los datos de una partida (fichas, secretos, objetivos, habilidades, pistas,
QR, sobres programados, visiones y estancias de la casa) viven en un único
`personajes.md` dentro de `contenido-privado/` (carpeta ignorada por git). Se
importa desde el panel del Máster (Partida → Importar personajes.md) o con:

```bash
node scripts/cargar-fichas.mjs CODIGOMASTER contenido-privado/la-ultima-noche/personajes.md
```

Todo se empareja por nombre o título, así que se puede importar las veces que
haga falta: crea lo nuevo y actualiza lo cambiado, sin tocar lo que ya ha pasado
(sobres abiertos, visiones enviadas, habilidades usadas). Las imágenes (fotos y
visiones) pueden ir como ruta a un fichero junto al `.md` (`visiones/fer-1.jpg`):
al importar se suben a la base de datos de la partida y se sirven por un enlace
con clave (`/api/imagenes/…`); las ya subidas no se repiten. El formato está
explicado en `frontend/src/partidaMd.js` y al final del propio fichero.

## Desplegar gratis (Render + Postgres en la nube)

El `Dockerfile` construye una sola imagen: Spring Boot sirve la API y el frontend
compilado, así que basta con un servicio. Vale para cualquier proveedor que acepte
Docker; estos pasos son para **Render** (servicio web gratuito) con un Postgres
gratuito y persistente (**Prisma Postgres** o **Neon**).

1. **Base de datos.** Si ya la usas en local (`.env`), es la misma: los datos
   cargados desde casa aparecen en Render. Si no, crea un proyecto en
   <https://prisma.io> o <https://neon.tech> y copia la conexión directa. La URL
   JDBC es `jdbc:postgresql://HOST:5432/BASE?sslmode=require` (usuario y
   contraseña van aparte).
2. **Código en GitHub.** Render despliega desde un repositorio: sube esta carpeta
   con `Dockerfile` y `render.yaml`. `contenido-privado/` queda fuera gracias al
   `.gitignore`.
3. **Servicio.** En <https://render.com>: "New +" → "Blueprint" → elige el
   repositorio. Render lee `render.yaml`, genera `ADMIN_KEY` y pide `DB_URL`,
   `DB_USER` y `DB_PASSWORD`. El primer despliegue tarda unos 10 minutos.
4. **Partida.** Crea la partida contra la URL pública (la `ADMIN_KEY` está en la
   pestaña *Environment* del servicio) y carga las fichas:

   ```bash
   curl -X POST https://TU-SERVICIO.onrender.com/api/admin/partidas \
     -H "X-Admin-Key: LA_CLAVE" -H "Content-Type: application/json" \
     -d '{"nombre":"La Última Noche","casa":"Nombre de la casa","fechaInicio":"2026-10-23","fechaFin":"2026-10-25","nombreMaster":"David"}'
   node scripts/cargar-fichas.mjs CODIGOMASTER contenido-privado/la-ultima-noche/personajes.md https://TU-SERVICIO.onrender.com
   ```

5. **Móviles.** Cada jugador entra con `https://TU-SERVICIO.onrender.com/?codigo=SUCODIGO`.

Límites del plan gratuito: el servicio se duerme tras 15 minutos sin visitas y tarda
alrededor de un minuto en despertar (abre la app un rato antes de empezar; durante
la partida los móviles la mantienen despierta). Neon se pausa sola y se reanuda en
segundos. Para probar la imagen en tu PC (sin volúmenes: los datos del contenedor
se pierden al pararlo):

```bash
docker build -t misterio-en-vivo .
docker run --rm --name misterio-prueba -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local misterio-en-vivo
```

### Desarrollar sin tener el backend arrancado en el PC

El proxy de Vite apunta al backend local, pero puede apuntar al desplegado:

```powershell
$env:API_URL = 'https://TU-SERVICIO.onrender.com'; npm run dev
```

## Configuración (variables de entorno)

| Variable | Para qué | Por defecto |
|---|---|---|
| `ADMIN_KEY` | Clave para crear partidas. **Cámbiala fuera de tu ordenador.** | `cambia-esta-clave` |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/misterio…` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de MySQL | `misterio` / `misterio` |
| `PORT` | Puerto del backend | `8080` |

## API

Autenticación: `Authorization: Bearer CODIGO` (jugador o Máster) y `X-Admin-Key`
para administración.

| Ruta | Quién | Qué hace |
|---|---|---|
| `POST /api/admin/partidas` · `GET /api/admin/partidas` | Admin | Crear / listar partidas |
| `POST /api/sesion` | Cualquiera | Valida un código y devuelve nombre, rol y partida |
| `GET /api/yo` | Jugador | Su panel completo |
| `POST /api/yo/mensajes/{id}/leido` | Jugador | Marcar mensaje como leído |
| `PUT /api/yo/anotaciones/{jugadorId}` | Jugador | Guardar su cuaderno privado sobre otro personaje |
| `POST /api/yo/pagos` · `POST /api/yo/tienda/{pistaId}` · `PUT /api/yo/arma` | Jugador | Pagar, comprar una pista, elegir arma (asesino) |
| `POST /api/yo/objetivos/{id}/entregas` · `DELETE /api/yo/entregas/{id}` | Jugador | Aportar o retirar las pruebas de un objetivo propio |
| `GET /api/master/estado` | Máster | Estado completo de la partida |
| `PUT /api/master/fase` · `PUT /api/master/jugadores/{id}/asesino` | Máster | Fase y asesinos (pueden ser varios) |
| `POST /api/master/dinero` · `PUT /api/master/pistas/{id}/precio` · `POST /api/master/arma/pista` | Máster | Dar o quitar dinero, precios de la tienda, pista del arma |
| `POST/PUT/DELETE /api/master/jugadores…` | Máster | Fichas de jugadores |
| `…/jugadores/{id}/secretos` · `/secretos/{id}` | Máster | Secretos y su revelación |
| `…/jugadores/{id}/objetivos` · `/objetivos/{id}` | Máster | Objetivos y su estado |
| `/api/master/pistas…` · `/pistas/{id}/visibilidad` | Máster | Pistas y quién las ve |
| `POST /api/master/mensajes` | Máster | Mensaje a algunos o a todos |
| `PUT/DELETE /api/master/variables/{clave}` | Máster | Variables narrativas |

## Tests

```bash
cd backend && mvn verify
```

Recorren una partida completa con datos inventados y comprueban los permisos:
qué ve cada jugador, que un jugador no puede usar el panel del Máster y que dos
partidas no se mezclan.

## Pendiente

- Completar las fichas y la red narrativa (fases 1–4 del documento base).
- Tiempo real (SSE/WebSocket); ahora los paneles se refrescan solos cada pocos segundos.
- Migraciones de base de datos (Flyway) cuando el modelo se dé por cerrado.
- Despliegue: dónde se alojará para que funcione en la casa.
- Diseño visual definitivo.
- Subir fotos de personajes desde el panel del Máster (ahora se pega un enlace http(s)).
