---
name: Clinical IT Asset Infrastructure
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#444650'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#757781'
  outline-variant: '#c5c6d1'
  surface-tint: '#455c99'
  primary: '#001c50'
  on-primary: '#ffffff'
  primary-container: '#17326c'
  on-primary-container: '#859cdd'
  inverse-primary: '#b2c5ff'
  secondary: '#00658d'
  on-secondary: '#ffffff'
  secondary-container: '#41befd'
  on-secondary-container: '#004b69'
  tertiary: '#1a0b66'
  on-tertiary: '#ffffff'
  tertiary-container: '#30277b'
  on-tertiary-container: '#9a93ec'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#b2c5ff'
  on-primary-fixed: '#001847'
  on-primary-fixed-variant: '#2c447f'
  secondary-fixed: '#c6e7ff'
  secondary-fixed-dim: '#81cfff'
  on-secondary-fixed: '#001e2d'
  on-secondary-fixed-variant: '#004c6b'
  tertiary-fixed: '#e4dfff'
  tertiary-fixed-dim: '#c5c0ff'
  on-tertiary-fixed: '#150362'
  on-tertiary-fixed-variant: '#423b8e'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 2rem
    fontWeight: '700'
    lineHeight: 2.5rem
    letterSpacing: -0.025em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 1.5rem
    fontWeight: '700'
    lineHeight: 2rem
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 1.5rem
    fontWeight: '600'
    lineHeight: 2rem
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 1.25rem
    fontWeight: '600'
    lineHeight: 1.75rem
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 1.25rem
    fontWeight: '600'
    lineHeight: 1.75rem
    letterSpacing: -0.015em
  title-sm:
    fontFamily: Inter
    fontSize: 1rem
    fontWeight: '600'
    lineHeight: 1.5rem
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 1rem
    fontWeight: '400'
    lineHeight: 1.5rem
  body-md:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: '400'
    lineHeight: 1.25rem
  body-sm:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: '400'
    lineHeight: 1rem
  label-md:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: '500'
    lineHeight: 1.25rem
  label-sm:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: '500'
    lineHeight: 1rem
    letterSpacing: 0.025em
  code-asset:
    fontFamily: Inter
    fontSize: 0.8125rem
    fontWeight: '600'
    lineHeight: 1rem
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 0.75rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system establishes a high-precision, clinical enterprise interface tailored for mission-critical IT asset management across medical centers, outpatient clinics, and hospital campuses. The design style combines **Corporate / Modern** rigor with **Tactile Precision**, reflecting the crisp architectural identity of clinical physical facilities: sterile bright surfaces, architectural structural blues, vibrant cyan accents, and clear status communication.

The interface serves IT directors, field support engineers, biomedical technicians, and audit teams who handle sensitive equipment, custody receipts, and strict hardware tracking. It prioritizes:
- **Clinical Serenity & Clarity:** Zero visual clutter, ultra-crisp tabular layouts, and clear typographic hierarchy.
- **Traceability & Confidence:** Visual weight given to hardware statuses, QR/barcode scans, asset custody trails, and operational readiness.
- **High-Density Utility:** Fluid handling of large data sets, dual asset tagging (`INV-000001` / `CPU-000001`), and immediate action triggers without sacrificing comfort or accessibility.

## Colors

The palette directly translates the physical architecture and identity of the medical centers into a high-contrast enterprise SaaS interface:

### Core Brand Tokens
- **Primary (`#17326C` / Hover `#1D3A7B`):** Deep institutional blue. Anchors master structural elements, persistent sidebars, primary action buttons, active navigation states, and high-emphasis data points.
- **Secondary / Accent (`#00A3E0` / Hover `#008BC7`):** Electric medical cyan. Drives interactive focal points, focus rings, progress indicators, quick filters, and active tab borders.
- **Tertiary (`#2E2579`):** Deep indigo-violet. Used for administrative badges, category tags, executive analytics highlights, and secondary metadata groupings.
- **Canvas & Surfaces:**
  - Base Background: `#F8FAFC` (Slate 50) — ultra-clean, clinical neutral background.
  - Surface Card / Table Body: `#FFFFFF` (Pure White).
  - Surface Subdued / Table Header: `#F1F5F9` (Slate 100).
  - Structural Borders: `#E2E8F0` (Slate 200) for grid dividers, cards, and modal dividers.

### Domain-Specific Semantic Statuses
- **Operativo / Confirmado:** Background `#ECFDF5`, Border `#A7F3D0`, Text `#047857` (Emerald `#10B981`).
- **En Traslado / Pendiente:** Background `#FFFBEB`, Border `#FDE68A`, Text `#B45309` (Amber `#F59E0B`).
- **Dañado / Mantenimiento / Incidencia:** Background `#FEF2F2`, Border `#FECACA`, Text `#B91C1C` (Coral Red `#EF4444`).
- **Auditoría / Información:** Background `#F0F9FF`, Border `#BAE6FD`, Text `#0369A1` (Sky `#0284C7`).

## Typography

The design system standardizes on **Inter** to ensure optimal legibility, tabular figure support, and clarity at compact scale. 

- **Tabular Figures (`tnum`):** Enforced across all table columns displaying serial numbers, IP addresses, dates, and quantitative inventory counters.
- **Asset Code Styling (`code-asset`):** Specific treatment for dual asset tags (`INV-001928`, `CPU-004312`). Displayed with medium weight, slight tracking (`0.05em`), and paired with slate backgrounds (`bg-slate-100 text-slate-800 border-slate-300`) to guarantee zero ambiguity between characters like `0` and `O` or `1` and `I`.
- **Hierarchical Balance:** Executive headers use tight letter spacing (`-0.02em`) to mirror solid signage and modern web application standards.

## Layout & Spacing

A responsive 12-column fluid grid system with fixed minimum gutters powers the workspace.

### Breakpoints & Layout Adaptations
- **Desktop (>= 1280px):** Permanent collapsible sidebar (expanded: `260px`, collapsed: `72px`). 12-column grid with `margin: 2rem` and `gutter: 1.5rem`. Multi-column asset property sheets and dense data tables with sticky action columns.
- **Tablet (768px - 1279px):** Sidebar collapses to an icon-only rail (`72px`) or overlay drawer. Responsive metric cards shift from 4 columns to 2x2. Data tables activate horizontal scroll with left-sticky primary asset identifiers.
- **Mobile (< 768px):** Sidebar shifts to a slide-over sheet triggered by a mobile header with hamburger navigation. Layout converts to a single column with `margin-mobile: 1rem`. Data tables offer card-view fallback or compact scroll rows with tap-to-expand details.

### Spacing Rhythm
- Compact UI components (table rows, badge clusters, input groups) maintain a tight 4px / 8px baseline (`space-xs` and `space-sm`).
- Structural boundaries (cards, filter toolbars, metric KPI tiles) use 16px to 24px internal padding (`space-md` to `space-lg`).

## Elevation & Depth

Visual hierarchy employs a **structured clinical layering** strategy: crisp borders paired with subtle, ambient, cool-tinted shadows (`rgba(23, 50, 108, 0.04)` to `rgba(23, 50, 108, 0.12)`).

1. **Level 0 (Flat Base):** Background canvas (`#F8FAFC`) with no shadow.
2. **Level 1 (Card & Table Surface):** Pure white `#FFFFFF` surface with a 1px border in `#E2E8F0` and `box-shadow: 0 1px 3px 0 rgba(23, 50, 108, 0.04), 0 1px 2px -1px rgba(23, 50, 108, 0.04)`.
3. **Level 2 (Dropdowns, Popovers, Filter Bars):** `#FFFFFF` surface with 1px border in `#CBD5E1` and `box-shadow: 0 4px 6px -1px rgba(23, 50, 108, 0.07), 0 2px 4px -2px rgba(23, 50, 108, 0.05)`.
4. **Level 3 (Modals & Delivery Act Sign-off Sheets):** Centered floating sheet with `box-shadow: 0 20px 25px -5px rgba(23, 50, 108, 0.12), 0 8px 10px -6px rgba(23, 50, 108, 0.08)` over a darkened backdrop (`bg-slate-900/50 backdrop-blur-sm`).

## Shapes

The geometric framework follows a balanced `rounded-md` / `rounded-lg` paradigm:

- **Buttons, Inputs & Dropdowns:** `0.5rem` (`rounded-md` / 8px) — provides ergonomic feel while maintaining corporate precision.
- **Metric Cards, Data Tables & Containers:** `0.75rem` (`rounded-xl` / 12px) with crisp outer border bounding.
- **Status Chips & Pills:** Full pill `9999px` (`rounded-full`) for high-contrast scanning of lifecycle statuses (e.g., "Operativo", "En Traslado").
- **Asset Code Badges & QR Blocks:** `0.375rem` (6px) with an inner mono border for mechanical clarity.

## Components

### Buttons
- **Primary:** Deep Blue `#17326C` background, white text, subtle hover lift to `#1D3A7B`, active state `#11244E`. Ring focus in Cyan `#00A3E0` with 2px offset.
- **Secondary / Action:** Pure white background, border `#CBD5E1`, text `#1E293B`. Hover `#F8FAFC` and text `#17326C`.
- **Cyan Accent (Quick Action / QR Scan):** Cyan `#00A3E0` background, white text, hover `#008BC7`.
- **Destructive:** Background `#EF4444`, hover `#DC2626`, white text.

### Asset Status Badges & Chips
- Semantically tinted pill badges (`px-2.5 py-0.5 text-xs font-semibold rounded-full border`):
  - *Operativo:* `bg-emerald-50 text-emerald-700 border-emerald-200` with leading 6px pulsing dot.
  - *En Traslado:* `bg-amber-50 text-amber-700 border-amber-200`.
  - *Mantenimiento / Baja:* `bg-rose-50 text-rose-700 border-rose-200`.
  - *Auditoría Pendiente:* `bg-sky-50 text-sky-700 border-sky-200`.

### Data Tables (Hospital Asset Catalog)
- **Header:** Background `#F8FAFC`, uppercase `text-[11px] font-semibold text-slate-500 tracking-wider border-b border-slate-200`.
- **Rows:** Zebra-less white cards separated by `border-b border-slate-100`, hover row tint `bg-slate-50/70`.
- **Sticky Actions:** End column with asset actions (Ver Acta, Reasignar, Historial, Imprimir QR) with ellipsis menu and direct icon buttons.

### Metric KPI Cards
- White surface, `rounded-xl`, border `#E2E8F0`, internal padding `1.25rem`.
- Header row with icon container in light cyan (`bg-sky-50 text-[#00A3E0] rounded-lg p-2`), micro-label in uppercase `#64748B`, bold stat counter (`text-2xl font-bold text-[#17326C]`), and weekly variation pill.

### Form Inputs & Barcode/QR Lookup
- Inputs feature `h-10 px-3.5 text-sm bg-white border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#00A3E0] focus:border-[#00A3E0]`.
- Specialized QR scanning input addon featuring instant camera scanner trigger embedded on the trailing edge.

### Actas de Entrega / Recepción (Custody Modals)
- High-level review modal displaying dual-party verification (Técnico TI vs. Custodio Clínico), hardware specifications breakdown, and canvas signature capture box bounded by `#CBD5E1` border.