---
name: Academic Modernism
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45464f'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#767680'
  outline-variant: '#c6c5d0'
  surface-tint: '#4f5c8f'
  primary: '#000420'
  on-primary: '#ffffff'
  primary-container: '#0c1b4b'
  on-primary-container: '#7784ba'
  inverse-primary: '#b7c4fe'
  secondary: '#0062a0'
  on-secondary: '#ffffff'
  secondary-container: '#77bbff'
  on-secondary-container: '#004a7a'
  tertiary: '#646100'
  on-tertiary: '#ffffff'
  tertiary-container: '#b6af00'
  on-tertiary-container: '#444100'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dce1ff'
  primary-fixed-dim: '#b7c4fe'
  on-primary-fixed: '#071747'
  on-primary-fixed-variant: '#374475'
  secondary-fixed: '#d0e4ff'
  secondary-fixed-dim: '#9bcbff'
  on-secondary-fixed: '#001d34'
  on-secondary-fixed-variant: '#004a7a'
  tertiary-fixed: '#f0e81b'
  tertiary-fixed-dim: '#d3cb00'
  on-tertiary-fixed: '#1e1c00'
  on-tertiary-fixed-variant: '#4c4900'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.025em
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.005em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-sm: 1rem
  gutter-lg: 2rem
  margin: 1.5rem
  margin-sm: 1rem
  margin-lg: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

## Brand & Style

This design system defines a digital learning ecosystem balancing institutional authority with energetic curiosity. It is tailored for teachers, students, school administrators, and parents who require frictionless, daily task completion alongside intelligent pedagogical assistance.

The aesthetic philosophy centers on **Corporate / Modern** augmented with luminous digital accents. High-contrast typography and clear layout geometry project structure, discipline, and institutional trust, while crisp glacier tones and radiant lemon sparks denote artificial intelligence, progress, and scholastic achievement. The interface eliminates academic clutter in favor of crisp whitespace, deliberate visual hierarchy, and tactile feedback.

## Colors

The palette establishes strict utility:

- **Primary Navy (`#0C1B4B`):** Anchors the interface. Used for deep structure, headers, critical calls to action, dense data tables, and high-emphasis textual content.
- **Glacier Blue (`#62A7EA`):** Communicates active navigation states, interactive links, selected filters, and calm secondary actions. Tints (e.g., `#EBF3FC`) serve as soft backgrounds for selected rows and informational badges.
- **Lemon Accent (`#F2EA1E`):** Represents intelligent features, AI assistant highlights, badges, and scholastic recognition. It must strictly pair with deep Navy text (`#0C1B4B`) for AA/AAA contrast compliance; it is never paired with white text.
- **Surface & Canvas:** Base background operates on `#F8FAFC` and `#F1F5F9` to reduce eye fatigue across multi-hour school sessions. Individual workspace cards sit on pure white (`#FFFFFF`).
- **Dividers & Strokes:** Boundary lines leverage `#E2E8F0` to maintain clean segmentations without aggressive structural borders.

### Status Indicators
- **Asistió / Entregado (Success):** Emerald tint (`#ECFDF5`) with deep emerald text (`#047857`).
- **Tardanza / Pendiente (Warning):** Amber tint (`#FFFBEB`) with warm bronze text (`#B45309`).
- **Falta / No Entregado (Danger):** Rose tint (`#FFF1F2`) with crimson text (`#BE123C`).
- **AI Spark / Destacado:** Lemon yellow (`#F2EA1E`) paired with Navy (`#0C1B4B`).

## Typography

Typography relies on **Inter** to maintain legibility across dense educational rosters, academic performance dashboards, and administrative data sets.

- **Headlines:** Display and headline levels utilize tight negative tracking (`-0.025em` to `-0.01em`) and bold weights (`700`, `800`) in Navy (`#0C1B4B`) to evoke confident editorial leadership.
- **Body & Metrics:** Standard reading text employs neutral regular weights (`400`) in neutral slate (`#334155` for high priority, `#64748B` for secondary annotations).
- **Labels & Tags:** Attendance badges and categorical flags employ uppercase or capitalized short strings set in medium to bold weights (`600`, `700`) with positive tracking (`0.02em` to `0.04em`) to ensure legibility at micro scales.

## Layout & Spacing

The layout is built upon an 8pt dynamic scale within a responsive 12-column fluid grid.

- **Desktop (1200px+):** 12 columns with 24px (`1.5rem`) gutters and 48px (`3rem`) outer margins. Content maximum width is capped at 1440px to ensure data views remain readable without panoramic eye strain.
- **Tablet (768px – 1199px):** 8 columns with 16px (`1rem`) gutters and 24px (`1.5rem`) side padding. Left-hand navigation drawers collapse into icon-only rails.
- **Mobile (320px – 767px):** 4 columns with 16px gutters and 16px canvas margins. Primary navigation transitions to a persistent sticky bottom tab bar or top drawer.
- **Internal Density:** Operational cards, gradebooks, and attendance tables use compact vertical spacing (`space-sm` and `space-md`), while marketing and dashboard landing sections expand to `space-xl` and `space-2xl`.

## Elevation & Depth

Visual depth combines crisp surface tints with ambient, diffused shadows tinted by deep navy.

- **Level 0 (Flat Surface):** Base canvas `#F8FAFC`. Flat panels demarcated solely by a 1px border (`#E2E8F0`).
- **Level 1 (Cards & Data Containers):** Pure white `#FFFFFF` surface accompanied by a subtle tinted shadow:
  `box-shadow: 0 1px 3px 0 rgba(12, 27, 75, 0.04), 0 1px 2px -1px rgba(12, 27, 75, 0.04);` bordered by `#E2E8F0`.
- **Level 2 (Interactive Floating & Hover):** Lifted cards, filter flyouts, and active attendance tiles:
  `box-shadow: 0 4px 6px -1px rgba(12, 27, 75, 0.06), 0 2px 4px -2px rgba(12, 27, 75, 0.04);`
- **Level 3 (Modals, Slide-overs, AI Assistant Dialogs):** High floating overlays:
  `box-shadow: 0 20px 25px -5px rgba(12, 27, 75, 0.08), 0 8px 10px -6px rgba(12, 27, 75, 0.04);`
- **AI Spark Glow:** Intelligent query widgets and assistant cards feature an accent halo using Lemon and Glacier Blue:
  `box-shadow: 0 0 0 1px #62A7EA, 0 4px 20px -2px rgba(242, 234, 30, 0.35);`

## Shapes

The design system adopts a refined rounded aesthetic (`roundedness: 2`) that balances academic decorum with soft, friendly modernism:

- **Standard UI Elements (Buttons, Inputs, Table Cells):** `0.5rem` (8px) radius.
- **Standard Cards, Panels, Attendance Grids:** `1rem` (16px) radius (`rounded-lg` / `rounded-xl`).
- **Modals, Floating Panels, Feature Callouts:** `1.5rem` (24px) radius (`rounded-2xl`).
- **Badges, Tags, Status Indicators, and AI Pills:** Fully rounded pill shapes (`9999px`) to distinguish categorical metadata from structural containers.

## Components

### Brand Identity & Logotype
- The primary brand mark is integrated as the header emblem:
  `<img src="{{DATA:IMAGE:IMAGE_2}}" alt="Colegia" class="h-9 w-auto object-contain" />`
- The logo must be placed exclusively over light surfaces (`#FFFFFF` or `#F8FAFC`) to preserve legibility and high contrast.

### Buttons
- **Primary:** Background Navy `#0C1B4B`, text `#FFFFFF`, radius `0.5rem`, font-weight `600`. Hover state transitions smoothly to `#1A2D6D`. Focus rings display a 2px offset ring in Glacier Blue `#62A7EA`.
- **Secondary / Ghost:** Transparent background with 1px border in `#CBD5E1`, text `#0C1B4B`. Hover state applies `#F1F5F9`.
- **AI Assistant Trigger:** Lemon `#F2EA1E` background, Navy `#0C1B4B` text and icons, subtle outer glow. Hover darkens to `#E3DC16`.

### Attendance & Status Chips
Pill-shaped badges (`rounded-full`, padding `0.25rem 0.75rem`, font size `11px`, bold):
- **Asistió / Entregado:** Background `#ECFDF5`, text `#047857`, 1px border `#A7F3D0`.
- **Tardanza:** Background `#FFFBEB`, text `#B45309`, 1px border `#FDE68A`.
- **Falta:** Background `#FFF1F2`, text `#BE123C`, 1px border `#FECDD3`.
- **Pendiente:** Background `#F8FAFC`, text `#475569`, 1px border `#CBD5E1`.
- **AI Highlight:** Background `#FEF08A`, text `#0C1B4B`, 1px border `#F2EA1E`.

### Cards & Data Containers
- White `#FFFFFF` surface with `1rem` (16px) border-radius, bounded by a 1px border in `#E2E8F0` and Level 1 elevation.
- Internal headers include clear title hierarchy, subtle badge counters, and action dropdowns.

### Form Inputs & Checkboxes
- **Input Fields:** 42px height, border 1px `#CBD5E1`, border-radius `0.5rem`, padding `0 0.75rem`, placeholder text `#94A3B8`. Active focus applies a 2px outline in `#62A7EA` with zero border red-shift.
- **Checkboxes & Radios:** Navy `#0C1B4B` fill when selected, featuring a crisp white tick mark and `4px` corner curve.

### Academic Roster & Grade Lists
- Zebra alternate striping using transparent and `#F8FAFC`.
- Hover states transition rows to Glacier Blue tint (`#F0F7FD`) with smooth 150ms ease.