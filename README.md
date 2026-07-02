# cloud-itonami-isco-3122

Open Occupation Blueprint for **ISCO-08 3122**: Manufacturing Supervisors.

This repository designs a forkable OSS business for an independent manufacturing supervisor: a floor-monitoring robot performs equipment-status checks and safety-walkthrough capture under a governor-gated actor, so the practice keeps its own supervision and safety records instead of renting a closed shop-floor management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a floor-monitoring robot performs equipment-status checks and safety-walkthrough evidence capture under an actor that proposes
actions and an independent **Manufacturing Supervision Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
clearing a safety-hold on production equipment, or overriding a quality-control failure) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production plan + crew roster + safety checklist
        |
        v
Supervision Advisor -> Manufacturing Supervision Governor -> supervise/inspect, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3122`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
