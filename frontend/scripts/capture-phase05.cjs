// Run through playwright-cli run-code --filename. Fixtures exist only in this browser.
/* global URL, document, window */
// eslint-disable-next-line @typescript-eslint/no-unused-expressions
async (page) => {
  const base = 'http://127.0.0.1:5180';
  const destination = 'output/playwright/phase05';
  const centers = ['11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222'];
  const admin = {
    id: 'test-admin', organizationId: 'test-org', displayName: 'Administración · Prueba visual',
    grants: ['access.users.read', 'access.users.manage', 'access.roles.read', 'access.matrices.read', 'access.audit.read', 'organization.read']
      .map(permission => ({ role: 'ADMINISTRADOR', scope: 'GLOBAL', centerId: null, permission }))
  };
  let profile = admin;
  let unavailable = false;
  let anonymous = false;
  let holdSession = false;
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/v1/me') {
      if (holdSession) return;
      return route.fulfill({ status: unavailable ? 503 : anonymous ? 401 : 200, json: unavailable || anonymous ? {} : profile });
    }
    if (path === '/api/v1/me/matrices') return route.fulfill({ json: [] });
    if (path === '/api/v1/users') return route.fulfill({ json: [] });
    return route.fulfill({ status: 404, json: { code: 'NOT_FIXTURED' } });
  });
  const shot = async (name) => {
    await page.screenshot({ path: `${destination}/${name}.png`, fullPage: true, animations: 'disabled' });
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    if (overflow) throw new Error(`Horizontal page overflow: ${name}`);
  };
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(base);
  await page.getByRole('heading', { name: 'Dashboard general de Gestión TI' }).waitFor();
  await page.getByText('0', { exact: true }).waitFor();
  await shot('01-admin-desktop');
  await page.getByRole('button', { name: 'Colapsar menú' }).click();
  await shot('13-sidebar-collapsed');
  await page.getByRole('button', { name: 'Expandir menú' }).click();
  await page.getByRole('searchbox', { name: 'Buscar sección' }).fill('Usuarios');
  await page.getByRole('list', { name: 'Resultados de navegación' }).waitFor();
  await shot('14-navigation-search');
  await page.keyboard.press('Escape');
  await page.getByRole('searchbox', { name: 'Buscar sección' }).fill('');
  await page.keyboard.press('Escape');
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('link', { name: 'Usuarios', exact: true }).click();
  await page.getByRole('heading', { name: 'Usuarios', exact: true }).waitFor();
  await page.getByText('Sin usuarios', { exact: true }).waitFor();
  await shot('02-users-empty');
  await page.getByRole('button', { name: 'Nuevo usuario' }).click();
  await page.getByRole('dialog').waitFor();
  await shot('03-user-form');
  await page.keyboard.press('Escape');
  profile = { ...admin, id: 'test-technician', displayName: 'Técnico · Prueba visual', grants: [
    { role: 'TECNICO_TI', scope: 'CENTROS', centerId: centers[0], permission: 'organization.read' },
    { role: 'TECNICO_TI', scope: 'CENTROS', centerId: centers[1], permission: 'inventory.read' },
  ] };
  await page.goto(base);
  await page.getByRole('heading', { name: 'Su alcance de trabajo' }).waitFor();
  await shot('04-technician-desktop');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`${base}/mi-acceso`);
  await page.getByRole('heading', { name: 'Mi acceso', exact: true }).waitFor();
  await page.getByRole('combobox', { name: 'Centro de trabajo' }).selectOption(centers[1]);
  if (await page.getByRole('table').getByText('organization.read').count()) throw new Error('Center filter failed');
  await shot('05-mobile-access');
  await page.setViewportSize({ width: 320, height: 720 });
  await shot('12-mobile-320');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Abrir menú de navegación' }).click();
  await page.getByRole('dialog', { name: 'Navegación', exact: true }).waitFor();
  await shot('06-mobile-navigation');
  await page.keyboard.press('Escape');
  if (!await page.getByRole('button', { name: 'Abrir menú de navegación' }).evaluate(el => document.activeElement === el)) throw new Error('Mobile focus was not restored');
  await page.goto(`${base}/access/users`);
  await page.getByRole('heading', { name: 'No tiene acceso a esta sección' }).waitFor();
  await shot('07-access-denied');
  profile = { ...admin, id: 'test-reader', displayName: 'Consulta · Prueba visual', grants: [] };
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(base);
  await page.getByRole('heading', { name: 'Información a su alcance' }).waitFor();
  await shot('08-consultation-desktop');
  unavailable = true;
  await page.goto(base);
  await page.getByRole('heading', { name: 'No se pudo verificar su acceso' }).waitFor();
  await shot('09-session-error');
  unavailable = false;
  anonymous = true;
  await page.goto(base);
  await page.getByRole('textbox').first().waitFor();
  await shot('10-login');
  anonymous = false;
  holdSession = true;
  await page.goto(base);
  await page.getByRole('status').waitFor();
  await shot('11-session-loading');
  await page.unrouteAll({ behavior: 'ignoreErrors' });
  if (errors.length) throw new Error(errors.join('\n'));
  return 'PASS: 14 screenshots; desktop/mobile 320/390; center filter; sidebar collapse; navigation search; direct denial; focus restoration; no page overflow; no JS errors. UI fixtures only, not an OIDC end-to-end test.';
}
