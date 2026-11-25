# 0011. Money as a decimal string with its currency

- Status: Accepted
- Date: 2026-11-25

## Context

In the [domain model](../domain.md), an amount is a whole number of centavos, the only currency is the real, and the platform never rounds what it receives (`MON-1` to `MON-3`).

JSON has one type for numbers. Many parsers read it into a binary floating-point number, which has no exact form for most decimal fractions. Money precision is one of the test problems that the platform exists to pose.

The platform is modeled on Pix, and the API of Pix writes an amount as a string with two decimals.

## Decision

- **Money is an object with an amount and a currency:** `{ "amount": "10.50", "currency": "BRL" }`.
- **The amount is a string.** It holds reais, a point and exactly two decimals: `^(0|[1-9][0-9]{0,9})\.[0-9]{2}$`. It has no sign, no separator of thousands, no exponent and no leading zero. It has at most ten digits before the point, as in Pix.
- **The currency is a code of ISO 4217.** Only `BRL` is valid.
- **No JSON number carries money.** No schema of a contract has the type `number`. A whole number, such as a count, is an `integer`.
- **An amount is never negative. A balance can be.** A balance has the same form, with a minus sign in front when it is below zero.
- **A string that does not match breaks the contract,** and the request is refused with 400. An amount of zero where the domain wants more breaks a rule of the domain, and the request is refused with 422 ([ADR 0010](0010-problem-details-for-every-error-response.md)).

## Options considered

- **A decimal string.** Chosen. It is exact in every parser, it is the form of Pix, and a reader of a report sees an amount the way a person writes it. The form itself poses the problems that the suites are meant to show: a third decimal, a missing one, a comma, an exponent.
- **An integer of centavos.** The simplest exact form, and the unit of the domain model. It reads badly as evidence, and it leaves a test little to probe at the edge of the API.
- **A JSON number with decimals.** It is what a first draft uses. A client that reads it into a floating-point number has lost the centavo before any rule applies.
- **An amount without its currency.** It is shorter. An amount without a currency is only a number, and a second currency would then change every contract.

## Consequences

- The contracts share one schema for money. The linter forbids the type `number`.
- A service converts at its edge: a string in the API, whole centavos inside.
- A client parses the string into a decimal type. The framework gets a type for money that compares exactly.
- The fee, the limits and the holds are money, and they use the same schema.
- The largest amount of a contract is 9,999,999,999.99 reais.
