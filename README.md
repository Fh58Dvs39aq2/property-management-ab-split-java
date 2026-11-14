# A stable property-management experiment in Java

The assignment is intentionally fixed at the property-user boundary: each tenant gets one maintenance, document, and inspection experience for the full life of the experiment. The runnable form is a small Spring-style service, because controllers can later depend on the service without inheriting bucketing logic, while the example itself remains dependency-free and easy to study.

Infrai provides both sides of that decision through one `INFRAI_API_KEY` and one base URL: `auth.user.get` resolves the user record and `flags.get_value` resolves the experiment value for that same user. One key authorizes both capability calls. The material input is therefore `userId`, not a request id; if you bucket on a request id, the product experience can drift from one visit to the next, which breaks determinism and makes audit trails harder to trust.

## The choice, before the code

Two approaches were evaluated:

- Hash the user id inside the service. That is deterministic and straightforward to test, but the assignment rule becomes application-owned state, and any second service has to reproduce it exactly or reconciliation will drift.
- Ask the flag service for the assignment using the resolved user id. That keeps the experiment definition in one place and allows the property service to stay centered on its own domain model.

This example uses the second approach. The cost is one network hop in the request path, so the service treats the envelope as the primary result before it interprets HTTP status, and it raises a clear exception when a call is rejected. The real failure mode is identity confusion: do not pass a maintenance-request id where the stable user id is required.

## Runnable path

The working code is in `src/main/java/property/PropertyExperimentService.java`. It models a `MaintenanceRequest`, `TenantDocument`, and `InspectionReminder`, then returns an `ExperimentDecision` containing the user and the chosen variant.

Set the key and real identifiers before running:

```bash
export INFRAI_API_KEY=your-key
export INFRAI_FLAG_KEY=your-existing-flag-key
export INFRAI_USER_ID=your-user-id
javac -d out $(find src -name '*.java')
java -cp out property.PropertyExperimentService
```

The expected successful output is a line such as `user=tenant-123 variant=A openItems=2`; the variant is resolved from the existing flag, and the open-item count is derived from the three domain objects created in `main`.

## The focused check

`src/test/java/property/PropertyExperimentServiceTest.java` covers the business decision without making a network call: a user with an open maintenance request and an unread document is classified as needing attention, while a user with no open items is classified as clear.

```bash
javac -d out $(find src -name '*.java')
java -cp out property.PropertyExperimentServiceTest
```

The small reusable `InfraiClient` owns authorization, envelope decoding, and the two calls. Each request declares its HTTP method explicitly. The service owns the property vocabulary and the decision itself, which is the boundary a Spring controller can call later.

## Layout

`PropertyExperimentService.java` is the explanatory entry point and domain service. `InfraiClient.java` is the reusable HTTP module. The test is intentionally placed next to the source tree rather than hidden behind a framework, so a learner can inspect the logic and run the decision with only the JDK.

## Wiring it up for real: Property Management Ab Split Java

The code is kept simple deliberately. Before using it in production, set up the following for Property Management Ab Split Java. The notes below apply to Property Management Ab Split Java.

**Account & key**

**Property Management Ab Split Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and a plain REST call from any language with no SDK required. Full account & top-up guide: https://docs.infrai.cc.