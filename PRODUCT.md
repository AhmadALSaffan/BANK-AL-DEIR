# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Two audiences, both first-class:

- **Reviewers / recruiters** evaluating the developer's Android skill — from the GitHub repo, screenshots, or a demo walkthrough. This is the primary success audience.
- **Simulated end-users** inside the app: retail banking customers who check balances, hold multiple cards, move money, scan QR codes to pay, top up, and pay bills. The screens are designed for this person even though the account data is a sandbox.

The end-user audience is **bilingual**: English and Arabic must both be first-class. Layouts and copy must hold up in both LTR and RTL. The names "Bank Al-Deir" and "Fatora" (فاتورة, "invoice/bill") reflect an Arabic-speaking user base.

## Product Purpose

Bank Al-Deir is a native Android banking application built to **look and feel like a real, shippable retail bank app** — as a portfolio showcase of Android + Firebase engineering. It is explicitly **not** a real bank and moves no real money (see README disclaimer). Success is a reviewer coming away convinced the app is production-grade in polish, flow, and craft.

## Positioning

The differentiator is **craft over scope**: this is a demo that refuses to look like a demo. Where portfolio banking apps typically ship generic Material templates and obvious placeholder data, Bank Al-Deir commits to a deliberate, hand-finished "anti-AI" visual identity and real end-to-end flows (auth → cards → transfers → bill pay → receipts) so nothing reads as scaffolding.

## Operating Context

- Distributed as an installable APK and as a GitHub repo; also evaluated through static screenshots/mockups in the README.
- Runs on Android phones, API 21+. Verified on emulator during development.
- Backed by Firebase Auth (email/password, Google sign-in) and Firebase Realtime Database. No real payment rails; Google Pay is wired via play-services-wallet as a demo top-up path.
- Screens frequently sit behind a PIN gate, which affects how flows are demoed and screenshotted.

## Capabilities and Constraints

Confirmed functional surface:

- **Auth**: registration, email/password login, Google sign-in.
- **Accounts & cards**: single wallet balance pooled with a designated main card; multiple cards; create new card; card options (transfer to/from a card, set main card — limited to 3 changes).
- **Money movement**: send money, receive, card-to-card transfers, QR-code scan-to-pay.
- **Top-up**: add funds via Google Pay (demo).
- **Fatora (bill pay)**: government services, university fees, mobile top-ups, electricity, and related bills, each as its own flow, all paid from the wallet balance.
- **Transactions**: full history, per-transaction receipt/detail screen (with success animation).
- **Confirmation**: 4-digit OTP delivered as a local notification (`OtpGate`) gates money-moving actions (payments and transfers); falls back to a toast if notifications are denied.
- **Profile**: view/update profile.

Data model terminology (Firebase Realtime DB): `wallets/{walletId}/Balance`, `wallets/{walletId}/cards/{cardKey}/Balnce` (note the existing misspelled key), `wallets/{walletId}/mainCardKey`, `wallets/{walletId}/mainCardChanges`, `history/{transactionNumber}`. The main card mirrors the wallet Balance as one pool — top-ups always credit Balance.

Constraints:

- Native Android (Kotlin, XML layouts, ViewBinding, Navigation component). Not web; a native design language is expected, not a web look.
- Must remain buildable/installable via `./gradlew assembleDebug`; emulator storage has been a recurring install constraint during development.
- No real banking services or real money movement may be implied as functional.

## Brand Commitments

- **Name**: "Bank Al-Deir" — locked.
- **Logo**: the original `iconbank` asset — locked; use it centered/well-placed rather than replacing it with a generated mark.
- **Visual direction**: "anti-AI" flat craft is a **locked design principle** — flat surfaces, no radial glows, no gradients, no colored icon-badge frames, sentence-case copy, no fabricated/placeholder data (fake card numbers, marketing filler, dead buttons). This is a durable constraint, not a passing style.
- **Palette is NOT locked**: the current dark-charcoal + mint `#4EDEA3` emerald theme is the incumbent implementation, but the user did not commit to it as permanent. Future work may evolve the palette; it must not be treated as an untouchable brand asset.

## Evidence on Hand

- README with disclaimer, feature list, tech stack, and hosted mockups/screenshots (`mockup1..5.png`).
- Onboarding banner images supplied by the user (`onbording1/2/3.png`).
- Real, working flows in-code (auth, cards, transfers, Fatora, receipts, OTP) — not mocked stubs.
- No real customers, testimonials, transaction volumes, pricing, or partnerships exist. Future work must not fabricate any; account data is sandbox/demo only.

## Product Principles

1. **Refuse the demo look.** Every screen should read as a shipped bank, not a student project — the craft is the portfolio.
2. **No fabricated substance.** No fake card numbers, filler copy, dead buttons, or invented data; if it appears, it should work.
3. **Flat and honest over decorated.** Anti-AI flat surfaces and plain tinted icons; hierarchy comes from type, spacing, and restraint — not glows or gradients.
4. **Both directions, both languages.** English and Arabic / LTR and RTL are first-class; nothing may break when mirrored.
5. **Confirm before money moves.** Money-moving actions pass through explicit confirmation (PIN / 4-digit OTP); trust is part of the product's believability.

## Accessibility & Inclusion

- **RTL / bilingual** is a product requirement: layouts must mirror correctly and copy must work in Arabic and English (`android:supportsRtl="true"` is set).
- Standard Android touch-target and contrast expectations apply; the dark theme must keep text legible against lifted surfaces.
