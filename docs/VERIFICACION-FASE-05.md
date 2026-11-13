# Fase 05 — Frontend y navegación

Verificación: **13 de noviembre de 2026**. Implementación y pruebas de interfaz completadas con los límites indicados abajo.

## Implementación

| Requisito | Resultado verificable |
|---|---|
| React, TypeScript, Vite, Tailwind y shadcn/ui | Se reutilizan las dependencias y primitivas existentes; no se añade otro framework. |
| Interfaz administrativa adaptable | Referencia `DISEÑOS/DASHBOARD/`: sidebar blanco colapsable, activo azul, fondo lavanda, barra con búsqueda de secciones, tarjetas y dos columnas. |
| Navegación y breadcrumbs | Configuración central en `frontend/src/lib/nav.ts`; título del documento y foco cambian al navegar. |
| Variantes de layout | Administración: accesos y salud; técnico: alcance de trabajo; consulta: información disponible. Comparten shell accesible. |
| Sesión existente | `/api/v1/me`, cookie de mismo origen y cierre POST con CSRF; se conserva el acceso existente. |
| Permisos y centros | Menú, accesos rápidos y guardia de ruta usan `canVisit`; usuarios y roles exigen permisos globales. |
| Componentes reutilizables | `DataTable`, `FilterBar`, `FormField`, `PageState`, `ConfirmDialog` y primitivas shadcn/ui. |
| Estados | Carga, vacío, error con reintento, acceso denegado y ruta inexistente; un error no se muestra como cero registros. |
| Datos y formularios | TanStack Query, cliente API, cancelación, react-hook-form y Zod; validación estructural del perfil recibido. |
| Accesibilidad y móvil | Salto al contenido, foco de ruta, navegación de teclado, cierre Escape, restauración de foco, etiquetas y errores asociados; movimiento reducido. |
| Pruebas y evidencia | 25 pruebas automáticas y 14 capturas reproducibles en Chromium. |

### Política de presentación

`ADMINISTRADOR` selecciona la variante administrativa; `TECNICO` o `TECNICO_TI`, la técnica; los demás perfiles usan consulta. Esto cambia presentación, nunca concede permisos. Un perfil con varios roles conserva los permisos efectivos entregados por el servidor.

El selector enumera exclusivamente IDs con asignaciones `CENTROS` del perfil. Los IDs son reales; no se inventan nombres de centros. Para perfiles exclusivamente globales se mantiene el alcance organizativo sin construir una lista ficticia de centros. Si se revoca el centro seleccionado, la selección deja de ser válida al actualizar el perfil.

La selección filtra navegación, accesos rápidos, la tabla **Mi acceso** y **Mis matrices**. Las páginas administrativas existentes mantienen sus propios filtros y los límites que impone su API; el selector no se presenta como un filtro universal de todos los datos. Los permisos globales siguen siendo válidos al seleccionar un centro.

### Sesión y consultas

- Un fallo de `/me` bloquea el layout privado y ofrece reintento. Un 401 muestra el acceso existente y elimina consultas privadas y caché de mutaciones.
- Se actualiza el perfil al recuperar foco y cada 60 segundos con la página activa; el servidor conserva la responsabilidad de autorizar cada petición.
- Los comandos obtienen CSRF fresco. No se reenvían automáticamente mutaciones ante errores.
- Una respuesta HTTP no JSON conserva su código de error. El cierre fallido muestra un aviso y permite reintentar, sin sustituir POST por GET.
- Las rutas administrativas se cargan bajo demanda. No se agregan cifras, activos, traslados ni acciones simuladas al producto.

## Pruebas ejecutadas

Desde `frontend/`:

```powershell
npm.cmd run lint
npm.cmd run test
npm.cmd run build
```

Resultado final: **lint correcto, 25/25 pruebas correctas, compilación TypeScript y build correctos**. Incluye búsqueda de secciones autorizadas y colapso/expansión del menú.

Cobertura: navegación por permisos, URL directa, alcance global frente a centro, filtro por centro, las tres variantes, carga/vacío/error, perfil inválido, sesión 401 y expiración, breadcrumbs, foco, menú móvil, búsqueda, 404, error de matrices, CSRF y confirmación rechazada.

El build mantiene una advertencia de tamaño: bloque inicial de aproximadamente **596 kB**, **187 kB gzip**. La carga diferida reduce el bloque anterior de aproximadamente 667 kB; quedan oportunidades de optimización del conjunto compartido. No se ha medido rendimiento en dispositivos físicos.

## Evidencia de navegador

**Son pruebas de presentación con respuestas HTTP aisladas dentro del navegador.** Los perfiles llevan la etiqueta “Prueba visual”; no se crean usuarios ni datos en MySQL y no se instala un modo demo en la aplicación. No representan una verificación real del flujo OIDC/MFA.

Playwright finalizó con `PASS`: escritorio 1440 × 1000, móviles 390 × 844 y 320 × 720, filtro de centro, denegación por ruta directa y devolución de foco. Sin desbordamiento horizontal de página ni excepciones JavaScript. Las tablas permiten desplazamiento horizontal interno mediante teclado. Los HTTP 401 y 503 de los escenarios de error son intencionales.

| Pantalla | Captura |
|---|---|
| Administración | [Escritorio](../output/playwright/phase05/01-admin-desktop.png) |
| Usuarios sin resultados | [Listado vacío](../output/playwright/phase05/02-users-empty.png) |
| Formulario existente | [Alta de usuario](../output/playwright/phase05/03-user-form.png) |
| Espacio técnico | [Escritorio](../output/playwright/phase05/04-technician-desktop.png) |
| Permisos por centro | [Móvil 390 px](../output/playwright/phase05/05-mobile-access.png) |
| Navegación móvil | [Menú abierto](../output/playwright/phase05/06-mobile-navigation.png) |
| Acceso denegado | [Ruta protegida](../output/playwright/phase05/07-access-denied.png) |
| Consulta | [Escritorio](../output/playwright/phase05/08-consultation-desktop.png) |
| Error de sesión | [Error y reintento](../output/playwright/phase05/09-session-error.png) |
| Acceso existente | [Login](../output/playwright/phase05/10-login.png) |
| Carga de sesión | [Carga](../output/playwright/phase05/11-session-loading.png) |
| Pantalla estrecha | [Móvil 320 px](../output/playwright/phase05/12-mobile-320.png) |
| Menú colapsado | [Sidebar compacto](../output/playwright/phase05/13-sidebar-collapsed.png) |
| Búsqueda por permisos | [Navegación desde la barra](../output/playwright/phase05/14-navigation-search.png) |

### Adaptación del diseño aportado

Se conserva la paleta `#faf8ff`, `#f2f3ff`, `#001c50`, `#17326c`, `#00658d` y blanco, tipografía Inter, sidebar de 256 px, barra de 64 px, cuatro tarjetas y distribución principal/secundaria. La composición es común para todos los perfiles; nombre, roles, alcance, navegación y datos se derivan de la sesión. El buscador admite Ctrl/Cmd K y permite navegar con teclado.

Los valores ilustrativos del diseño (activos, porcentajes, traslados, alertas, notificaciones y certificados) no se cargan en el producto. Las cuatro tarjetas representan permisos, alcance, matrices y roles reales. Tampoco se habilitan exportaciones ni registros sin API. La búsqueda corresponde a secciones implementadas, no anuncia búsqueda de activos. Los estados vacíos y de error se conservan.

Reproducir desde dos terminales, con Node/npm y Chrome disponibles:

```powershell
# Terminal 1, desde frontend/
npm.cmd run dev -- --port 5180 --host 127.0.0.1

# Terminal 2, desde la raíz del repositorio
New-Item -ItemType Directory -Force output/playwright/phase05
npx.cmd --yes --package @playwright/cli playwright-cli -s=phase05 open http://127.0.0.1:5180
npx.cmd --yes --package @playwright/cli playwright-cli -s=phase05 run-code --filename frontend/scripts/capture-phase05.cjs
npx.cmd --yes --package @playwright/cli playwright-cli -s=phase05 close
```

Los comandos CLI descargan su herramienta si no está disponible. Las respuestas de prueba se limitan a esa sesión de navegador y se retiran al terminar el script. Los snapshots temporales se excluyen mediante `.gitignore`; las capturas finales se conservan en `output/playwright/phase05/`.

## Límites de aceptación

- No se revalidaron inicio real con Keycloak, MFA, alta de usuarios, autorización de servidor ni aislamiento MySQL en esta fase. La evidencia de UI no sustituye esas pruebas.
- Se verificaron teclado, foco y tamaños indicados; quedan pendientes lector de pantalla y pruebas en Safari, Firefox y dispositivos físicos. No se declara certificación WCAG completa.
- Los cambios preexistentes de `KeycloakAdminGateway.java` se conservaron sin intervención en esta tarea.
