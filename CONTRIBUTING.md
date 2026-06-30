# Contributing

`cloud-itonami-6810` accepts contributions to the OSS blueprint, capability
bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/property`. This repo holds the
business blueprint and operator contracts.

```bash
clojure -X:test
clojure -M:lint
```

## Rules
- Do not commit real tenant, parcel-owner or financial data.
- Keep listings, leases and disclosures behind the Property Governor.
- Treat property workflows as high-risk: add tests for parcel identity,
  term-overlap, lease, disclosure and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
