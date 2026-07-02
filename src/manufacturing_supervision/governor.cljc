(ns manufacturing-supervision.governor
  "ManufacturingSupervisionGovernor — the independent safety/
  traceability layer for the ISCO-08 3122 independent manufacturing-
  supervision actor. Wired as its own `:govern` node in
  `manufacturing-supervision.actor`'s StateGraph, downstream of
  `:advise` — the Advisor has no notion of line provenance or safety-
  hold/quality-override risk, so this MUST be a separate system able
  to reject a proposal (itonami actor pattern, per ADR-2607011000 /
  CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. line provenance  — the request's line must be registered.
    2. no-actuation       — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: clearing a safety-hold on production
  equipment and overriding a quality-control failure always require
  human sign-off):
    3. :op :clear-safety-hold.
    4. :op :override-quality-failure.
    5. low confidence (< `confidence-floor`)."
  (:require [manufacturing-supervision.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:clear-safety-hold :override-quality-failure})

(defn- hard-violations [{:keys [proposal]} line-record]
  (cond-> []
    (nil? line-record)
    (conj {:rule :no-line :detail "未登録 line"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `manufacturing-supervision.store/Store`.
  Returns `{:ok? bool :violations [...] :confidence n :hard? bool
  :escalate? bool}`."
  [request context proposal store]
  (let [line-record (store/line store (:line-id request))
        hard (hard-violations {:proposal proposal} line-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
