# PayFlow architecture

## Modular monolith

A single Spring Boot deployable with clear package boundaries. Controllers validate and delegate; services own business rules and transactions; repositories isolate persistence.

## Consistency

- Payment creation is idempotent (Redis + DB unique key).
- Status changes go through an explicit state machine.
- Monetary posts use `@Transactional` and balanced ledger entries.
- Refunds cannot exceed remaining refundable amount or apply to blocked/failed payments.

## Async boundary

Kafka is used for notifications and webhook fan-out — not for the core authorize/capture decision path, which remains synchronous inside the payment service so API clients get a definitive status.

## Frontend

Authenticated shell shares one design system (glass cards, purple accents, dark navy). Public landing and checkout reuse the same tokens so the product feels continuous.
