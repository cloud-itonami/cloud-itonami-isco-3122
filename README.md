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

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144`, `-2320`, `-2411`, `-2422`, `-2431`, `-2621`
and `-2634`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/manufacturing_supervision/store.cljc` — `Store` protocol +
  `MemStore`: registered lines, committed records, an append-only
  audit ledger.
- `src/manufacturing_supervision/advisor.cljc` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a supervision
  operation from a request; `llm-advisor` wraps a
  `langchain.model/ChatModel` — either way the advisor only ever
  produces a `:propose`-effect proposal, never a committed record, and
  LLM parse failures always yield `confidence 0.0` (forces escalation,
  never fabricated confidence).
- `src/manufacturing_supervision/governor.cljc` —
  `ManufacturingSupervisionGovernor/check`: a pure function, wired as
  its own `:govern` node. Hard invariants (unregistered line, a
  proposal whose `:effect` isn't `:propose`) always route to `:hold`.
  Escalation invariants (`:clear-safety-hold`,
  `:override-quality-failure`, or low advisor confidence) always route
  to `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that clearing a safety-hold on production equipment and overriding a
  quality-control failure always require human sign-off.
- `src/manufacturing_supervision/actor.cljc` — `build-graph`,
  `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring
  itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
