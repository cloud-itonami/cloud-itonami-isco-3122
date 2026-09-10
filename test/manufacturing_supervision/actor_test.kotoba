(ns manufacturing-supervision.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [manufacturing-supervision.actor :as actor]
            [manufacturing-supervision.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-line! st {:line-id "line-1" :name "Assembly Line A"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:line-id "line-1" :op :inspect :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "line-1"))))))

(deftest holds-on-unregistered-line-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:line-id "no-such-line" :op :inspect :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-line")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; safety-hold clearance always escalates (governor invariant)
        request {:line-id "line-1" :op :clear-safety-hold :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "line-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "line-1")))))))
