(ns realty.facts-test
  (:require [clojure.test :refer [deftest is]]
            [realty.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-docs-satisfied-needs-every-doc
  (let [all (facts/doc-checklist "JPN")]
    (is (facts/required-docs-satisfied? "JPN" all))
    (is (not (facts/required-docs-satisfied? "JPN" (rest all))))
    (is (not (facts/required-docs-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))

(deftest netherlands-closing-is-officially-sourced-and-notary-gated
  (let [nld (facts/spec-basis "NLD")
        docs (facts/doc-checklist "NLD")]
    (is (= "Kadaster (Basisregistratie Kadaster / openbare registers)"
           (:owner-authority nld)))
    (is (re-find #"Burgerlijk Wetboek Boek 3 art. 89" (:legal-basis nld)))
    (is (= #{:notary-approved :deed-signed :kadaster-registered
             :funds-released-by-notary}
           (facts/human-gates "NLD")))
    (is (re-find #"civil-law notary" (facts/actuation-authority "NLD")))
    (is (some #(re-find #"energy-performance label" %) docs))
    (is (some #(re-find #"VvE" %) docs))
    (is (some #(re-find #"Erfpacht" %) docs))
    (is (some #(re-find #"tenancy" %) docs))
    (is (facts/required-docs-satisfied? "NLD" docs))
    (is (not (facts/required-docs-satisfied? "NLD" (rest docs))))))
