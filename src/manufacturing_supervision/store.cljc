(ns manufacturing-supervision.store
  "SSoT for the ISCO-08 3122 independent manufacturing-supervision
  sole-proprietor actor. Store is a protocol injected into the
  `manufacturing-supervision.actor` StateGraph — `MemStore` is the
  default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    line     — a registered production line (:line-id, :name)
    record   — a committed operating record under a line (supervision
               note, inspection, safety-hold clearance, quality-
               control-failure override) — written ONLY via
               commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (line [s line-id])
  (records-of [s line-id])
  (ledger [s])
  (register-line! [s line])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (line [_ line-id] (get-in @a [:lines line-id]))
  (records-of [_ line-id] (filter #(= line-id (:line-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-line! [s line]
    (swap! a assoc-in [:lines (:line-id line)] line) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:lines {} :records [] :ledger []} seed)))))
