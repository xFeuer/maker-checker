# contracts

The OpenAPI specifications of the platform's APIs.

- They are written before the code.
- `platform/` implements them.
- `automation/` tests against them.

Each service has its own directory: `contracts/<service>/openapi.yaml`.

| Service | Specification |
|---|---|
| `account-service` | [openapi.yaml](account-service/openapi.yaml) |
