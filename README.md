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
  secretos (ocultos o revelados al grupo), objetivos, pistas con desbloqueo por
  jugador, mensajes privados o a todos, variables narrativas y registro de eventos.
- **Panel del jugador** (pensado para móvil): personaje, objetivos, pistas
  descubiertas, mensajes del Máster y secretos. Solo recibe lo que puede saber.
- El backend es la única autoridad: el frontend no decide ni filtra nada.

## Estructura

```
backend/    Java 21 + Spring Boot 3 (API REST, reglas, permisos)
frontend/   React + Vite (panel de jugador y de Máster)
docker-compose.yml   MySQL para desarrollo
```

## Arrancar en local

Requisitos: Java 21, Maven, Node 22.

**Backend**, opción rápida sin MySQL (H2 en fichero):

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Con MySQL:

```bash
docker compose up -d
cd backend && mvn spring-boot:run
```

**Frontend**:

```bash
cd frontend
npm install
npm run dev
```

Abre <http://localhost:5173>. Para probar desde el móvil en la misma wifi, usa la
dirección de red que muestra Vite.

### Crear una partida

```bash
curl -X POST http://localhost:8080/api/admin/partidas \
  -H "X-Admin-Key: cambia-esta-clave" -H "Content-Type: application/json" \
  -d '{"nombre":"La Última Noche","casa":"Nombre de la casa","fechaInicio":"2026-10-23","fechaFin":"2026-10-25","nombreMaster":"David"}'
```

La respuesta incluye `codigoMaster`. Entra con él en la app y da de alta a los
jugadores: cada uno recibe su propio código y enlace.

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
| `GET /api/master/estado` | Máster | Estado completo de la partida |
| `PUT /api/master/fase` · `PUT /api/master/asesino` | Máster | Fase y asesino activo |
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
