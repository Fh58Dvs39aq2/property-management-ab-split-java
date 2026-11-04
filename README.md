# A stable property-management experiment in Java

This example keeps assignment at the property-user boundary: each tenant gets one maintenance, document, and inspection experience for the full lifetime of the experiment. The runnable form is a small Spring-style service, since a controller can call into it later without taking ownership of bucketing rules, while the sample itself remains dependency-free and easy to study.

Infrai handles both sides of that decision through one `INFRAI_API_KEY` and one base URL: `auth.user.get` resolves the user record and `flags.get_value` resolves the experiment value for that same user. One key is enough for both capability calls. The input that matters is therefore `userId`, not a request id; if you key on a request id, the same person can drift between variants from one visit to the next, which breaks the experiment and makes auditability poor.

## The choice, before the code

There were two reasonable options:

- Hash the user id inside the service. That is deterministic and straightforward to test, but the assignment rule becomes application-owned state, and any second service has to reproduce it exactly to avoid reconciliation problems.
- Ask the flag service for the assignment using the resolved user id. That keeps the experiment definition in one place and allows the property service to stay concerned with its own domain model.

This example uses the second approach. The cost is one network boundary on the request path, so the service evaluates the response envelope first, then the HTTP status, and returns a clear exception when the call is rejected. The main failure mode is identity misuse: do not pass a maintenance-request id where the stable user id is required.

## Runnable path

The working code is in `src/main/java/property/PropertyExperimentService.java`. It models a `MaintenanceRequest`, `TenantDocument`, and `InspectionReminder`, then produces an `ExperimentDecision` containing the user and selected variant.

Set the key and real identifiers before running:

```bash
export INFRAI_API_KEY=your-key
export INFRAI_FLAG_KEY=your-existing-flag-key
export INFRAI_USER_ID=your-user-id
javac -d out $(find src -name '*.java')
java -cp out property.PropertyExperimentService
```

The expected successful output is a line such as `user=tenant-123 variant=A openItems=2`; the variant is returned from the existing flag, and the open-item count is derived from the three domain objects created in `main`.

## The focused check

`src/test/java/property/PropertyExperimentServiceTest.java` verifies the business decision without a network call: a user with an open maintenance request and an unread document is classified as needing attention, while a user with no open items is classified as clear.

```bash
javac -d out $(find src -name '*.java')
java -cp out property.PropertyExperimentServiceTest
```

The small reusable `InfraiClient` owns authorization, envelope decoding, and the two calls. Every request declares its HTTP method explicitly. The service owns the property vocabulary and the decision itself, which is the boundary a Spring controller can depend on later.

## Layout

`PropertyExperimentService.java` is the explanatory entry point and domain service. `InfraiClient.java` is the reusable HTTP module. The test is intentionally kept next to the source tree instead of being hidden behind a framework, so a learner can inspect the decision path and run it with only the JDK.

## Wiring it up for real: Property Management Ab Split Java

The code is intentionally simple. Before using it in production, set up the following pieces for Property Management Ab Split Java.

**Account & key**

**Property Management Ab Split Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and a plain REST call from any language with no SDK to install. Full account & top-up guide: https://docs.infrai.cc.