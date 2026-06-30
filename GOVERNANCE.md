# Governance

`cloud-itonami-6810` is an OSS open-business blueprint for community
real-estate agency.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- listings without an identified parcel can never publish.
- overlapping lease terms can never be committed.
- the Property Governor remains independent of the advisor.
- hard policy violations (force-overlap, unauthorized disclosure) cannot be overridden by human approval.
- every listing, lease, handover and disclose path is auditable.
- tenant personal data and credentials stay outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:
- bypassing listing or lease policy checks
- mishandling tenant data
- misrepresenting certification status
- failing to respond to security incidents
