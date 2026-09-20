# Design — Enamel

<!-- impeccable:design-doc · seed e7b54839 · world: Enamel (grounded #4) -->

The visual world is **vitreous-enamel signage** — the deep, glossy, keylined enamel
plaques of a Levantine banking hall and civic street. It replaced a dark-charcoal +
mint-neon emerald theme (which sat inside the "near-black + neon accent" AI cluster).
The world is fixed; there is no dark/light toggle. Name, logo (`iconbank`), and the
anti-AI flat principle from PRODUCT.md are preserved; the logo's maroon ring is now the
same family as the theme.

## Ground & color strategy

Committed strategy: a **porcelain** ground with **enamel colour fields owning whole
regions** (nameplate, primary actions), not accents scattered on neutral. Chosen from
the scene: a phone used in daylight, bilingual Arabic/English.

## Palette (named tokens in `values/colors.xml`)

| Token | Hex | Role |
|---|---|---|
| `enamel_oxblood` | `#7A1B2E` | Primary enamel field (nameplate, primary buttons). Echoes the logo ring. |
| `enamel_oxblood_light` | `#8F2436` | Oxblood tint / primary container |
| `enamel_petrol` | `#0E3B3A` | Secondary enamel field (tonal buttons, alternate plaques) |
| `enamel_brass` / `_light` / `_dark` | `#B07E33` / `#CFA255` / `#8A5F22` | Reserved accent: money legends, numerals, active state |
| `porcelain` / `_bright` / `_dim` | `#F1E7D2` / `#F8F1E1` / `#E5D8BC` | Ground, light type on enamel, inset fields |
| `enamel_ink` | `#2A1E16` | Primary text on porcelain |
| `enamel_keyline` | `#33251C` | The dark hairline around every enamel field — the material's signature |
| `signal_green` / `signal_orange` | `#1E6B41` / `#BE4A22` | Received / spent, used **only** for value state |

These map onto the Material 3 tokens (`md_theme_*`) so every screen repaints through the
theme. `colorPrimary` = oxblood, `colorSecondary` = petrol, `colorTertiary` = brass,
`colorOutline` = keyline, `colorSurface` = porcelain. Base parent is
`Theme.Material3.Light.NoActionBar`; status bar is light (dark icons).

## The keyline signature

Every enamel plaque is a saturated (or porcelain) field **bounded by a 1.5dp dark
keyline** (`colorOutline`) — the real tell of vitreous enamel signs. Applied to filled/
tonal/outlined buttons, `MaterialCardView` (`Widget.BankALDeir.Card`), `card_type_bg`
tiles, and the oxblood nameplate (`drawable/enamel_plaque_oxblood`). **Inputs are the one
exception** — `edt_emerald` and the filled `TextInputLayout` stay borderless
porcelain-inset (a standing user preference), so keylines read as "plaque," not "every box
has a border."

## Type

Signage voice: `sans-serif-condensed` (Roboto Condensed) themed app-wide — a civic/transit
signage face. Sentence-case copy (anti-AI principle), condensed and bold for headings.
Money legends are set as tracked brass small-caps (`letterSpacing` ~0.14); balances are
bold porcelain numerals on the oxblood nameplate. Arabic falls back to the system Naskh and
must stay legible — bilingual legends (e.g. `الرصيد · TOTAL BALANCE`) are first-class.
*Future:* self-host a real kufi Arabic plaque face (e.g. Reem Kufi) to fully honor "lean in."

## Shape

Controls & fields 14dp; plaques & cards 18dp; the nameplate 20dp. No elevation/shadow —
depth comes from the keyline and the flat colour fields, never drop shadows or glows.

## Motion & ripple

No ripple anywhere (`colorControlHighlight` transparent). Buttons keep the
`button_press` state-list animator. Staggered list entrances via the existing `anim/`
layout animations.

## Coverage status (this build)

- **System-level (all screens, via tokens):** porcelain ground, oxblood/petrol plaques,
  keylines, brass accents, condensed type, light status bar. Verified on onboarding, login,
  signup.
- **Hand-committed:** Home nameplate + promo carousel (`homeCards` DB node); Profile as a
  bank-membership card with embossed account number; Send money (oxblood balance plaque +
  amount hero); Receive (oxblood QR plaque with scannable white tile + brass copy pills);
  PIN page as enamel keypad discs; onboarding image keyline-framed. Transaction amounts use
  the enamel signals (green/orange).
- **Fatora flow:** enamel throughout — landing is an oxblood hero with a category strip;
  menus carry a brass `FATORA` legend; all 9 payment forms use brass signage field labels
  (`FatoraFieldLabel`). No off-palette drawables remained.
- **Propagated but not yet bespoke-composed:** cards, top-up, transactions history, receipt
  (`show-transaction` still uses hardcoded *emerald* drawables `back_plp` / `@color/check`).
  Next build.
- **Asset to replace:** onboarding photos (`onbording1–3.png`) are dark product mockups;
  redraw as flat enamel illustrations for full coherence.
