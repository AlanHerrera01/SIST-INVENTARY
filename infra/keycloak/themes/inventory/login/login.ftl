<#-- Tema de login "inventory": replica el diseno de DISEÑOS/LOGIN.
     Requiere las variables estandar del flujo OIDC de Keycloak. -->
<!DOCTYPE html>
<html lang="${lang!"es"}">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${msg("loginTitle", realm.displayName!'Veris Inventario')}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <link href="${url.resourcesPath}/css/inventory.css" rel="stylesheet" />
  <#if realm.internationalizationEnabled!false>
    <script>const locale = "${lang}";</script>
  </#if>
</head>
<body class="login-pf">
<div class="inv-shell">
  <main class="inv-main">
    <div class="inv-card">
      <!-- Columna izquierda: acceso -->
      <section class="inv-left">
        <header class="inv-brand-row">
          <div class="inv-brand">
            <div class="inv-logo" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M12 2C10.62 2 9.5 3.12 9.5 4.5S10.62 7 12 7s2.5-1.12 2.5-2.5S13.38 2 12 2zm-5 7l3.5 12h3l3.5-12h-3l-2 7.5L10 9H7z"/></svg>
            </div>
            <div>
              <div class="inv-wordmark">
                <span class="inv-wordmark-name">veris</span>
                <span class="inv-wordmark-dot">•</span>
                <span class="inv-wordmark-app">ParaMí</span>
              </div>
              <p class="inv-tagline">Gestión de Inventario TI • Centrales Médicas</p>
            </div>
          </div>
          <span class="inv-realm-badge"><span class="inv-dot"></span>SSO Activo</span>
        </header>

        <div class="inv-heading">
          <h1>Acceso Seguro al Sistema</h1>
          <p>Ingresa con tus credenciales corporativas del directorio activo.</p>
        </div>

        <#if message?has_content && (message.type! = "error" || message.type! = "warning")>
          <div class="inv-alert" role="alert">
            <span class="inv-alert-icon" aria-hidden="true">!</span>
            <span>${kcSanitize(message.summary!message)}</span>
          </div>
        </#if>

        <form id="kc-form-login" class="inv-form" action="${url.loginAction}" method="post">
          <div class="inv-field">
            <label for="username">${msg("username")}</label>
            <div class="inv-input-wrap">
              <svg class="inv-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input id="username" name="username" type="text" autofocus autocomplete="username"
                     value="${(login.username!'')}" placeholder="usuario@veris.com.ec" required />
            </div>
          </div>

          <div class="inv-field">
            <label for="password">${msg("password")}</label>
            <div class="inv-input-wrap">
              <svg class="inv-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input id="password" name="password" type="password" autocomplete="current-password" required />
            </div>
          </div>

          <#if realm.rememberMe!false>
            <div class="inv-remember">
              <input id="rememberMe" name="rememberMe" type="checkbox" />
              <label for="rememberMe">${msg("rememberMe")}</label>
            </div>
          </#if>

          <button class="inv-submit" name="login" id="kc-login" type="submit">
            ${msg("doLogIn")}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </button>

          <#if realm.resetPasswordAllowed!false>
            <a class="inv-forgot" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
          </#if>
        </form>

        <div class="inv-legal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
          <p>Acceso exclusivo para personal autorizado de <strong>Veris Centrales Médicas</strong>.
          Conexión cifrada y control de acceso basado en roles (RBAC) por centro.</p>
        </div>
      </section>

      <!-- Columna derecha: hero institucional -->
      <aside class="inv-right" aria-hidden="true">
        <div class="inv-right-glow inv-right-glow-a"></div>
        <div class="inv-right-glow inv-right-glow-b"></div>
        <div class="inv-right-inner">
          <span class="inv-platform">PLATAFORMA DE ACTIVOS CLÍNICOS • TI</span>
          <h2>Gestión integral, trazabilidad y control de equipamiento tecnológico hospitalario.</h2>
          <p>Consola centralizada para la administración de hardware, equipos diagnósticos,
             periféricos de consultorio y actas digitales de entrega y custodia.</p>
          <div class="inv-kpis">
            <div class="inv-kpi">
              <div class="inv-kpi-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg></div>
              <div><p class="inv-kpi-title">Inventario multicentro</p><p class="inv-kpi-sub">Activos, bodegas TI y ubicaciones por sede</p></div>
              <span class="inv-kpi-tag">Quito · Gye · CUE</span>
            </div>
            <div class="inv-kpi">
              <div class="inv-kpi-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg></div>
              <div><p class="inv-kpi-title">Actas y firmas digitales</p><p class="inv-kpi-sub">Matrices de firma y custodia auditada</p></div>
            </div>
            <div class="inv-kpi">
              <div class="inv-kpi-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>
              <div><p class="inv-kpi-title">Seguridad y auditoría</p><p class="inv-kpi-sub">RBAC por centro, sesiones y bitácora</p></div>
            </div>
          </div>
        </div>
        <footer class="inv-right-foot">
          <span>Veris Ecuador S.A. • Todos los derechos reservados</span>
          <span>Sistema de Inventario Tecnológico</span>
        </footer>
      </aside>
    </div>
  </main>

  <footer class="inv-footer">
    <span class="inv-footer-item">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
      Portal Seguro Veris IT • Conexión Cifrada TLS
    </span>
    <span class="inv-footer-item">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
      Control de acceso por roles y centros autorizados
    </span>
  </footer>
</div>
</body>
</html>
