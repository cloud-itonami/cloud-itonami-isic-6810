# Business Model: Community Real-Estate Agency

## Classification
- Repository: `cloud-itonami-6810`
- ISIC Rev.5: `6810` — real-estate agency activities
- Social impact: housing access, tenancy protection, transparent fees

## Customer
- independent property agencies
- housing cooperatives and community land trusts
- student and affordable-housing operators
- agencies leaving closed CRM SaaS

## Offer
- parcel and listing management
- sale and rental listings
- lease scheduling with term-overlap detection
- tenancy and rent records
- maintenance and handover workflows
- role-based access and immutable audit ledger
- governed closing execution: listing intake, per-jurisdiction disclosure/
  title checklisting, buyer/seller/tenant KYC-sanctions screening, and a
  human-approved closing (title-transfer recording + escrow disbursement)
  handoff (the Realtor-LLM ⊣ RealtorGovernor actor -- see README)

## Revenue
- self-host setup fee
- managed hosting subscription per agency
- support retainer with SLA
- listing and settlement integration
- per-closing execution fee (intake through closing handoff)
- jurisdiction-pack licensing: a maintained, spec-cited disclosure/title
  requirement catalog for a specific country, kept current

| Package | Customer | Price shape |
|---|---|---|
| Managed Starter | 店舗1–3・管理戸数100–500戸・内勤3–10名の地域密着型仲介兼管理会社 | ¥30,000/月 flat |

**Market-anchored (2026-08-10)**: benchmarked against the real
不動産業務管理/賃貸管理 and real-estate transaction-management markets. Of the
7 products surveyed, **4 publish real numbers and 3 disclose nothing**:

- **いい生活 (ESいい物件One 賃貸管理)** — 専任管理タイプ 月額「20,000円〜/法人」,
  初期費用「300,000円」 (プラン500 = 500戸・区画); 家賃管理タイプ 月額
  「40,000円〜/法人」, 初期「500,000円」
  (<https://www.es-service.net/price/>). **¥20,000–40,000/月** at our size.
- **賃貸革命 (日本情報クリエイト)** — 「月々17,600円(税別)から ご利用いただけます」,
  varying by 契約台数, with plan bands from 100戸未満 to 2,000戸以上
  (<https://www.n-create.co.jp/pr/l/kakumei-chintai_price/>). **¥17,600〜/月**.
- **いえらぶ 賃貸管理開業パック** — 「初期費用：15万円」「月額：3万円（税別）」,
  bundling らくらく賃貸管理 + リーシングシステム + ホームページ
  (<https://www.ielove-group.jp/news/detail-304>). **¥30,000/月**.
- **dotloop** (US transaction management) — Premium 「$31.99」/month per agent;
  Teams and Business+ are 「Custom Quote Available」
  (<https://www.capterra.com/p/136372/dotloop/pricing/>). At 3–10 agents and
  ~¥150/$, **¥14,400–48,000/月**.

**Non-disclosing**: **いえらぶCLOUD** itself publishes no price — its pricing page
is a quote form (「ご回答内容を基に、初期費用・月額費用のお見積りをご提案」,
<https://ielove-cloud.jp/price/>). **SkySlope** (<https://skyslope.com/pricing/>)
and **Qualia** likewise route to sales; the $25–60/user figures circulating for
SkySlope are third-party estimates, not vendor-published, and are not used here.
The pattern is legible and worth recording: **the closer a product sits to
executing the transaction, the less likely it is to publish a price.**

**¥30,000/月 flat** sits mid-band in the measured ¥17,600–48,000 range. The
lower anchors (賃貸革命 ¥17,600, いい生活 専任管理 ¥20,000) are property/contract
ledgers. **The upper anchor — いい生活 家賃管理 ¥40,000 — justifies its premium
with rent collection and remittance, i.e. by moving money. This actor cannot
claim that premium: by construction it never records a title transfer and never
disburses escrow** (`realty.governor`'s `:actuation` gate and `realty.phase`,
which never puts `:closing/submit` in any phase's `:auto` set). What it adds
instead is absent from all four published comparators: per-jurisdiction
disclosure/title checklisting, buyer/seller/tenant KYC-sanctions screening, and
an un-overridable hold on a fabricated jurisdiction requirement, a sanctions/PEP
hit, or an incomplete document set. The trade being priced is therefore explicit
— **this tier does not move the money; it permanently gates the judgment
immediately before the money moves.** ¥30,000 also coincides with the price at
which いえらぶ actually sells a complete starter bundle, and with dotloop at a
mid-sized 6-agent office, so it is defensible from both the domestic and the
international comparison axis.

**Subscribe (2026-08-10)**: a live Stripe Payment Link for the Managed Starter
tier (¥30,000/月 flat) is available now — [**subscribe to Managed
Starter**](https://buy.stripe.com/aFadRa72vfVSe9OdiieEo06). This is a no-code
Stripe-hosted checkout; nothing in this repo's actor code changed. After
subscribing, contact gftdcojp to arrange managed-tenant setup (manual
fulfillment today, no automated onboarding yet). **No agency has claimed or
subscribed to this tier yet — this is a live, working checkout with zero paid
tenants, not a claim of existing revenue.**

## Trust Controls
- listings require an identified parcel
- overlapping lease terms can never be committed
- tenancy disclosures require governor approval
- every listing, lease and handover path is auditable
- tenant personal data stays outside Git
- no closing is recorded and no escrow fund is disbursed without human
  sign-off (the RealtorGovernor's actuation gate -- never bypassable)
- a fabricated jurisdiction disclosure requirement or a sanctions/PEP hit
  forces an un-overridable hold
