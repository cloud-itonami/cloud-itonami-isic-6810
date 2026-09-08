(ns realty.observation
  "The observation contract over the 6810 actor's world — how a reading of an
  OFFICIAL source (land registry, cadastre, statistics agency) becomes a
  provenance-preserving, re-observable claim about RECORDED property events,
  and what such a claim can never become.

  This layer is deliberately SEPARATE from `realty.registry` (the actor's own
  closing drafts) and from the actor's execution path: drafts are what this
  actor prepares under a human gate; observations are what external official
  sources show. An observation is never an execution, never a draft, and
  never feeds back into a closing decision.

  ONE CONTRACT, TWELVE PARTS. Every observation run against this contract
  produces the same shapes, so a refresh can be compared with a prior refresh
  and a reader can audit what was seen, when, from where, and what was NOT
  seen:

  1. SOURCE RECEIPT — `receipt`: the frozen record of one source reading
     (https URL, class, language, issuing entity, jurisdiction, sha256
     content-hash, observed-at vs asserted-at, method). A receipt without a
     hash is a rumor. The id must derive from content-hash + observed-at;
     a re-validated receipt whose id no longer derives that way is tampering,
     not history.
  2. TYPED SUBJECT + EVENTS — one subject (a property identified ONLY by a
     jurisdiction-scoped registry identifier — cadastral parcel id, title
     number, land-registry id, unique property reference — never by a street
     address), bound to typed recorded events (title transfer, mortgage
     registration and discharge, encumbrance, amendment). Party data is refused
     BY CONSTRUCTION: the scope's privacy boundaries forbid natural-person
     owner identification and personal residential-address linkage, so an
     observation carrying an address or parties cannot even exist here.
  3. MEASUREMENT WINDOW — every observation states `{:from :to}`; recorded
     events must fall inside it; a claim without a window is un-dateable
     and refused.
  4. CURRENCY AND AREA BASIS — a monetary figure carries its currency and
     the date its amount is nominal at; a dimensional figure carries its
     unit; both carry the verbatim raw transcription. Neither is ever
     normalized into a comparable number here (amounts at different dates
     and areas under different measurement standards are NOT interchangeable).
  5. METHOD / VERSION — every artifact names `closing-observation/1`; there
     is no model anywhere in this path (deterministic validation only).
  6. MISSINGNESS / COVERAGE — flags come from a closed vocabulary; a
     jurisdiction with no spec-basis entry in `realty.facts` must carry
     `:jurisdiction-spec-basis-absent`, and a recorded transfer with no
     price-paid figure must carry `:price-unavailable` — silence would claim
     completeness, and completeness is not claimed anywhere.
  7. DERIVED OBSERVATION — `window-observation` (per-subject in-window event
     COUNTS + verbatim registration references) and `coverage-observation`
     (counts over the `realty.facts` catalog). A COUNT, never a price, a
     trend, a valuation or a market measure.
  8. REFRESH HISTORY — `observe` / `refresh` / `refresh-delta` (pure,
     append-only in data): re-observations link to what they refresh via
     `:obs/refresh-of`; the same observation id can never be recorded twice;
     a cross-subject refresh link is refused at append time, not just at
     readout; the delta is verbatim-level (added / removed / changed figures
     carried IN FULL on both sides, gap movement, both generations' receipt
     ids) and computes no numeric difference anywhere.
  9. HYAKKA PROPOSAL — `hyakka-proposal`: the exact claim shape proposed to
     the `fudosan` corpus, one per figure plus one per observed subject. The
     proposal carries the receipts, the verbatim values, their bases, the
     gaps, the scope's epistemic and privacy boundaries, and `:no-model
     true`. It is DATA for the proposing run to carry — this contract sends
     nothing anywhere. Prop names are contract-local and NOT yet registered
     in the Hyakka ontology; the proposal says so instead of quietly
     borrowing someone else's prop.
  10. QUERY / READBACK — `readback` (the latest observation for a subject at
      or before an as-of, re-validating everything it returns and refusing
      tampered receipts; a miss is reported as a miss, never defaulted) and
      `readback-chain` (the full `:obs/refresh-of` lineage, oldest first,
      every generation revalidated, refusing a truncated, cyclic or
      cross-subject lineage, with pairwise deltas aligned to the chain).
  11. HISTORY DISCIPLINE — duplicate observation ids are refused; the same
      subject id may never be re-typed under a different entity type (a
      parcel is not a building and a building is not a dwelling unit);
      anything that is not this contract's observation is refused.
  12. REFUSALS — every rule above refuses loudly (`ex-info` with a
      `:refusal/code`) instead of degrading quietly. `refusals` documents
      the codes.

  WHAT THIS CONTRACT NEVER PRODUCES: a valuation, a market score, a ranking
  of properties / neighbourhoods / jurisdictions, an ownership claim about
  any person (a registered title is not beneficial ownership and parties are
  not carried at all), or investment advice of any kind. Recorded-transaction
  prices are observations of what a source disclosed — not current market
  value, not a property valuation service, and not an offer."
  (:require [clojure.set :as set]
            [kotoba.lang.text :as str]
            [realty.facts :as facts]))

;; --- identity --------------------------------------------------------------

(def contract-version
  "Every receipt, observation, derived row and proposal carries this string."
  "closing-observation/1")

;; --- refusals (loud, never silent degradation) ------------------------------

(defn- refuse
  [code message]
  (throw (ex-info message {:refusal/code code
                           :refusal/contract contract-version
                           :refusal/message message})))

(defn refusal-code
  "The `:refusal/code` of an `ex-info` thrown by this contract, or nil."
  [e]
  (:refusal/code (ex-data e)))

(def refusals
  "Every refusal code this contract can raise, with what it means. A caller
  catching an exception without a code here did not come from this contract."
  #{:receipt/missing-field
    :receipt/url-not-https
    :receipt/unknown-source-class
    :receipt/unknown-method
    :receipt/bad-content-hash
    :receipt/bad-observed-at
    :receipt/bad-asserted-at
    :receipt/observed-before-asserted
    :receipt/bad-jurisdiction
    :receipt/stale-id
    :figure/missing-field
    :figure/empty-raw
    :figure/unknown-kind
    :figure/monetary-without-currency
    :figure/bad-currency
    :figure/monetary-without-nominal-at
    :figure/bad-nominal-at
    :figure/dimensional-without-area-unit
    :figure/unknown-area-unit
    :figure/bad-amount
    :figure/bad-value
    :subject/missing-field
    :subject/unknown-entity-type
    :subject/unknown-identifier-class
    :subject/address-refused
    :observation/missing-field
    :observation/unknown-event-type
    :observation/missing-event-field
    :observation/bad-window
    :observation/window-inverted
    :observation/event-outside-window
    :observation/cross-subject-event
    :observation/address-refused
    :event/party-data-refused
    :observation/no-receipts
    :observation/cross-jurisdiction-receipt
    :observation/unknown-figure-receipt
    :observation/unknown-event-ref
    :observation/unknown-missingness-flag
    :observation/silence-claims-completeness
    :observation/refresh-of-unknown
    :observation/refresh-of-self
    :observation/refresh-of-cross-subject
    :history/duplicate-observation-id
    :history/not-an-observation
    :history/entity-type-conflict
    :delta/cross-subject
    :coverage/bad-recorded-at
    :readback/tampered-receipt
    :readback/broken-lineage
    :readback/cyclic-lineage
    :readback/chain-cross-subject
    :proposal/not-an-observation})

;; --- vocabulary (closed; extending one is a contract change) ----------------

(def receipt-classes
  "The closed source-class vocabulary a receipt may carry. The first block is
  taken verbatim from the workspace real-estate scope's `:source-policy
  :allow`; the last two are registry-adjacent publishers (government portals
  and programme operators also publish registry-derived data) and have NO
  scope class — see `unmapped-in-scope`. Unknown classes are refused, not
  guessed."
  #{:official-land-registry :official-cadastre :official-statistics-agency
    :official-tax-authority :official-regulator :official-securities-filing
    :official-stock-exchange-filing :official-municipal-planning-authority
    :official-central-bank :fund-first-party :manager-first-party
    :official-programme-operator :official-government-portal})

(def unmapped-in-scope
  "Receipt classes with NO counterpart in the scope's `:source-policy :allow`
  vocabulary. A proposal carrying one of these is flagged
  `:proposal/source-class-unmapped` — surfaced, never silently relabelled
  into a scope class it does not have."
  #{:official-programme-operator :official-government-portal})

(def receipt-methods
  "How the bytes were read. `:verbatim-citation` — page/PDF read and quoted;
   `:official-api` — structured response from the publisher's own endpoint;
   `:archived-receipt` — hash over bytes archived by a previous run."
  #{:verbatim-citation :official-api :archived-receipt})

(def subject-entity-types
  "What a subject IS. From the scope's entity types: a parcel is not a
  building and a building is not a dwelling unit; one observation carries
  exactly one entity type for its subject id."
  #{:parcel :building :dwelling-unit})

(def identifier-classes
  "How a subject is identified. Registry identifiers ONLY — a street address
  is never an identifier here (privacy boundary: no personal
  residential-address linkage)."
  #{:cadastral-parcel-id :title-number :land-registry-id
    :unique-property-reference})

(def event-types
  "The closed vocabulary of RECORDED events this contract can observe. All of
  them are registry acts (they happened in a registry), not market acts
  (nobody here claims what anything is worth)."
  #{:title-transfer-recorded
    :mortgage-registered
    :mortgage-discharged
    :encumbrance-registered
    :amendment-recorded})

(def figure-kinds
  "The closed figure vocabulary. `:price-paid` is a disclosed recorded
  transaction price — an observation, NOT a current market value.
  `:parcel-area` is a dimension under a named measurement unit — NOT
  interchangeable with another unit's figure."
  #{:price-paid :parcel-area})

(def area-units
  "Closed area-unit vocabulary. Units are carried verbatim; no conversion
  exists in this contract."
  #{:m2 :sqft :jo :tsubo :pyeong :acre})

(def missingness-flags
  "The closed missingness vocabulary an observation may declare. Chosen from
  the gaps this actor's world actually has; extending it is a contract
  change, not a free-form field."
  #{:price-unavailable
    :area-unavailable
    :date-unavailable
    :registry-extract-partial
    :source-not-text-extractable
    :identifier-unverified
    :jurisdiction-spec-basis-absent})

(def epistemic-boundaries
  "The scope's epistemic boundaries this contract operates under, quoted as
  data so every proposal carries them instead of assuming them."
  #{:recorded-transaction-price-is-not-current-market-value
    :official-or-assessed-valuation-is-not-market-price
    :registered-title-is-not-beneficial-ownership
    :area-figures-differ-by-measurement-standard-and-are-not-interchangeable
    :currency-amounts-are-nominal-at-their-own-date-not-comparable-without-a-stated-basis
    :a-parcel-is-not-a-building-and-a-building-is-not-a-dwelling-unit
    :missing-is-unmeasured
    :worldwide-is-a-coverage-goal-not-a-completeness-claim})

(def privacy-boundaries
  "The scope's privacy boundaries enforced BY CONSTRUCTION here (see
  `refusals`: addresses and party data cannot enter an observation)."
  #{:legal-entities-and-public-professional-roles-only
    :no-natural-person-owner-identification
    :no-personal-residential-address-linkage
    :no-occupancy-or-household-data
    :no-personal-contact-data
    :no-personal-wealth
    :no-mortgage-borrower-identification
    :no-sensitive-trait-inference
    :no-neighbourhood-desirability-ranking})

;; --- shape helpers ----------------------------------------------------------

(def date-re
  "ISO calendar date, the only date shape this contract accepts."
  #"^\d{4}-\d{2}-\d{2}$")

(defn- valid-date? [s] (and (string? s) (re-find date-re s)))

(defn- nonempty-str? [x] (and (string? x) (not (str/blank? x))))

(defn- iso-locale-date?
  "A date string with no timezone ambiguity: plain YYYY-MM-DD compares
  correctly as a string, which is all this contract ever does with dates."
  [s] (valid-date? s))

(defn- frozen
  "What 'frozen' means concretely: a plain hash-map copy. Callers keep the
  original; the contract returns a value that shares nothing mutable."
  [m] (into {} m))

;; --- 1. source receipt ------------------------------------------------------

(defn receipt-id
  "The only id a receipt may carry: derived from its content-hash and
  observed-at. An id that no longer derives is an edited receipt."
  [{:receipt/keys [content-hash observed-at]}]
  (when (and (nonempty-str? content-hash) (valid-date? observed-at))
    (str "receipt:" (subs content-hash 0 16) ":" observed-at)))

(defn receipt
  "Validate + freeze one source receipt. Pure function — reads nothing,
  fetches nothing. The hash is the sha256 of the observed bytes as recorded
  by the reading run; this contract cannot recompute it (it never had the
  bytes) so it validates its FORM and refuses a receipt whose stored id no
  longer derives from hash + observed-at (:receipt/stale-id — edited after
  freezing is refused, never re-branded)."
  [{:receipt/keys [source-url source-class source-language issuing-entity
                   jurisdiction content-hash observed-at asserted-at method]
    :as r}]
  (when-not (and (map? r)
                 (nonempty-str? source-url) (nonempty-str? source-language)
                 (nonempty-str? issuing-entity) (nonempty-str? content-hash)
                 (some? source-class) (some? method))
    (refuse :receipt/missing-field
            "receipt: source-url, source-class, source-language,
             issuing-entity, content-hash and method are all required"))
  (when-not (str/starts-with? source-url "https://")
    (refuse :receipt/url-not-https
            (str "receipt: source-url must be https, got: " source-url)))
  (when-not (contains? receipt-classes source-class)
    (refuse :receipt/unknown-source-class
            (str "receipt: unknown source-class " source-class)))
  (when-not (contains? receipt-methods method)
    (refuse :receipt/unknown-method
            (str "receipt: unknown method " method)))
  (when-not (re-find #"^[0-9a-f]{64}$" content-hash)
    (refuse :receipt/bad-content-hash
            "receipt: content-hash must be lowercase sha256 hex (64 chars)"))
  (when-not (iso-locale-date? observed-at)
    (refuse :receipt/bad-observed-at
            "receipt: observed-at must be YYYY-MM-DD"))
  (when-not (iso-locale-date? asserted-at)
    (refuse :receipt/bad-asserted-at
            "receipt: asserted-at must be YYYY-MM-DD"))
  (when (neg? (compare observed-at asserted-at))
    (refuse :receipt/observed-before-asserted
            "receipt: observed-at precedes asserted-at — the reading cannot
             precede the source's own assertion"))
  (when-not (nonempty-str? jurisdiction)
    (refuse :receipt/bad-jurisdiction "receipt: jurisdiction required"))
  (let [expected (receipt-id r)]
    (when-not (= expected (:receipt/id r))
      (refuse :receipt/stale-id
              (str "receipt: stored id does not derive from content-hash + "
                   "observed-at (expected " expected ")"))))
  (frozen (assoc r :receipt/contract-version contract-version)))

(defn revalidate-receipt
  "Readback-time revalidation of a frozen receipt: the same rules, so a
  receipt edited after freezing is refused on the way out too."
  [r]
  (if (and (map? r) (= contract-version (:receipt/contract-version r)))
    (receipt r)
    (refuse :readback/tampered-receipt
            "readback: artifact is not this contract's receipt")))

;; --- 2. figures -------------------------------------------------------------

(defn figure
  "Validate one verbatim figure. `:figure/raw` is the transcription exactly
  as the source states it — required, never normalized. A `:price-paid`
  figure carries :amount (integer), :currency (ISO-4217 alpha-3) and
  :nominal-at (the date the amount is nominal at). A `:parcel-area` figure
  carries :value and :unit from the closed area vocabulary. `:figure/source`
  is the receipt id backing it; `:figure/event-ref` optionally names the
  recorded event it belongs to."
  [{:figure/keys [kind raw amount value currency nominal-at unit] :as f}]
  (when-not (and (map? f) (contains? figure-kinds kind))
    (refuse :figure/unknown-kind (str "figure: unknown kind " kind)))
  (when-not (nonempty-str? raw)
    (refuse :figure/empty-raw
            "figure: raw verbatim transcription required (no raw = not read)"))
  (case kind
    :price-paid (do (when-not (and (int? amount) (not (neg? amount)))
                      (refuse :figure/bad-amount
                              "figure: price-paid amount must be a >= 0 integer"))
                    (when-not (and (nonempty-str? currency)
                                   (re-find #"^[A-Z]{3}$" currency))
                      (refuse :figure/monetary-without-currency
                              "figure: price-paid needs ISO-4217 currency"))
                    (when-not (iso-locale-date? nominal-at)
                      (refuse :figure/monetary-without-nominal-at
                              "figure: price-paid needs a nominal-at date —
                               an amount without its own date is not
                               comparable with anything")))
    :parcel-area (do (when-not (number? value)
                       (refuse :figure/bad-value
                               "figure: parcel-area value must be numeric"))
                     (when-not (contains? area-units unit)
                       (refuse :figure/dimensional-without-area-unit
                               (str "figure: parcel-area needs a unit from "
                                    area-units)))))
  (when-not (nonempty-str? (:figure/source f))
    (refuse :figure/missing-field "figure: source receipt id required"))
  (frozen f))

;; --- 3. subject -------------------------------------------------------------

(defn subject
  "Validate one observation subject: a property identified ONLY by a
  jurisdiction-scoped registry identifier under a closed identifier class
  and a closed entity type. A street address is refused wherever it tries
  to enter — the subject is the registry reference, not a home."
  [{:subject/keys [id entity-type identifier-class] :as s}]
  (when-not (nonempty-str? id)
    (refuse :subject/missing-field "subject: registry identifier required"))
  (when-not (contains? subject-entity-types entity-type)
    (refuse :subject/unknown-entity-type
            (str "subject: unknown entity-type " entity-type)))
  (when-not (contains? identifier-classes identifier-class)
    (refuse :subject/unknown-identifier-class
            (str "subject: unknown identifier-class " identifier-class)))
  (when (some #(contains? s %) [:subject/address :subject/street-address
                                :subject/location :subject/postal-address])
    (refuse :subject/address-refused
            "subject: address keys are refused — a registry reference, not
             a residential address linkage"))
  (frozen s))

;; --- 4. observation ---------------------------------------------------------

(defn observation
  "Validate + freeze one observation of ONE subject over ONE window:
  receipts (>= 1), typed recorded events (each inside the window and bound
  to the subject), verbatim figures with bases, closed-vocabulary
  missingness. Refuses party data and addresses by construction, refuses a
  jurisdiction with no `realty.facts` spec-basis that does not carry
  `:jurisdiction-spec-basis-absent`, and refuses a recorded transfer with
  neither a covering price-paid figure nor `:price-unavailable` — silence
  would claim completeness."
  [{:obs/keys [id subject events receipts figures missingness window
               refresh-of recorded-at]
    :as o}]
  (when-not (nonempty-str? id)
    (refuse :observation/missing-field "observation: :obs/id required"))
  (when-not (iso-locale-date? recorded-at)
    (refuse :observation/missing-field
            "observation: :obs/recorded-at must be YYYY-MM-DD"))
  (let [sid (:subject/id subject)
        jurisdiction (:obs/jurisdiction o)]
    (when-not (nonempty-str? jurisdiction)
      (refuse :observation/missing-field
              "observation: :obs/jurisdiction required"))
    (subject subject)
    (when-not (and (map? window)
                   (iso-locale-date? (:from window))
                   (iso-locale-date? (:to window)))
      (refuse :observation/bad-window "observation: window must be {:from :to}, YYYY-MM-DD"))
    (when (pos? (compare (:from window) (:to window)))
      (refuse :observation/window-inverted
              "observation: window :from is after :to"))
    (when (empty? receipts)
      (refuse :observation/no-receipts
              "observation: at least one source receipt required"))
    (doseq [r receipts]
      (revalidate-receipt r)
      (when-not (= jurisdiction (:receipt/jurisdiction r))
        (refuse :observation/cross-jurisdiction-receipt
                (str "observation: receipt " (:receipt/id r)
                     " is from " (:receipt/jurisdiction r)
                     ", observation is about " jurisdiction))))
    (doseq [e events]
      (when-not (contains? event-types (:event/type e))
        (refuse :observation/unknown-event-type
                (str "observation: unknown event type " (:event/type e))))
      (when-not (nonempty-str? (:event/registration-ref e))
        (refuse :observation/missing-event-field
                "observation: event needs a registration-ref"))
      (when-not (nonempty-str? (:event/issuer e))
        (refuse :observation/missing-event-field
                "observation: event needs an issuing entity (registry)"))
      (when-not (iso-locale-date? (:event/recorded-at e))
        (refuse :observation/missing-event-field
                "observation: event needs a YYYY-MM-DD recorded-at"))
      (when (or (contains? e :event/parties) (contains? e :event/party-names)
                (contains? e :event/parties-anon))
        (refuse :event/party-data-refused
                "observation: party data is refused — no natural-person
                 identification, no borrower identification"))
      (when-not (or (string? (:event/subject-id e)) (keyword? (:event/subject-id e)))
        (refuse :observation/missing-event-field
                "observation: event must name its subject"))
      (when-not (= sid (:event/subject-id e))
        (refuse :observation/cross-subject-event
                (str "observation: event " (:event/registration-ref e)
                     " belongs to " (:event/subject-id e)
                     ", observation is about " sid)))
      (when-not (and (neg? (compare (:event/recorded-at e) (:to window)))
                     (pos? (compare (:event/recorded-at e) (:from window))))
        (refuse :observation/event-outside-window
                (str "observation: event " (:event/registration-ref e)
                     " recorded " (:event/recorded-at e)
                     " is outside the window")))
      )
    (doseq [f figures]
      (figure f)
      (when-not (some #(= (:figure/source f) (:receipt/id %)) receipts)
        (refuse :observation/unknown-figure-receipt
                (str "observation: figure cites receipt "
                     (:figure/source f) " which is not in this observation")))
      (when-let [eref (:figure/event-ref f)]
        (when-not (some #(= eref (:event/registration-ref %)) events)
          (refuse :observation/unknown-event-ref
                  (str "observation: figure cites event " eref
                       " which is not in this observation")))))
    (let [flags (or missingness #{})]
      (when-not (and (set? flags) (every? #(contains? missingness-flags %) flags))
        (refuse :observation/unknown-missingness-flag
                "observation: missingness flags must come from the closed vocabulary"))
      (when (and (nil? (facts/spec-basis jurisdiction))
                 (not (contains? flags :jurisdiction-spec-basis-absent)))
        (refuse :observation/silence-claims-completeness
                (str "observation: jurisdiction " jurisdiction
                     " has no spec-basis in realty.facts — carry "
                     ":jurisdiction-spec-basis-absent, do not claim silently")))
      (doseq [e events]
        (when (and (= :title-transfer-recorded (:event/type e))
                   (not (some #(and (= :price-paid (:figure/kind %))
                                    (= (:event/registration-ref e)
                                       (:figure/event-ref %)))
                              figures))
                   (not (contains? flags :price-unavailable)))
          (refuse :observation/silence-claims-completeness
                  (str "observation: transfer " (:event/registration-ref e)
                       " has no price-paid figure and no :price-unavailable
                        flag — declare the gap or carry the figure")))))
    (frozen (assoc o
                   :obs/contract-version contract-version
                   :obs/refresh-of (or refresh-of nil)))))

(defn- same-subject? [a b]
  (= (:subject/id a) (:subject/id b)))

;; --- 8. history (pure, append-only in data) ---------------------------------

(defn observe
  "Append a validated observation to a history vector, returning a NEW
  vector (never mutate history in place). Refuses a duplicate observation
  id, a non-observation, and re-typing a subject id under a different
  entity type."
  [history o]
  (when-not (and (map? o) (= contract-version (:obs/contract-version o)))
    (refuse :history/not-an-observation
            "history: not an observation of this contract"))
  (when (some #(= (:obs/id o) (:obs/id %)) history)
    (refuse :history/duplicate-observation-id
            (str "history: observation id " (:obs/id o) " already recorded")))
  (when-let [prior (some #(when (= (:subject/id (:obs/subject o))
                                  (:subject/id (:obs/subject %)))
                            %)
                          history)]
    (when-not (= (:subject/entity-type (:obs/subject o))
                 (:subject/entity-type (:obs/subject prior)))
      (refuse :history/entity-type-conflict
              (str "history: subject " (:subject/id (:obs/subject o))
                   " was recorded as " (:subject/entity-type (:obs/subject prior))
                   ", now re-typed as " (:subject/entity-type (:obs/subject o))
                   " — a parcel is not a building"))))
  (conj (vec history) o))

(defn refresh
  "Record `o` as a re-observation of the observation named `prior-id`:
  links it via :obs/refresh-of and appends. Refuses an unknown prior, a
  self-link, and — at APPEND time, not only at readout — a link across
  subjects."
  [history prior-id o]
  (let [prior (some #(when (= prior-id (:obs/id %)) %) history)]
    (when-not prior
      (refuse :observation/refresh-of-unknown
              (str "refresh: prior observation " prior-id " not in history")))
    (when (= prior-id (:obs/id o))
      (refuse :observation/refresh-of-self
              "refresh: an observation cannot refresh itself"))
    (when-not (same-subject? (:obs/subject prior) (:obs/subject o))
      (refuse :observation/refresh-of-cross-subject
              (str "refresh: " (:obs/id o) " is about "
                   (:subject/id (:obs/subject o))
                   " but claims to refresh " prior-id " about "
                   (:subject/id (:obs/subject prior)))))
    (observe history (assoc o :obs/refresh-of prior-id))))

;; --- 8b. refresh delta (verbatim-level; no numeric difference anywhere) -----

(defn- keyed-events [obs]
  (into {} (map (fn [e] [(:event/registration-ref e) e]) (:obs/events obs))))

(defn- keyed-figures [obs]
  (into {}
        (map (fn [f] [[(:figure/kind f) (:figure/event-ref f) (:figure/raw f)] f])
             (:obs/figures obs))))

(defn refresh-delta
  "The verbatim-level audit of what moved between two frozen observations of
  the SAME subject: events added / removed / changed, figures added /
  removed / changed (both sides carried IN FULL), missingness flags added
  and removed, and the receipt ids of BOTH generations. Computes no numeric
  difference and normalizes nothing — amounts at different dates and areas
  under different units are not comparable, so they are only ever carried,
  side by side, never combined. `:delta/kind` is :unchanged when nothing
  moved."
  [prior next]
  (when-not (same-subject? (:obs/subject prior) (:obs/subject next))
    (refuse :delta/cross-subject
            "delta: the two observations are about different subjects"))
  (let [pe (keyed-events prior) ne (keyed-events next)
        pf (keyed-figures prior) nf (keyed-figures next)
        pm (:obs/missingness prior #{}) nm (:obs/missingness next #{})
        changed-events (vec (for [k (filter (fn [k] (and (contains? pe k) (contains? ne k)
                                                        (not= (get pe k) (get ne k))))
                                            (distinct (concat (keys pe) (keys ne))))]
                              {:delta/key k :delta/prior (get pe k) :delta/next (get ne k)}))
        changed-figures (vec (for [k (filter (fn [k] (and (contains? pf k) (contains? nf k)
                                                          (not= (get pf k) (get nf k))))
                                             (distinct (concat (keys pf) (keys nf))))]
                               {:delta/key k :delta/prior (get pf k) :delta/next (get nf k)}))
        added-events (vec (map #(get ne %) (filter #(contains? ne %) (remove #(contains? pe %) (keys ne)))))
        removed-events (vec (map #(get pe %) (filter #(contains? pe %) (remove #(contains? ne %) (keys pe)))))
        added-figures (vec (map #(get nf %) (filter #(contains? nf %) (remove #(contains? pf %) (keys nf)))))
        removed-figures (vec (map #(get pf %) (filter #(contains? pf %) (remove #(contains? nf %) (keys pf)))))
        gap-added (vec (sort (map name (set/difference nm pm))))
        gap-removed (vec (sort (map name (set/difference pm nm))))
        moved? (or (seq added-events) (seq removed-events) (seq changed-events)
                   (seq added-figures) (seq removed-figures) (seq changed-figures)
                   (seq gap-added) (seq gap-removed))]
    (frozen
     {:delta/kind (if moved? :changed :unchanged)
      :delta/subject-id (:subject/id (:obs/subject prior))
      :delta/prior-id (:obs/id prior) :delta/next-id (:obs/id next)
      :delta/prior-receipts (vec (sort (map :receipt/id (:obs/receipts prior))))
      :delta/next-receipts (vec (sort (map :receipt/id (:obs/receipts next))))
      :delta/added-events added-events :delta/removed-events removed-events
      :delta/changed-events changed-events
      :delta/added-figures added-figures :delta/removed-figures removed-figures
      :delta/changed-figures changed-figures
      :delta/gap-added gap-added :delta/gap-removed gap-removed
      :delta/comparability-note
      "verbatim-level only; no numeric difference is computed; amounts at
       different dates and areas under different units are carried side by
       side, never combined"
      :delta/contract-version contract-version})))

;; --- 7. derived observations (COUNTS, never prices or trends) ---------------

(defn window-observation
  "The derived, per-subject reading of ONE frozen observation: in-window
  event counts by type, the verbatim registration references, figure counts
  by kind, the missingness carried forward, the receipt ids. A count of what
  the receipts show — not a market measure, not a valuation, not a trend,
  and carrying no amounts at all."
  [obs]
  (frozen
   {:derived/contract-version contract-version
    :derived/subject-id (:subject/id (:obs/subject obs))
    :derived/window (:obs/window obs)
    :derived/event-counts (frequencies (map :event/type (:obs/events obs)))
    :derived/registration-refs (vec (sort (map :event/registration-ref
                                               (:obs/events obs))))
    :derived/figure-counts (frequencies (map :figure/kind (:obs/figures obs)))
    :derived/missingness (:obs/missingness obs #{})
    :derived/receipt-ids (vec (sort (map :receipt/id (:obs/receipts obs))))
    :derived/no-model true
    :derived/note
    "counts are coverage-limited observations of what the receipts show —
     not a market measure, not a valuation, not a trend"}))

(defn coverage-observation
  "The derived, catalog-level reading of `realty.facts`: how many
  jurisdictions carry a spec-basis, a provenance URL, official-source links,
  human gates — COUNTS over what the catalog itself publishes, plus the
  catalog's own honest note. Worldwide is a coverage goal, not a
  completeness claim, and the note says so."
  [recorded-at]
  (when-not (iso-locale-date? recorded-at)
    (refuse :coverage/bad-recorded-at
            "coverage: recorded-at must be YYYY-MM-DD"))
  (let [ks (keys facts/catalog)
        with-prov (filter #(nonempty-str? (:provenance (facts/spec-basis %))) ks)
        with-official (filter #(seq (:official-sources (facts/spec-basis %))) ks)
        with-gates (filter #(seq (:human-gates (facts/spec-basis %))) ks)]
    (frozen
     {:coverage/contract-version contract-version
      :coverage/recorded-at recorded-at
      :coverage/jurisdictions-with-spec-basis (count ks)
      :coverage/jurisdictions-with-provenance (count (distinct with-prov))
      :coverage/jurisdictions-with-official-sources-links (count with-official)
      :coverage/jurisdictions-with-human-gates (count with-gates)
      :coverage/note (:note (facts/coverage))
      :coverage/no-model true})))

;; --- 9. hyakka proposal (SHAPE — this contract transmits nothing) -----------

(defn hyakka-proposal
  "The exact claim shapes proposed to the `fudosan` corpus: one claim per
  verbatim figure plus one per observed subject, each carrying its receipt,
  value, basis, window and gaps; plus the scope's epistemic and privacy
  boundaries and `:no-model true`. Prop names are contract-local and NOT
  registered in the Hyakka ontology — the proposal says so
  (`:proposal/props-unregistered`). Receipt classes without a scope
  counterpart are flagged `:proposal/source-class-unmapped`. This returns
  DATA for the proposing run to carry; nothing is sent anywhere by this
  contract."
  [obs]
  (when-not (and (map? obs) (= contract-version (:obs/contract-version obs)))
    (refuse :proposal/not-an-observation
            "proposal: not an observation of this contract"))
  (let [subject (:obs/subject obs)
        window (:obs/window obs)
        gaps (vec (sort (map name (:obs/missingness obs #{}))))
        figure-claims
        (vec (for [f (:obs/figures obs)]
               (if (= :price-paid (:figure/kind f))
                 {:claim/prop "fudosan.prop/recorded-price-paid-observation"
                  :claim/subject-id (:subject/id subject)
                  :claim/entity-type (:subject/entity-type subject)
                  :claim/value-verbatim (:figure/raw f)
                  :claim/amount (:figure/amount f)
                  :claim/currency (:figure/currency f)
                  :claim/nominal-at (:figure/nominal-at f)
                  :claim/area-unit nil
                  :claim/receipt-id (:figure/source f)
                  :claim/event-ref (:figure/event-ref f)
                  :claim/window window
                  :claim/gaps gaps}
                 {:claim/prop "fudosan.prop/parcel-area-observation"
                  :claim/subject-id (:subject/id subject)
                  :claim/entity-type (:subject/entity-type subject)
                  :claim/value-verbatim (:figure/raw f)
                  :claim/amount nil
                  :claim/currency nil
                  :claim/nominal-at nil
                  :claim/area-unit (:figure/unit f)
                  :claim/receipt-id (:figure/source f)
                  :claim/event-ref (:figure/event-ref f)
                  :claim/window window
                  :claim/gaps gaps})))
        subject-claim
        {:claim/prop "fudosan.prop/recorded-events-in-window"
         :claim/subject-id (:subject/id subject)
         :claim/entity-type (:subject/entity-type subject)
         :claim/event-counts (frequencies (map :event/type (:obs/events obs)))
         :claim/window window
         :claim/receipt-ids (vec (sort (map :receipt/id (:obs/receipts obs))))
         :claim/gaps gaps}
        unmapped (vec (sort (map name (set/intersection
                                      (set (map :receipt/source-class
                                                (:obs/receipts obs)))
                                      unmapped-in-scope))))]
    (frozen
     {:proposal/corpus "fudosan"
      :proposal/target "network-awai/app-hyakka"
      :proposal/contract-version contract-version
      :proposal/subject-id (:subject/id subject)
      :proposal/jurisdiction (:obs/jurisdiction obs)
      :proposal/figure-claims figure-claims
      :proposal/subject-claims [subject-claim]
      :proposal/epistemic-boundaries (vec (sort (map name epistemic-boundaries)))
      :proposal/privacy-boundaries (vec (sort (map name privacy-boundaries)))
      :proposal/gaps gaps
      :proposal/no-model true
      :proposal/props-unregistered true
      :proposal/source-class-unmapped unmapped
      :proposal/note
      "SHAPE ONLY — this proposal is data for the proposing run to carry;
       this contract sends nothing anywhere"})))

;; --- 10. query / readback ---------------------------------------------------

(defn readback
  "The latest observation for `subject-id` whose :obs/recorded-at is at or
  before `as-of`, re-validating everything it returns (receipts by their
  derived id, figures, missingness, subject) and refusing tampered
  artifacts on the way out. A miss is a miss: nil, never a default."
  [history subject-id as-of]
  (when-not (iso-locale-date? as-of)
    (refuse :observation/bad-window "readback: as-of must be YYYY-MM-DD"))
  (let [candidates (filter #(and (= subject-id (:subject/id (:obs/subject %)))
                                 (not (pos? (compare (:obs/recorded-at %) as-of))))
                           history)]
    (when-let [obs (last (sort-by :obs/recorded-at candidates))]
      (doseq [r (:obs/receipts obs)]
        (revalidate-receipt r))
      (doseq [f (:obs/figures obs)] (figure f))
      (subject (:obs/subject obs))
      obs)))

(defn readback-chain
  "The full :obs/refresh-of lineage for a subject, OLDEST FIRST, every
  generation revalidated on the way out. Refuses a lineage whose link
  points at an unknown id (:readback/broken-lineage), a cyclic lineage
  (:readback/cyclic-lineage), and a chain element about another subject
  (:readback/chain-cross-subject). Returns the chain plus the pairwise
  deltas aligned to it (n-1 deltas for n generations)."
  [history subject-id]
  (let [by-id (into {} (map (fn [o] [(:obs/id o) o]) history))
        latest (last (filter #(= subject-id (:subject/id (:obs/subject %)))
                             (sort-by :obs/recorded-at history)))]
    (when latest
      (loop [cur latest, acc (), seen #{}]
        (when (contains? seen (:obs/id cur))
          (refuse :readback/cyclic-lineage
                  (str "readback: lineage cycles at " (:obs/id cur))))
        (let [acc (conj acc cur), seen (conj seen (:obs/id cur))]
          (doseq [r (:obs/receipts cur)] (revalidate-receipt r))
          (doseq [f (:obs/figures cur)] (figure f))
          (subject (:obs/subject cur))
          (when-not (= subject-id (:subject/id (:obs/subject cur)))
            (refuse :readback/chain-cross-subject
                    (str "readback: chain element " (:obs/id cur)
                         " is about " (:subject/id (:obs/subject cur)))))
          (if-let [pid (:obs/refresh-of cur)]
            (let [parent (get by-id pid)]
              (when-not parent
                (refuse :readback/broken-lineage
                        (str "readback: " (:obs/id cur) " refreshes " pid
                             ", which is not in history")))
              (recur parent acc seen))
            (let [chain (vec acc)
                  deltas (vec (map (fn [[a b]] (refresh-delta a b))
                                   (partition 2 1 chain)))]
              {:chain chain :deltas deltas})))))))
