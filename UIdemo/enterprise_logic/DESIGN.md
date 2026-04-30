---
name: Enterprise Logic
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#434655'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#747686'
  outline-variant: '#c4c5d7'
  surface-tint: '#2151da'
  primary: '#0037b0'
  on-primary: '#ffffff'
  primary-container: '#1d4ed8'
  on-primary-container: '#cad3ff'
  inverse-primary: '#b7c4ff'
  secondary: '#0051d5'
  on-secondary: '#ffffff'
  secondary-container: '#316bf3'
  on-secondary-container: '#fefcff'
  tertiary: '#2b4663'
  on-tertiary: '#ffffff'
  tertiary-container: '#435e7c'
  on-tertiary-container: '#bbd7fa'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dce1ff'
  primary-fixed-dim: '#b7c4ff'
  on-primary-fixed: '#001551'
  on-primary-fixed-variant: '#0039b5'
  secondary-fixed: '#dbe1ff'
  secondary-fixed-dim: '#b4c5ff'
  on-secondary-fixed: '#00174b'
  on-secondary-fixed-variant: '#003ea8'
  tertiary-fixed: '#d0e4ff'
  tertiary-fixed-dim: '#adc9eb'
  on-tertiary-fixed: '#001d35'
  on-tertiary-fixed-variant: '#2d4965'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-dashboard:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-section:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-large:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-base:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  caption:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-padding: 24px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style

This design system is built upon a **Corporate Modern** aesthetic, specifically tailored for high-stakes enterprise data environments. The visual narrative centers on "Precision and Trust," utilizing a structured hierarchy that balances high-tech sophistication with utilitarian efficiency. 

The atmosphere is professional and grounded, leaning into a monochromatic foundation with deep, authoritative gradients in primary brand moments. To evoke a sense of security, the system utilizes clean lines, generous whitespace, and a disciplined application of depth, ensuring that the act of managing sensitive files feels controlled and effortless.

## Colors

The color strategy uses a **Deep Blue to Indigo** gradient for high-impact brand areas like login screens and sidebar headers to establish an immediate sense of enterprise-grade authority. 

For the functional workspace:
- **Primary Actions:** Blue-600 is the workhorse for interactive elements, ensuring high visibility against white and gray backgrounds.
- **Accents:** Blue-200 is used sparingly for focus states, progress bar backgrounds, and subtle highlights.
- **Surface Tones:** A mix of Gray-50 and Gray-100 creates distinct logical zones without relying on heavy borders.
- **Semantic Feedback:** Standardized status colors ensure that file upload states (Success, Error, Warning, Info) are instantly recognizable.

## Typography

The typography system relies exclusively on **Inter**, a typeface designed for screen readability and systematic UI. The hierarchy is characterized by significant contrast between "Display" styles used for authentication and "Functional" styles used for the dashboard.

- **Headlines:** Use Bold (700) or SemiBold (600) weights to anchor pages. 
- **Data Density:** Body text is primarily 14px to allow for high information density in file tables and property panels.
- **Labels:** Small caps or bold 12px labels are used for metadata headers and table column titles to maintain a professional, organized structure.

## Layout & Spacing

This design system utilizes a **12-column fluid grid** for the main dashboard content, allowing the file management interface to scale from laptop screens to ultra-wide monitors. 

- **Sidebar:** Fixed at 260px for navigation and system-wide filters.
- **Content Area:** Uses a 24px outer margin with 16px gutters between cards.
- **Rhythm:** An 8px base grid drives all component spacing. Vertical stacks within cards should follow a consistent 16px or 24px pattern to maintain a sense of order.
- **Table Density:** Row heights are optimized at 48px for standard views and 40px for "compact" views.

## Elevation & Depth

The design system employs **Tonal Layering** supplemented by subtle ambient shadows to define the Z-axis.

1.  **Canvas (Level 0):** Gray-50 background.
2.  **Cards & Containers (Level 1):** White background with a 1px border in Gray-100. Shadows are soft and diffused: `0 4px 6px -1px rgb(0 0 0 / 0.05)`.
3.  **Dropdowns & Modals (Level 2):** White background with a more pronounced shadow to indicate focus and physical separation: `0 10px 15px -3px rgb(0 0 0 / 0.1)`.

Gradients are reserved for high-level primary actions and brand backgrounds, never for individual component depth.

## Shapes

The shape language is defined by **Soft Geometricism**. 

- **Primary Containers:** All cards and large containers use `rounded-xl` (12px) to soften the enterprise feel and make the interface feel more modern.
- **Interactive Elements:** Buttons and Input fields use a standard `rounded-lg` (8px) for a crisp, functional appearance.
- **Status Indicators:** Pills and tags use a fully rounded (pill-shaped) radius to differentiate them from actionable buttons.

## Components

### Buttons
- **Primary:** Blue-600 background, white text. Dashboard height: 40px (h-10); Auth height: 48px (h-12).
- **Secondary:** White background, Gray-100 border, Blue-600 text.
- **Ghost:** No background or border, Blue-600 or Gray-500 text. Used for utility actions.

### File Upload Dropzone
- Large container with a dashed border (Blue-200), Blue-50 background, and `rounded-xl` corners. High-contrast icon in the center to signify drag-and-drop capability.

### Tables
- Header row with Gray-50 background and SemiBold 12px text.
- Row styling: Alternating striped rows (White / Gray-25) to improve horizontal scanning of long file lists.
- Hover state: Rows highlight in Blue-50 to indicate interactivity.

### Progress Bars
- 8px height with a Blue-200 background track and a Blue-600 primary fill. Success states transition the fill to Green-600.

### Input Fields
- White background, Gray-200 border, 8px corner radius. Focus state uses a 2px Blue-200 ring and Blue-600 border.