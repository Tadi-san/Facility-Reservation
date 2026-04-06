# Questions & Clarifications – Facility Reservation & Asset Operations Suite

This document records ambiguities, assumptions, and design decisions made during development of the Facility Reservation & Asset Operations Suite. Each entry describes a question, our interpretation, and the implemented solution.

---

## 1. Reservation Conflict Prevention – Granularity and Real-time Updates

**Question:** The prompt requires “reservation conflict prevention to the minute.” Does this mean conflicts are checked only at the minute level, or should we also consider overlapping seconds? Additionally, when a room’s essential equipment is marked out of service, the room becomes non‑reservable – does this apply to existing holds or only new reservations?

**Understanding:** “To the minute” implies we compare start and end times with minute precision (ignoring seconds). When a room’s equipment fails, the room should become non‑reservable immediately, and any existing holds should be flagged with a warning, not automatically cancelled.

**Solution:**
- All availability checks compare `start_time` and `end_time` using minute‑truncated timestamps.
- A background job re‑calculates room reservability whenever an asset’s status changes to “out of service.” For existing holds, the UI shows a warning banner and suggests alternative rooms.
- The room is removed from availability queries while equipment is broken.

---

## 2. Shift Handoff Notes – Persistence and Assignment

**Question:** The prompt says “shift handoff notes with timestamps and assignees so unresolved tasks persist across shifts.” Should a handoff note be tied to a specific shift or to a task/asset? How are assignees notified?

**Understanding:** Handoff notes are lightweight task records that can be assigned to a specific role or person. They are not tied to a particular shift but have a creation timestamp and a “resolved” flag. Unresolved notes appear on the dashboard of the assigned user or role.

**Solution:**
- Model: `handoff_note` (id, text, assigned_to_user_id, assigned_to_role, created_at, resolved_at, created_by).
- Front Desk Agents can create notes for themselves or other roles (e.g., Maintenance Technician). Unresolved notes are shown in a dedicated widget on the dashboard.
- No automatic notification (offline‑first), but the UI shows a badge counter.

---

## 3. Maintenance Closed‑Loop Flow – What Constitutes “Return to Normal”?

**Question:** The prompt states that every “under repair” asset must either return to “normal” or be retired as “out of service.” Should an asset be allowed to transition directly from “under repair” to “out of service” without a repair ticket closure? What about spare parts inventory?

**Understanding:** The closed loop requires that any asset marked “under repair” must have a corresponding repair ticket that records parts/labor. Once the ticket is closed, the asset can become “normal.” Retirement is a separate action that also requires a ticket (e.g., “unrepairable”).

**Solution:**
- Repair ticket states: `open`, `in_progress`, `closed`, `cancelled`.
- Asset status can only change from `under_repair` to `normal` when a repair ticket is closed with parts/labor notes.
- Retirement (`out_of_service`) can be done directly by an administrator, but a final inspection record must be attached.
- No spare parts inventory tracking in MVP (out of scope).

---

## 4. Promotions and Discount Codes – Application Logic

**Question:** The prompt mentions “free, tiered, or early‑bird” discounts. Should discounts apply to the total reservation fee or per seat? Can multiple discounts be combined? How is “early‑bird” defined (e.g., X days before start)?

**Understanding:** Discounts are applied to the optional registration fee for a reservation. Early‑bird is defined by a cutoff time relative to the reservation start time (e.g., 7 days before start). Multiple discounts are not combined by default; the best applicable discount is chosen.

**Solution:**
- Discount rules: `type` (free, tiered, early_bird), `value` (percentage or fixed amount), `min_days_before_start` (for early_bird), `tiers` (e.g., 10% for 5‑10 attendees, 20% for >10).
- During reservation checkout, the system calculates the best applicable discount and applies it.
- Discount codes are entered at the time of payment (offline order record). Admin can generate unique codes.

---

## 5. Versioned Evaluation Metric Library – Effective Date Overlap

**Question:** “Effective dates use MM/DD/YYYY, and historical versions remain viewable.” If a new metric version has an effective date that overlaps with an existing version, which one takes precedence? How are weights validated (sum to 100%)?

**Understanding:** Only one version can be active at a time. The effective date defines when a new version becomes active; the previous version is automatically archived. Weights must sum to exactly 100% at the time of creation.

**Solution:**
- Metric template version table: `id`, `name`, `weights` (JSON), `effective_date`, `created_by`, `is_active` (boolean).
- When inserting a new version with an `effective_date` in the future, it becomes active on that date (background job).
- Validation: `SUM(weights.values) = 100` on the server side; reject otherwise.
- Historical versions are read‑only and can be viewed in an audit interface.

---

## 6. Idempotent Payment Retries – Idempotency Key Scope

**Question:** “Idempotent retries using a client‑generated idempotency key” – should the key be unique per order, per request, or per payment attempt? What happens if a retry comes with a different key for the same order?

**Understanding:** The idempotency key is generated by the frontend for each logical payment attempt (e.g., after user clicks “Pay”). The key must be unique across all attempts for the same order. If a retry uses a different key, it is treated as a new attempt.

**Solution:**
- Endpoint `POST /api/orders/{id}/pay` accepts header `Idempotency-Key`.
- Backend stores a mapping of `(order_id, idempotency_key)` to the resulting payment status.
- If the same key is seen again, the same response is returned without processing.
- If a different key is used for the same order, it creates a separate payment record (allowed, but user should be warned).

---

## 7. Inspection Cadence Defaults – Override by Asset Type

**Question:** “Inspection cadence defaults (e.g., safety inspection every 30 days for critical assets).” Can an administrator override the cadence per asset or asset type? How are missed inspections flagged?

**Understanding:** The default cadence is configurable per asset category (e.g., HVAC, Fire Extinguisher). Administrators can override for individual assets. Missed inspections generate a notification for Maintenance Technicians.

**Solution:**
- Asset model includes `inspection_cadence_days` (nullable; if null, uses category default).
- A scheduled job runs daily, checking `last_inspection_date + cadence <= TODAY`. If yes, creates an inspection task and logs an alert.
- Overdue inspections are shown on the dashboard with a red badge.

---

## 8. Local CSV Reconciliation – Expected Format and Error Handling

**Question:** “Reconcile against locally imported CSV statements with checksum validation and duplicate detection.” What should the CSV contain? How are mismatches reported to the user?

**Understanding:** The CSV is expected to contain payment records from an external system (e.g., bank export). Columns: `order_id`, `amount`, `transaction_date`, `reference`. The system validates checksum of the file to avoid reprocessing the same file.

**Solution:**
- Upload endpoint `POST /api/reconciliation/upload` accepts CSV, computes SHA‑256.
- Compares each row with existing offline order records; if `order_id` matches, updates order status to `paid`.
- Generates a reconciliation report (JSON/CSV) listing matches, mismatches, and duplicates.
- Admin can download the report.

---

## 9. Role‑Based Authorization – Menu Visibility and API Enforcement

**Question:** The prompt requires distinct workspaces for five roles. Should menu items be hidden or just disabled for unauthorized users? How granular is API authorization?

**Understanding:** For security, the UI should hide features that a user cannot access (defense in depth). The backend must enforce authorization on every endpoint, not just hide menu items.

**Solution:**
- Frontend uses Angular route guards and a service that dynamically builds menus based on user’s role/permissions.
- Backend uses Spring Security method‑level annotations (`@PreAuthorize`) on each controller method.
- Permissions are derived from role but also allow fine‑grained resource checks (e.g., “can edit own reservations”).

---

## 10. Rate Limiting and Circuit Breakers – Implementation Scope

**Question:** The prompt mentions “rate limiting per user (e.g., 60 requests/minute)” and “circuit breakers around internal subsystems.” Should these be applied to all endpoints, including static assets? How is the circuit breaker reset?

**Understanding:** Rate limiting applies to authenticated API endpoints (to prevent abuse). Circuit breakers protect calls to internal services (e.g., job queue, indexing). Static assets are served with local HTTP caching, not rate‑limited.

**Solution:**
- Use Spring Cloud Circuit Breaker (Resilience4J) around calls to async job queue and search indexer.
- Rate limiting implemented via a `RateLimitFilter` that tracks user ID or IP and rejects after 60 requests per rolling minute (returns 429).
- Circuit breaker opens after 5 failures within 30 seconds; half‑open after 60 seconds.

---

## Summary of Assumptions

| Area | Assumption |
|------|------------|
| Conflict detection | Minute precision; existing holds get warnings, not auto‑cancellation |
| Handoff notes | Unresolved notes persist across shifts, appear on dashboard |
| Asset repair flow | Closed repair ticket required to return to normal |
| Discounts | Best single discount applied; no stacking |
| Metric versions | Only one active version at a time; weights sum to 100% |
| Idempotency key | Per‑payment‑attempt; different key = new attempt |
| Inspection cadence | Overridable per asset; missed inspections create tasks |
| CSV reconciliation | Checksum prevents duplicate file processing |
| Role authorization | UI hide + backend method security |
| Rate limiting | API only, not static assets; circuit breaker resets after 60s |

All decisions were made to align with the prompt’s offline‑first, on‑premise constraints while ensuring maintainability and security.