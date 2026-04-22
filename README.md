# alfa-auth-service

Microservicio de autenticación y autorización del sistema **Alfa Hospital**.  
Puerto: `8081` · Base de datos: SQL Server (`alfa_auth`) · Cache: Redis

---

## Tabla de contenidos

1. [Arquitectura y decisiones de diseño](#1-arquitectura-y-decisiones-de-diseño)
2. [Modelo de datos](#2-modelo-de-datos)
3. [Flujo de autenticación JWT](#3-flujo-de-autenticación-jwt)
4. [Redis – Lista negra de tokens](#4-redis--lista-negra-de-tokens)
5. [Endpoints](#5-endpoints)
6. [Configuración](#6-configuración)
7. [Guía de usuario por rol](#7-guía-de-usuario-por-rol)
8. [Recuperación de contraseña](#8-recuperación-de-contraseña)
9. [FAQ – Problemas comunes de acceso](#9-faq--problemas-comunes-de-acceso)

---

## 1. Arquitectura y decisiones de diseño

### ¿Por qué JWT propio y no Keycloak?

| Criterio | JWT propio (implementado) | Keycloak |
|---|---|---|
| Infraestructura | Solo Spring Boot + Redis | Servidor Keycloak adicional + su propia BD |
| Complejidad de setup | `application.properties` + 2 clases | Configuración de realms, clients, flows |
| Control de roles | Lógica en el propio servicio | Mapeo de roles entre Keycloak y la app |
| Despliegue | `docker-compose` simple | Contenedor extra con configuración inicial |
| Aprendizaje | Dominio completo del ciclo JWT | Abstracción que oculta el mecanismo |

Para el alcance del proyecto (5 roles hospitalarios, autenticación por email/contraseña) la solución JWT propia es suficiente, más ligera y más fácil de mantener por el equipo.

### Patrón de seguridad: Gateway + Headers

El servicio **no protege sus propios endpoints** con JWT en los filtros internos. La seguridad se delega al **API Gateway**:

```
Cliente → API Gateway (AuthFilter) → valida JWT + consulta /api/auth/validate
                                    → inyecta headers: X-User-Id, X-User-Email, X-User-Role
                                    → enruta al microservicio destino
```

Cada microservicio destino (agendamiento, notificaciones, etc.) lee los headers inyectados para autorización por rol, sin volver a validar el JWT.

---

## 2. Modelo de datos

### Entidad `User` (tabla `users`)

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `Long` | PK, autoincremento | Identificador interno |
| `email` | `String` | NOT NULL, UNIQUE | Credencial de acceso |
| `password` | `String` | NOT NULL | Hash BCrypt |
| `firstName` | `String` | NOT NULL | Nombre |
| `lastName` | `String` | NOT NULL | Apellido |
| `role` | `Role` | NOT NULL | Rol del sistema |
| `phone` | `String` | nullable | Teléfono de contacto |
| `idType` | `String` | nullable | Tipo de documento (CC, CE, etc.) |
| `idNumber` | `String` | nullable | Número de documento |
| `birthDate` | `String` | nullable | Fecha de nacimiento |
| `city` | `String` | nullable | Ciudad de residencia |
| `gender` | `String` | nullable | Género |

### Enum `Role`

```
PACIENTE      → usuario registrado desde la app (asignado automáticamente)
MEDICO        → profesional de salud
EJECUTIVO     → personal operativo
ADMINISTRADOR → gestión de usuarios y configuración
GERENCIA      → acceso de supervisión general
```

> El rol se almacena como `STRING` en la BD para legibilidad directa en consultas SQL.

---

## 3. Flujo de autenticación JWT

### Registro

```
[Cliente]  POST /api/auth/register  →  [AuthService]
                                            │
                                   ¿email existe?
                                     ├─ SÍ  → 400 "El email ya está registrado"
                                     └─ NO  → BCrypt.encode(password)
                                               → guarda User con role=PACIENTE
                                               → JwtService.generateToken(email, role, userId)
                                               → 200 { token, email, role }
```

### Login

```
[Cliente]  POST /api/auth/login  →  [AuthService]
                                         │
                                 findByEmail(email)
                                   ├─ no existe → 400 "Credenciales inválidas"
                                   └─ existe   → BCrypt.matches(password, hash)
                                                    ├─ NO  → 400 "Credenciales inválidas"
                                                    └─ SÍ  → generateToken(email, role, userId)
                                                              → 200 { token, email, role }
```

### Estructura del token JWT

```
Header:  { "alg": "HS256" }
Payload: {
  "sub":    "usuario@email.com",   ← email (subject)
  "role":   "MEDICO",
  "userId": 42,
  "iat":    1700000000,            ← emitido en
  "exp":    1700086400             ← expira en (24 h por defecto)
}
Firma: HMAC-SHA256(header + payload, jwt.secret)
```

### Validación en cada petición

```
[Cliente]  GET /cualquier-endpoint
  Header: Authorization: Bearer <token>
                │
          [API Gateway – AuthFilter]
                │
          GET /api/auth/validate  →  [AuthService]
                                          │
                                  1. ¿firma JWT válida?    → NO → false
                                  2. ¿token expirado?      → SÍ → false
                                  3. ¿token en blacklist?  → SÍ → false
                                          │ todo OK
                                        true
                │
          Inyecta headers → X-User-Id, X-User-Email, X-User-Role
                │
          [Microservicio destino]
```

---

## 4. Redis – Lista negra de tokens

Al hacer logout el token ya fue emitido y el cliente lo puede haber almacenado. Para invalidarlo sin estado en el servidor se usa Redis como blacklist.

**Clave:** `blacklist:<token_completo>`  
**Valor:** `"logout"`  
**TTL:** tiempo restante hasta la expiración natural del token

```java
// AuthService.logout()
long ttl = expiration.getTime() - System.currentTimeMillis();
if (ttl > 0) {
    redisTemplate.opsForValue().set("blacklist:" + token, "logout", Duration.ofMillis(ttl));
}
```

**Por qué TTL dinámico:** cuando el token expira naturalmente, Redis elimina la entrada automáticamente. No se acumulan tokens viejos ni se requiere limpieza manual.

**En validación:**
```java
Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
return jwtService.isTokenValid(token) && Boolean.FALSE.equals(isBlacklisted);
```

---

## 5. Endpoints

### Auth (`/api/auth`)

#### `POST /api/auth/register`
Registra un nuevo usuario. El rol asignado siempre es `PACIENTE`.

**Body:**
```json
{
  "email":     "juan.perez@email.com",
  "password":  "MiClave123!",
  "firstName": "Juan",
  "lastName":  "Pérez",
  "phone":     "3001234567",
  "idType":    "CC",
  "idNumber":  "1234567890",
  "birthDate": "1990-05-15",
  "city":      "Bogotá",
  "gender":    "M"
}
```

**Respuesta 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "juan.perez@email.com",
  "role":  "PACIENTE"
}
```

**Errores:** `400` si el email ya está registrado.

---

#### `POST /api/auth/login`
Autentica con email y contraseña.

**Body:**
```json
{
  "email":    "medico@alfahospital.com",
  "password": "MiClave123!"
}
```

**Respuesta 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "medico@alfahospital.com",
  "role":  "MEDICO"
}
```

**Errores:** `400` con mensaje `"Credenciales inválidas"` (no se diferencia si es email o contraseña para evitar enumeración de usuarios).

---

#### `POST /api/auth/logout`
Invalida el token actual agregándolo a la lista negra de Redis.

**Header:** `Authorization: Bearer <token>`  
**Respuesta:** `204 No Content`

---

#### `GET /api/auth/validate`
Verifica firma + expiración + blacklist. Consumido por el API Gateway.

**Header:** `Authorization: Bearer <token>`  
**Respuesta:** `true` o `false`

---

### Admin (`/api/auth/admin/users`)

> Uso interno del equipo de administración. No expuesto al usuario final.

#### `GET /api/auth/admin/users`
Lista todos los usuarios registrados.

**Respuesta 200:**
```json
[
  { "id": 1, "email": "juan@email.com", "firstName": "Juan", "lastName": "Pérez", "role": "PACIENTE" },
  { "id": 2, "email": "dr.gomez@alfa.com", "firstName": "Carlos", "lastName": "Gómez", "role": "MEDICO" }
]
```

#### `PATCH /api/auth/admin/users/{id}/rol?rol={ROL}`
Cambia el rol de un usuario.

**Ejemplo:** `PATCH /api/auth/admin/users/2/rol?rol=MEDICO`

**Respuesta 200:** usuario con rol actualizado.  
**Errores:** `404` si el id no existe.

---

### Internal (`/api/auth/internal`)

> Uso exclusivo entre microservicios. No expuesto al cliente.

#### `GET /api/auth/internal/user-by-email?email={email}`
Devuelve el perfil básico de un usuario dado su email.

**Respuesta 200:**
```json
{
  "id":      42,
  "nombre":  "Juan",
  "apellido": "Pérez",
  "email":   "juan@email.com",
  "phone":   "3001234567"
}
```

**Errores:** `404` si el email no existe.

---

## 6. Configuración

### Variables de entorno (producción)

| Variable | Descripción |
|---|---|
| `DB_URL` | JDBC URL del SQL Server (`jdbc:sqlserver://host:1433;databaseName=alfa_auth`) |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `REDIS_HOST` | Host del servidor Redis |
| `REDIS_PORT` | Puerto Redis (default `6379`) |
| `JWT_SECRET` | Clave secreta HMAC-SHA (mínimo 32 caracteres) |
| `JWT_EXPIRATION` | Duración del token en milisegundos (ej. `86400000` = 24 h) |

### Desarrollo local (`application-dev.properties`)

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=alfa_auth;encrypt=false;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=AlfaHospital2024!
spring.data.redis.host=localhost
spring.data.redis.port=6379
jwt.secret=alfa-hospital-secret-key-2024-muy-larga-para-seguridad
jwt.expiration=86400000
```

Para levantar con perfil dev:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 7. Guía de usuario por rol

### PACIENTE – Registro e inicio de sesión

**Registrarse:**
1. Ir a la pantalla de registro en la aplicación web.
2. Completar: nombre, apellido, email, contraseña, teléfono, tipo y número de documento, fecha de nacimiento, ciudad y género.
3. Al confirmar, se crea la cuenta con rol **PACIENTE** automáticamente y se inicia sesión.

**Iniciar sesión:**
1. Ingresar email y contraseña en la pantalla de login.
2. Si las credenciales son correctas, el sistema redirige al portal del paciente.

**Cerrar sesión:**
- Usar el botón "Cerrar sesión" en el menú. El token queda invalidado de inmediato y no se puede reutilizar aunque no haya expirado.

---

### MEDICO / EJECUTIVO / ADMINISTRADOR / GERENCIA

Estos roles **no se pueden auto-registrar**. El flujo es:

1. El usuario se registra normalmente → queda como `PACIENTE`.
2. Un **ADMINISTRADOR** accede al panel de gestión de usuarios.
3. El administrador asigna el rol correspondiente mediante:
   `PATCH /api/auth/admin/users/{id}/rol?rol=MEDICO`
4. El usuario cierra sesión y vuelve a iniciar sesión para obtener un token con el nuevo rol.

> **Importante:** el rol está codificado dentro del JWT. Un cambio de rol solo tiene efecto en el siguiente inicio de sesión.

---

## 8. Recuperación de contraseña

> El microservicio actual **no implementa recuperación de contraseña por email**. Esta funcionalidad está pendiente para una versión futura.

**Flujo provisional para casos urgentes:**
1. El usuario contacta al administrador del sistema.
2. El administrador accede directamente a la base de datos o usa un endpoint administrativo para actualizar el hash de contraseña.
3. Se notifica al usuario con una contraseña temporal para que la cambie en su próximo acceso.

**Para el equipo de desarrollo – implementación futura sugerida:**
- Endpoint `POST /api/auth/forgot-password` recibe email y genera un token de reseteo de corta duración (15 min) almacenado en Redis.
- Endpoint `POST /api/auth/reset-password` valida el token de reseteo y actualiza la contraseña con nuevo hash BCrypt.
- El servicio de notificaciones (`alfa-notificaciones-service`) envía el enlace por email.

---

## 9. FAQ – Problemas comunes de acceso

**P: Intenté iniciar sesión y recibo "Credenciales inválidas". ¿Qué hago?**  
R: Verifica que el email esté escrito correctamente (incluyendo mayúsculas/minúsculas). Si olvidaste la contraseña, contacta al administrador (ver sección 8). El mensaje no especifica si el error es en el email o en la contraseña por razones de seguridad.

---

**P: Mi token sigue funcionando aunque hice logout.**  
R: Esto no debería ocurrir. Al hacer logout el token es agregado a la lista negra en Redis. Verifica que el servicio Redis esté activo (`redis-cli ping` → debe responder `PONG`). Si Redis está caído, los logouts no se persisten.

---

**P: Cambié mi rol pero en la app sigo viendo el portal anterior.**  
R: El rol está codificado en el JWT. Debes **cerrar sesión y volver a iniciar sesión** para obtener un token con el rol actualizado.

---

**P: El token expiró antes de tiempo.**  
R: La expiración se configura con `jwt.expiration` (en milisegundos). El valor por defecto en desarrollo es `86400000` (24 horas). En producción puede ser diferente. Verifica la variable `JWT_EXPIRATION` en el entorno.

---

**P: Recibo `401 Unauthorized` al llamar a un endpoint aunque tengo token.**  
R: Verifica que el header tenga el formato exacto: `Authorization: Bearer <token>` (con espacio entre "Bearer" y el token, sin comillas). Verifica también que el token no haya expirado.

---

**P: Recibo `403 Forbidden` al intentar acceder a un recurso.**  
R: Tu rol no tiene permiso para ese recurso. Por ejemplo, un `PACIENTE` que intenta acceder a `/medico/agenda-semanal` recibe `403`. Contacta al administrador si crees que tu rol es incorrecto.

---

**P: El servicio no arranca y el log dice "Cannot connect to Redis".**  
R: Redis debe estar corriendo antes de iniciar el servicio. En local: `docker run -p 6379:6379 redis:alpine`. En producción, verifica `REDIS_HOST` y `REDIS_PORT`.

---

**P: El servicio no arranca y el log dice error de conexión a SQL Server.**  
R: Verifica que SQL Server esté activo y que la base de datos `alfa_auth` exista. En local, confirma que `spring.datasource.url` apunte al host correcto y que `encrypt=false;trustServerCertificate=true` estén en la URL para entornos sin SSL configurado.
