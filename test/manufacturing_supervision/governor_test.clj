(ns manufacturing-supervision.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [manufacturing-supervision.store :as store]
            [manufacturing-supervision.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-line! st {:line-id "line-1" :name "Assembly Line A"})
    st))

(deftest ok-on-clean-inspect
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:line-id "line-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-line
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:line-id "no-such-line"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-line (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :inspect :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:line-id "line-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-safety-hold-clearance
  (let [st (fresh-store)
        proposal {:op :clear-safety-hold :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:line-id "line-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-quality-failure-override
  (let [st (fresh-store)
        proposal {:op :override-quality-failure :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:line-id "line-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:line-id "line-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:line-id "line-1" :op :supervise})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "line-1"))))
    (is (= 1 (count (store/ledger st))))))
