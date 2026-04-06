
const state = { token: localStorage.getItem("eagle_token"), username: localStorage.getItem("eagle_username"), activePanel: "requester", loadedPanels: new Set(), roles: [], roomsById: new Map() };
if (document.getElementById("login-form")) initLogin();
if (document.querySelector(".dashboard-shell")) initDashboard();

function initLogin() {
  if (state.token) return window.location.replace('/dashboard.html');
  byId('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const status = byId('session-status');
    status.textContent = 'Authenticating...';
    try {
      const username = byId('username').value;
      const password = byId('password').value;
      const auth = await fetchJson('/api/v1/auth/login', 'POST', { username, password });
      localStorage.setItem('eagle_token', auth.token);
      localStorage.setItem('eagle_username', username);
      localStorage.setItem('eagle_roles', JSON.stringify(extractRoles(auth.token)));
      window.location.replace('/dashboard.html');
    } catch (error) { status.textContent = error.message; }
  });
}

function initDashboard() {
  if (!state.token) return window.location.replace('/login.html');
  state.roles = extractRoles(state.token);
  byId('active-user').textContent = state.username || 'workspace user';
  byId('logout-btn').addEventListener('click', logout);
  document.querySelectorAll('.nav-btn').forEach((b) => b.addEventListener('click', () => activatePanel(b.dataset.panel)));
  wireForms();
  wireDelegatedActions();
  applyRoleVisibility();
  seedTimes();
  activatePanel(defaultPanelForRoles());
  setInterval(async () => { if (state.activePanel === 'frontdesk') { try { await loadFrontDesk(); } catch (error) { setStatus(error.message); } } }, 30000);
}

function wireForms() {
  byId('requester-filter-form')?.addEventListener('submit', async (e) => { e.preventDefault(); await loadRequester(true); });
  byId('schedule-filter-form')?.addEventListener('submit', async (e) => { e.preventDefault(); await loadFrontDesk(true); });
  ['filter-start','filter-end','reservation-start','reservation-end','reservation-promo','selected-room-id'].forEach((id) => {
    const el = byId(id); if (!el) return; el.addEventListener('change', refreshBookingEstimate); el.addEventListener('input', refreshBookingEstimate);
  });
  byId('apply-promo-btn')?.addEventListener('click', applyPromotionQuote);

  byId('reservation-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const roomId = Number(byId('selected-room-id').value);
    const start = byId('reservation-start').value;
    const end = byId('reservation-end').value;
    if (!roomId) return showLocalFeedback('booking-feedback', 'Choose a room from the Select Room dropdown before submitting.');
    if (!start || !end) return showLocalFeedback('booking-feedback', 'Pick both reservation start and end.');
    try {
      await api('/api/v1/reservations', 'POST', { roomId, startTime: toIsoFromLocal(start), endTime: toIsoFromLocal(end), status: 'APPROVED' });
      const code = byId('reservation-promo').value.trim();
      if (code) await applyPromotionQuote();
      showLocalFeedback('booking-feedback', 'Reservation created successfully.');
      showToast('Reservation created successfully.', 'success');
      await Promise.all([loadRequester(true), allowedPanels().includes('frontdesk') ? loadFrontDesk(true) : Promise.resolve()]);
    } catch (error) { showLocalFeedback('booking-feedback', error.message); showToast(error.message, 'error'); }
  });

  byId('banner-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const saved = await api('/api/v1/frontdesk/banner-templates', 'POST', { templateKey: byId('banner-key').value, minutesBefore: Number(byId('banner-minutes').value), message: byId('banner-message').value, active: true });
      prependItem('banner-list', renderBannerTemplateItem(saved));
      showToast('Banner template saved.', 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });

  byId('handoff-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const saved = await api('/api/v1/frontdesk/shift-handoffs', 'POST', { fromUserId: Number(byId('handoff-from').value), toUserId: Number(byId('handoff-to').value), handoffTime: new Date().toISOString(), summary: byId('handoff-summary').value, pendingTasks: byId('handoff-tasks').value });
      prependItem('handoff-list', renderHandoffItem(saved));
      showToast('Shift handoff saved.', 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });

  byId('refresh-notifications-btn')?.addEventListener('click', async () => {
    try {
      const result = await api('/api/v1/frontdesk/notifications/refresh', 'POST');
      await loadFrontDesk(true);
      showToast(`Notifications refreshed. Queued ${result.queued}, displayed ${result.processed}.`, 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });

  byId('asset-status-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await updateAssetState(Number(byId('asset-id').value), byId('asset-operational-status').value, byId('asset-lifecycle-state').value);
      showToast('Asset state updated.', 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });

  byId('promotion-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const saved = await api('/api/v1/operations/promotions', 'POST', { code: byId('promotion-code').value, percentage: Number(byId('promotion-percent').value), promotionType: byId('promotion-type').value, startsAt: new Date(Date.now() - 86400000).toISOString(), endsAt: new Date(Date.now() + 86400000 * 30).toISOString(), active: true });
      prependItem('promotion-list', `<div class="item"><strong>${saved.code}</strong><div>${saved.promotionType}</div><div class="item-meta">${saved.percentage}% active discount</div></div>`);
      showToast('Promotion saved.', 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });

  byId('announcement-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const saved = await api('/api/v1/operations/announcements', 'POST', { title: byId('announcement-title').value, message: byId('announcement-message').value, published: true });
      prependItem('announcement-list', `<div class="item"><strong>${saved.title}</strong><div>${saved.message}</div></div>`);
      showToast('Announcement published.', 'success');
    } catch (error) { showToast(error.message, 'error'); }
  });
  byId('room-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/v1/admin/facilities/rooms', 'POST', { locationCode: byId('room-location-code').value, locationName: byId('room-location-name').value, locationAddress: byId('room-location-address').value, roomTypeName: byId('room-type-name').value, roomNumber: byId('room-number').value, floorNumber: Number(byId('room-floor').value), capacity: Number(byId('room-capacity').value), includeProjector: byId('room-projector').checked, includeHvac: byId('room-hvac').checked });
      byId('room-number').value = '';
      await Promise.all([loadAdmin(true), loadRequester(true)]);
      showToast('Room created.', 'success');
    } catch (error) { showLocalFeedback('room-feedback', error.message); showToast(error.message, 'error'); }
  });

  byId('user-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const saved = await api('/api/v1/users', 'POST', { username: byId('user-username').value, email: byId('user-email').value, password: byId('user-password').value, role: byId('user-role').value, staffContactInfo: byId('user-contact').value });
      prependItem('user-list', `<div class="item"><strong>${saved.username}</strong><div>${saved.email}</div><div class="item-meta">Contact ${saved.contactInfo}</div></div>`);
      byId('user-form').reset();
      showToast('User created.', 'success');
    } catch (error) { showLocalFeedback('user-feedback', error.message); showToast(error.message, 'error'); }
  });

  byId('scorecard-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const from = toUsDateString(byId('effective-from').value);
    const toInput = byId('effective-to').value;
    const to = toInput ? toUsDateString(toInput) : '';
    const weighted = { quality: Number(byId('weight-quality').value), speed: Number(byId('weight-speed').value), compliance: Number(byId('weight-compliance').value), experience: Number(byId('weight-experience').value) };
    const sum = Object.values(weighted).reduce((a, b) => a + b, 0);
    if (!from || (toInput && !to)) return showLocalFeedback('scorecard-feedback', 'Please choose valid dates.');
    if (sum !== 100) return showLocalFeedback('scorecard-feedback', 'Weights must sum to 100.');
    try {
      const payload = { metricKey: byId('metric-key').value, effectiveFrom: from, weightedDimensions: weighted, definition: JSON.stringify(weighted) };
      if (to) payload.effectiveTo = to;
      const saved = await api('/api/v1/metrics/scorecards', 'POST', payload);
      prependItem('scorecard-list', `<div class="item"><strong>${saved.metricKey} v${saved.version}</strong><div>${saved.effectiveFrom}</div><div class="item-meta">${saved.definition}</div></div>`);
      showLocalFeedback('scorecard-feedback', 'Scorecard saved successfully.');
      showToast('Scorecard saved.', 'success');
    } catch (error) { showLocalFeedback('scorecard-feedback', error.message); showToast(error.message, 'error'); }
  });
}

function wireDelegatedActions() {
  document.addEventListener('click', async (event) => {
    const roomButton = event.target.closest('[data-room-select]');
    if (roomButton) return selectRoom(Number(roomButton.dataset.roomSelect));

    const templateButton = event.target.closest('[data-template-edit]');
    if (templateButton) {
      byId('banner-key').value = templateButton.dataset.templateKey;
      byId('banner-minutes').value = templateButton.dataset.minutesBefore;
      byId('banner-message').value = templateButton.dataset.message;
      return showToast(`Loaded ${templateButton.dataset.templateKey} into the editor.`, 'success');
    }

    const action = event.target.closest('[data-reservation-action]');
    if (action) {
      try {
        const endpoint = action.dataset.reservationAction === 'confirm' ? 'confirm-arrival' : 'checkout';
        await api(`/api/v1/frontdesk/reservations/${action.dataset.reservationId}/${endpoint}`, 'POST');
        await loadFrontDesk(true);
        showToast(endpoint === 'confirm-arrival' ? 'Arrival confirmed.' : 'Checkout completed.', 'success');
      } catch (error) { showToast(error.message, 'error'); }
      return;
    }

    const dismiss = event.target.closest('[data-banner-dismiss]');
    if (dismiss) {
      try {
        await api(`/api/v1/frontdesk/notifications/${dismiss.dataset.bannerDismiss}/dismiss`, 'POST');
        await loadFrontDesk(true);
        showToast('Banner dismissed.', 'success');
      } catch (error) { showToast(error.message, 'error'); }
      return;
    }

    const asset = event.target.closest('[data-asset-action]');
    if (asset) {
      try {
        await updateAssetState(Number(asset.dataset.assetId), asset.dataset.operationalStatus, asset.dataset.lifecycleState);
        showToast('Asset state updated.', 'success');
      } catch (error) { showToast(error.message, 'error'); }
    }
  });
}

async function activatePanel(panelId) {
  if (!allowedPanels().includes(panelId)) return;
  document.querySelectorAll('.nav-btn').forEach((n) => n.classList.toggle('active', n.dataset.panel === panelId));
  document.querySelectorAll('.panel').forEach((p) => p.classList.toggle('active', p.id === panelId));
  state.activePanel = panelId;
  byId('panel-title').textContent = titleFor(panelId);
  await loadPanel(panelId);
}

async function loadPanel(panelId) {
  setStatus('Loading workspace data...');
  const loaders = { requester: loadRequester, frontdesk: loadFrontDesk, maintenance: loadMaintenance, operations: loadOperations, admin: loadAdmin };
  try { await loaders[panelId](); setStatus('Workspace is up to date.'); }
  catch (error) { setStatus(error.message || 'Workspace load failed.'); showToast(error.message || 'Workspace load failed.', 'error'); }
}

async function loadRequester(firstLoad) {
  if (firstLoad) await loadRequesterFilters();
  syncReservationTimes();
  const params = new URLSearchParams({ page: '0', size: '20' });
  if (byId('filter-location').value) params.set('location', byId('filter-location').value);
  if (byId('filter-room-type').value) params.set('roomType', byId('filter-room-type').value);
  if (byId('filter-capacity').value) params.set('minCapacity', byId('filter-capacity').value);
  if (byId('filter-start').value && byId('filter-end').value) { params.set('startTime', toIsoFromLocal(byId('filter-start').value)); params.set('endTime', toIsoFromLocal(byId('filter-end').value)); }
  const [rooms, mine] = await Promise.all([api(`/api/v1/catalog/rooms?${params.toString()}`), api('/api/v1/reservations/mine?page=0&size=20')]);
  state.roomsById = new Map(rooms.content.map((r) => [r.id, r]));
  render('rooms-grid', rooms.content.map(renderRoomCard));
  populateReservationRoomSelect(rooms.content);
  render('reservation-list', mine.content.map(renderReservationItem));
  refreshBookingEstimate();
}

async function loadRequesterFilters() {
  const [locations, types] = await Promise.all([api('/api/v1/catalog/locations'), api('/api/v1/catalog/room-types')]);
  populateSelect('filter-location', ['', ...locations], 'All locations');
  populateSelect('filter-room-type', ['', ...types], 'All room types');
}
async function loadFrontDesk() {
  const params = new URLSearchParams({ page: '0', size: '50' });
  if (byId('schedule-room-filter').value.trim()) params.set('room', byId('schedule-room-filter').value.trim());
  if (byId('schedule-status-filter').value) params.set('status', byId('schedule-status-filter').value);
  if (byId('schedule-from-filter').value) params.set('from', toIsoFromLocal(byId('schedule-from-filter').value));
  if (byId('schedule-to-filter').value) params.set('to', toIsoFromLocal(byId('schedule-to-filter').value));
  const [schedule, banners, handoffs, notifications, agents] = await Promise.all([
    api(`/api/v1/frontdesk/schedule-board?${params.toString()}`),
    api('/api/v1/frontdesk/banner-templates?page=0&size=20'),
    api('/api/v1/frontdesk/shift-handoffs?page=0&size=20'),
    api('/api/v1/frontdesk/notifications?page=0&size=50'),
    api('/api/v1/frontdesk/agents')
  ]);
  populateAgentSelects(agents);
  render('schedule-board', schedule.content.map(renderScheduleItem));
  render('banner-list', banners.content.map(renderBannerTemplateItem));
  render('handoff-list', handoffs.content.map(renderHandoffItem));
  render('active-banner-list', notifications.content.filter((n) => n.status === 'PROCESSED').map(renderActiveBannerItem));
  render('notification-history-list', notifications.content.filter((n) => n.status === 'DISMISSED').map(renderNotificationHistoryItem));
}

async function loadMaintenance() {
  const [inspections, tickets, assets] = await Promise.all([
    api('/api/v1/maintenance/inspections?page=0&size=20'),
    api('/api/v1/maintenance/tickets?page=0&size=20'),
    api('/api/v1/assets?page=0&size=50')
  ]);
  render('inspection-list', inspections.content.map((i) => `<div class="item"><strong>${i.roomNumber}</strong><div>${formatDate(i.inspectionTime)}</div><div class="item-meta">${i.outcome}</div></div>`));
  render('ticket-list', tickets.content.map((t) => `<div class="item"><strong>${t.title}</strong><div>SLA breached: ${t.slaBreached ? 'Yes' : 'No'}</div><div class="item-meta">Parts ${t.partsCost || 0} | Labor ${t.laborHours || 0}</div></div>`));
  populateAssetSelect(assets.content);
  render('asset-list', assets.content.map(renderAssetCard));
}

async function loadOperations() {
  const [promotions, announcements, moderation] = await Promise.all([
    api('/api/v1/operations/promotions?page=0&size=20'),
    api('/api/v1/operations/announcements?page=0&size=20'),
    api('/api/v1/operations/moderation-queue?page=0&size=20')
  ]);
  render('promotion-list', promotions.content.map((p) => `<div class="item"><strong>${p.code}</strong><div>${p.promotionType}</div><div class="item-meta">${p.percentage}% active discount</div></div>`));
  render('announcement-list', announcements.content.map((a) => `<div class="item"><strong>${a.title}</strong><div>${a.message}</div></div>`));
  render('moderation-list', moderation.content.map((m) => `<div class="item"><strong>${m.fileName}</strong><div>${m.status}</div><div class="item-meta">${m.reason || 'Awaiting review notes'}</div></div>`));
}

async function loadAdmin() {
  const [scorecards, users, assets, rooms] = await Promise.all([
    api('/api/v1/metrics/scorecards?page=0&size=20'),
    api('/api/v1/users?page=0&size=20'),
    api('/api/v1/assets?page=0&size=50'),
    api('/api/v1/catalog/rooms?page=0&size=50')
  ]);
  render('scorecard-list', scorecards.content.map((s) => `<div class="item"><strong>${s.metricKey} v${s.version}</strong><div>${s.effectiveFrom}</div><div class="item-meta">${s.definition}</div></div>`));
  render('user-list', users.content.map((u) => `<div class="item"><strong>${u.username}</strong><div>${u.email}</div><div class="item-meta">Contact ${u.contactInfo}</div></div>`));
  render('admin-room-list', rooms.content.map(renderRoomCard));
  render('admin-asset-list', assets.content.filter((a) => a.essential).map(renderAssetCard));
}

async function updateAssetState(assetId, operationalStatus, lifecycleState) {
  if (!assetId) throw new Error('Choose an asset before submitting a state change.');
  await api(`/api/v1/assets/${assetId}/status`, 'PATCH', { status: operationalStatus, lifecycleState });
  const reloads = [loadMaintenance(true), loadRequester(true)];
  if (state.roles.includes('ADMIN')) reloads.push(loadAdmin(true));
  await Promise.all(reloads);
}

async function applyPromotionQuote() {
  const code = byId('reservation-promo').value.trim();
  const estimate = calculateEstimate();
  if (!code) { byId('booking-final').textContent = formatMoney(estimate); return; }
  try {
    const result = await api('/api/v1/finance/promotions/apply', 'POST', { code, orderAmount: estimate, orderTime: new Date().toISOString() });
    byId('booking-final').textContent = formatMoney(Number(result.finalAmount));
    showLocalFeedback('booking-feedback', `Promotion ${code} applied successfully.`);
  } catch (error) {
    byId('booking-final').textContent = formatMoney(estimate);
    showLocalFeedback('booking-feedback', error.message);
    showToast(error.message, 'error');
  }
}

async function api(url, method = 'GET', body) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  return fetchJson(url, method, body, headers);
}

async function fetchJson(url, method, body, headers = { 'Content-Type': 'application/json' }) {
  const response = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  if (response.status === 401) { logout(); throw new Error('Unauthorized'); }
  if (!response.ok) {
    const text = await response.text();
    try { const parsed = JSON.parse(text); throw new Error(parsed.message || `Request failed: ${response.status}`); }
    catch (error) { if (error instanceof SyntaxError) throw new Error(text || `Request failed: ${response.status}`); throw error; }
  }
  return response.status === 204 ? {} : response.json();
}

function render(id, rows) { const target = byId(id); if (target) target.innerHTML = rows.join('') || '<div class="item">No records yet.</div>'; }
function prependItem(id, row) { const target = byId(id); if (!target) return; target.innerHTML = target.innerHTML.includes('No records yet.') ? row : row + target.innerHTML; }
function populateSelect(id, values, defaultLabel) { const t = byId(id); if (!t) return; t.innerHTML = values.map((v) => (v ? `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>` : `<option value="">${defaultLabel}</option>`)).join(''); }

function populateReservationRoomSelect(rooms) {
  const target = byId('selected-room-id'); if (!target) return;
  const available = rooms.filter((r) => r.available);
  const current = target.value;
  if (!available.length) { target.innerHTML = '<option value="">No rooms available for this time</option>'; target.value = ''; updateSelectedRoomLabel(null); return; }
  target.innerHTML = available.map((r) => `<option value="${r.id}">${escapeHtml(r.roomNumber)} - ${escapeHtml(r.location)} - Capacity ${r.capacity}</option>`).join('');
  const pick = available.find((r) => String(r.id) === current) || available[0];
  target.value = String(pick.id); updateSelectedRoomLabel(pick);
}

function populateAgentSelects(agents) {
  const from = byId('handoff-from'); const to = byId('handoff-to'); if (!from || !to) return;
  const options = agents.map((a) => `<option value="${a.id}">${escapeHtml(a.username)}</option>`).join('');
  from.innerHTML = options; to.innerHTML = options;
}

function populateAssetSelect(assets) {
  const target = byId('asset-id'); if (!target) return;
  target.innerHTML = assets.map((a) => `<option value="${a.id}">${escapeHtml(a.name)} - Room ${escapeHtml(a.roomNumber)} - ${escapeHtml(a.lifecycleState)}</option>`).join('');
}
function renderRoomCard(room) {
  const disabled = room.available ? '' : 'disabled';
  const cls = room.available ? 'success-text' : 'warning';
  return `<article class="workspace-card"><div class="item-meta">${escapeHtml(room.location)} - ${escapeHtml(room.roomType)}</div><h3>${escapeHtml(room.roomNumber)}</h3><p>Capacity ${room.capacity}</p><p class="${cls}">${escapeHtml(room.availabilityMessage || 'Availability unknown.')}</p>${room.warning ? `<p class="warning">${escapeHtml(room.warning)}</p>` : ''}<button class="primary-btn" data-room-select="${room.id}" ${disabled}>${room.available ? 'Reserve This Room' : 'Unavailable'}</button></article>`;
}
function renderReservationItem(r) { return `<div class="item"><strong>Room ${escapeHtml(r.roomNumber)}</strong><div>${formatDateTime(r.startTime)} - ${formatDateTime(r.endTime)}</div><div class="item-meta">${escapeHtml(r.status)}</div>${r.roomWarning ? `<div class="warning">${escapeHtml(r.roomWarning)}</div>` : ''}</div>`; }
function renderScheduleItem(item) {
  const actions = [];
  if (item.status === 'PENDING' || item.status === 'APPROVED') actions.push(`<button type="button" class="ghost-accent-btn" data-reservation-action="confirm" data-reservation-id="${item.id}">Confirm Arrival</button>`);
  if (item.status === 'CHECKED_IN') actions.push(`<button type="button" class="ghost-accent-btn" data-reservation-action="checkout" data-reservation-id="${item.id}">Checkout</button>`);
  const statusClass = item.status === 'CHECKED_IN' ? 'success-text' : (item.status === 'PENDING' ? 'warning' : 'item-meta');
  return `<div class="item"><strong>Room ${escapeHtml(item.roomNumber)}</strong><div>${formatDateTime(item.startTime)} - ${formatDateTime(item.endTime)}</div><div class="item-meta">Guest ${escapeHtml(item.requesterUsername || '')}</div><div class="${statusClass}">${escapeHtml(item.status)}</div><div class="item-meta">Checked in: ${item.checkedInAt ? formatDateTime(item.checkedInAt) : 'Not yet'}</div><div class="item-meta">Checked out: ${item.checkedOutAt ? formatDateTime(item.checkedOutAt) : 'Not yet'}</div><div class="action-row">${actions.join('')}</div></div>`;
}
function renderBannerTemplateItem(t) { return `<div class="item"><strong>${escapeHtml(t.templateKey)}</strong><div>${t.minutesBefore} minutes before event</div><div class="item-meta">${escapeHtml(t.message)}</div><div class="action-row"><button type="button" class="ghost-accent-btn" data-template-edit="true" data-template-key="${escapeHtml(t.templateKey)}" data-minutes-before="${t.minutesBefore}" data-message="${escapeHtml(t.message)}">Edit Template</button></div></div>`; }
function renderHandoffItem(h) { return `<div class="item"><strong>${escapeHtml(h.fromUser)} to ${escapeHtml(h.toUser)}</strong><div>${escapeHtml(h.summary)}</div><div class="item-meta">${formatDateTime(h.handoffTime)}</div><div class="warning">${escapeHtml(h.pendingTasks || 'No unresolved tasks')}</div></div>`; }
function renderActiveBannerItem(b) { return `<div class="item"><strong>${escapeHtml(b.templateKey)}</strong><div>${escapeHtml(b.message)}</div><div class="item-meta">Room ${escapeHtml(b.roomNumber)} - ${escapeHtml(b.requesterUsername)} - ${formatDateTime(b.scheduledFor)}</div><div class="action-row"><button type="button" class="ghost-accent-btn" data-banner-dismiss="${b.id}">Dismiss Banner</button></div></div>`; }
function renderNotificationHistoryItem(b) { return `<div class="item"><strong>${escapeHtml(b.templateKey)}</strong><div>${escapeHtml(b.message)}</div><div class="item-meta">${escapeHtml(b.status)} - Room ${escapeHtml(b.roomNumber)} - ${formatDateTime(b.scheduledFor)}</div><div class="item-meta">Dismissed: ${b.dismissedAt ? formatDateTime(b.dismissedAt) : 'Not dismissed'}</div></div>`; }
function renderAssetCard(a) { const essential = a.essential ? 'Essential equipment' : 'Standard asset'; return `<div class="item"><strong>${escapeHtml(a.name)}</strong><div>Room ${escapeHtml(a.roomNumber)} - ${escapeHtml(a.assetType)}</div><div class="${a.lifecycleState === 'UNDER_REPAIR' || a.operationalStatus === 'OUT_OF_SERVICE' ? 'warning' : 'item-meta'}">${escapeHtml(a.operationalStatus)} - ${escapeHtml(a.lifecycleState)} - ${essential}</div><div class="action-row"><button type="button" class="ghost-accent-btn" data-asset-action="true" data-asset-id="${a.id}" data-operational-status="OUT_OF_SERVICE" data-lifecycle-state="UNDER_REPAIR">Set Under Repair</button><button type="button" class="ghost-accent-btn" data-asset-action="true" data-asset-id="${a.id}" data-operational-status="IN_SERVICE" data-lifecycle-state="NORMAL">Restore</button><button type="button" class="ghost-accent-btn" data-asset-action="true" data-asset-id="${a.id}" data-operational-status="${a.operationalStatus}" data-lifecycle-state="RETIRED">Retire</button></div></div>`; }

function selectRoom(id) { const room = state.roomsById.get(id); if (!room) return; byId('selected-room-id').value = String(room.id); updateSelectedRoomLabel(room); syncReservationTimes(); refreshBookingEstimate(); showLocalFeedback('booking-feedback', room.available ? `Selected ${room.roomNumber}. You can book it now.` : room.availabilityMessage || 'This room is unavailable.'); }
function updateSelectedRoomLabel(room) { const t = byId('selected-room-label'); if (!t) return; t.textContent = !room ? 'Choose an available room from the filtered list.' : `${room.roomNumber} - ${room.location} - ${room.roomType} - Capacity ${room.capacity}`; }
function syncReservationTimes() { if (byId('filter-start').value) byId('reservation-start').value = byId('filter-start').value; if (byId('filter-end').value) byId('reservation-end').value = byId('filter-end').value; }
function seedTimes() { if (!byId('filter-start').value || !byId('filter-end').value) { const now = new Date(); now.setMinutes(Math.ceil(now.getMinutes() / 30) * 30, 0, 0); const later = new Date(now.getTime() + 3600000); byId('filter-start').value = toLocalDateTime(now); byId('filter-end').value = toLocalDateTime(later); } if (!byId('schedule-from-filter').value || !byId('schedule-to-filter').value) { const start = new Date(); start.setHours(0,0,0,0); const end = new Date(); end.setHours(23,59,0,0); byId('schedule-from-filter').value = toLocalDateTime(start); byId('schedule-to-filter').value = toLocalDateTime(end); } if (!byId('effective-from').value) byId('effective-from').value = new Date().toISOString().slice(0,10); }
function refreshBookingEstimate() { const room = state.roomsById.get(Number(byId('selected-room-id')?.value)); if (room) updateSelectedRoomLabel(room); const estimate = calculateEstimate(); byId('booking-estimate').textContent = formatMoney(estimate); byId('booking-final').textContent = formatMoney(estimate); }
function calculateEstimate() { const s = byId('reservation-start').value; const e = byId('reservation-end').value; if (!s || !e) return 0; const hours = Math.max((new Date(e).getTime() - new Date(s).getTime()) / 3600000, 1); return Number((Math.ceil(hours * 2) / 2 * 75).toFixed(2)); }

function showToast(message, type) { if (!message) return; let container = byId('toast-container'); if (!container) { container = document.createElement('div'); container.id = 'toast-container'; container.className = 'toast-container'; document.body.appendChild(container); } const toast = document.createElement('div'); toast.className = `toast toast-${type || 'success'}`; toast.textContent = message; container.appendChild(toast); setTimeout(() => { toast.classList.add('toast-hide'); setTimeout(() => toast.remove(), 250); }, 4000); }
function showLocalFeedback(id, message) { const t = byId(id); if (t) t.textContent = message; }
function setStatus(text) { const t = byId('load-status'); if (t) t.textContent = text; }
function logout() { localStorage.removeItem('eagle_token'); localStorage.removeItem('eagle_username'); localStorage.removeItem('eagle_roles'); window.location.replace('/login.html'); }
function byId(id) { return document.getElementById(id); }
function titleFor(id) { return { requester: 'Requester Workspace', frontdesk: 'Front Desk Workspace', maintenance: 'Maintenance Workspace', operations: 'Operations Workspace', admin: 'Administrator Workspace' }[id]; }
function toUsDateString(value) { if (!value) return ''; const [y,m,d] = value.split('-'); return `${m}/${d}/${y}`; }
function toIsoFromLocal(value) { return new Date(value).toISOString(); }
function formatDateTime(value) { return new Date(value).toLocaleString(); }
function formatDate(value) { return new Date(value).toLocaleDateString(); }
function formatMoney(v) { return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(Number(v || 0)); }
function toLocalDateTime(d) { const p = (x) => String(x).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`; }
function escapeHtml(v) { return String(v ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;'); }
function extractRoles(token) { try { const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'); const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '='); const payload = JSON.parse(atob(padded)); return Array.isArray(payload.roles) ? payload.roles.map((r) => r.replace('ROLE_', '')) : []; } catch (_e) { return []; } }
function allowedPanels() { if (state.roles.includes('ADMIN')) return ['requester','frontdesk','maintenance','operations','admin']; const p = []; if (state.roles.includes('REQUESTER')) p.push('requester'); if (state.roles.includes('AGENT')) p.push('frontdesk'); if (state.roles.includes('TECH')) p.push('maintenance'); if (state.roles.includes('OPS')) p.push('operations'); return p.length ? p : ['requester']; }
function defaultPanelForRoles() { return allowedPanels()[0]; }
function applyRoleVisibility() { const allowed = new Set(allowedPanels()); document.querySelectorAll('.nav-btn').forEach((b) => { b.hidden = !allowed.has(b.dataset.panel); }); document.querySelectorAll('.panel').forEach((p) => { p.hidden = !allowed.has(p.id); }); }
