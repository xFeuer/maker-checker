# 0012. An idempotency key for every POST

- Status: Accepted
- Date: 2026-11-25

## Context

A client that gets no answer cannot know whether its request arrived, so it sends the request again. The second arrival must not move money a second time. The [domain model](../domain.md) demands that of a client (`PAY-5`), of the services among themselves (`LED-7`) and of the clearing network (`PAY-13`, `NET-5`).

HTTP makes `GET`, `PUT` and `DELETE` safe to repeat. `POST` is not.

The IETF has a draft for a header that makes it so: `draft-ietf-httpapi-idempotency-key-header`. Revision 08 is from October 2026. It is not an RFC yet.

## Decision

- **Every `POST` takes an `Idempotency-Key` header.** The linter checks that.
- **Every other change is a `PUT` or a `DELETE` on a URL that names the thing,** such as the hold of a payment. `PATCH` is not used.
- **The header follows the draft.**
  - The key is a string in double quotes.
  - A request without a valid key is refused with 400.
  - A key that returns with another request is refused with 422.
  - A repeat that arrives while the first request is still being handled is refused with 409.
- **The client chooses the key:** 1 to 64 letters, digits, hyphens or underscores. A UUID is a good one.
- **A key belongs to the customer of the caller, and it never expires.**
- **A repeat of the same request gets the first response again:** the same status and the same body, whatever has happened since. The same request is the same operation with the same body, compared as JSON, so its formatting does not count.

## Options considered

The mechanism:

- **The header of the IETF draft.** Chosen. Its name, its syntax and its three status codes come from one text. That text can still change before it is an RFC.
- **A key without quotes,** as many APIs take it today. It is familiar. It follows no text, so the contracts would have to define all of it.
- **A reference in the body,** chosen by the client. It would also work outside HTTP. It mixes the repeat of a request with the data of a payment, and every operation would need its own field.
- **`PUT` everywhere, with an identifier that the client chooses.** It needs no header. The platform names its own payments, from their first moment (`PAY-6`).

The owner of a key:

- **The customer.** Chosen. A second user or a second process of the same company can repeat a request, and no customer meets the key of another one (`CUS-4`).
- **The user.** A repeat by a colleague would create a second payment.
- **The whole platform.** A key of one customer would tell another customer that it is taken.

The life of a key:

- **Forever.** Chosen. `PAY-5` names no end, and an API that forgets a key can pay twice.
- **A day.** The storage would have an end. A late repeat would pay again.

The answer to a repeat:

- **The first response again.** Chosen. A client cannot tell a repeat from the first arrival, and that is what a retry needs.
- **The payment as it is now.** There would be nothing to store but the key. A client would then meet two kinds of success for one request.

## Consequences

- `payment-service` keeps the key, the request and the first response, for each customer, and it never deletes them.
- Two requests with one key can arrive together. One of them creates the payment. The other one gets 409, or the first response once it exists.
- The suites get their idempotency tests from this: a repeat, a repeat with another body, two at once, and one key from two customers.
- A delivery of the clearing network can be a `PUT` on a URL with the end-to-end ID. Then it needs no key.
- The decision has to be read again when the draft becomes an RFC.
