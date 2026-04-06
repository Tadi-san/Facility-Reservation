# Eagle API Spec

## Auth
- `POST /api/v1/auth/login`

## Health
- `GET /api/v1/health`
- `GET /api/v1/health/canary` (`ADMIN`)

## Catalog + Reservations
- `GET /api/v1/catalog/rooms`
- `GET /api/v1/catalog/locations`
- `GET /api/v1/catalog/room-types`
- `GET /api/v1/reservations/mine` (authenticated user)
- `GET /api/v1/reservations` (`AGENT|OPS|ADMIN`)
- `POST /api/v1/reservations`

## Front Desk (`AGENT|ADMIN`)
- `GET /api/v1/frontdesk/schedule-board`
- `POST /api/v1/frontdesk/reservations/{id}/confirm-arrival`
- `POST /api/v1/frontdesk/reservations/{id}/checkout`
- `GET /api/v1/frontdesk/banner-templates`
- `POST /api/v1/frontdesk/banner-templates`
- `GET /api/v1/frontdesk/agents`
- `GET /api/v1/frontdesk/shift-handoffs`
- `POST /api/v1/frontdesk/shift-handoffs`
- `POST /api/v1/frontdesk/notifications/refresh` (queues async job)
- `GET /api/v1/frontdesk/notifications`
- `POST /api/v1/frontdesk/notifications/{id}/dismiss`

## Operations (`OPS|ADMIN`)
- `GET /api/v1/operations/promotions`
- `POST /api/v1/operations/promotions`
- `GET /api/v1/operations/announcements`
- `POST /api/v1/operations/announcements`
- `GET /api/v1/operations/moderation-queue`
- `POST /api/v1/operations/moderation-queue`
- `POST /api/v1/operations/search/reindex` (queues async reindex job)
- `GET /api/v1/operations/search?q=...`

## Assets + Maintenance (`TECH|ADMIN`)
- `GET /api/v1/assets`
- `PATCH /api/v1/assets/{id}/status`
- `GET /api/v1/maintenance/inspections`
- `GET /api/v1/maintenance/tickets`
- `POST /api/v1/maintenance/tickets`
- `PATCH /api/v1/maintenance/tickets/{id}`
- `POST /api/v1/maintenance/tickets/{id}/close`

## Metrics (`OPS|ADMIN`)
- `GET /api/v1/metrics/scorecards`
- `POST /api/v1/metrics/scorecards`

## Users (`ADMIN`)
- `GET /api/v1/users`
- `POST /api/v1/users`
- `POST /api/v1/admin/facilities/rooms`

## Finance
- `POST /api/v1/finance/promotions/apply`
- `GET /api/v1/finance/orders` (`OPS|ADMIN`)
- `POST /api/v1/finance/orders`
- `POST /api/v1/finance/orders/{id}/pay` (`OPS|ADMIN`)
- `POST /api/v1/finance/orders/{id}/refund` (`OPS|ADMIN`)
- `POST /api/v1/finance/orders/reconcile` (`OPS|ADMIN`)
