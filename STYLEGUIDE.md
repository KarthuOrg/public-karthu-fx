# Nebula Dark and Nebula Light — JavaFX Desktop Style Guide

A compact, IDE-grade theme (Notepad++ / Eclipse density) in two colour schemes: **Nebula Dark**,
with purple/blue-tinted dark surfaces — deliberately NOT flat black/gray — and **Nebula Light**,
with a cool grey chrome, white fields and dark ink. Both share every shape, size and spacing;
only the colours differ. Fully token-driven: the colours live in `dark.css` and `light.css`
(`.root` variables, the same names in both); structure/density live in `base.css`. To restyle,
change tokens — do not hardcode colors in component rules.

---

## 1. Design principles
- **Two schemes, one set of names.** Every colour token means the same in both schemes, so a rule
  written with tokens works in both. An application chooses Light, Dark or System; without a
  choice the scheme is Dark (§5).
- **Tinted, never flat.** In Nebula Dark every neutral carries a slight blue/indigo hue
  (hue ≈ 260–265); Nebula Light's chrome is a cool grey and its editable surfaces white.
  Never use pure `#000`/`#808080` grays, and no field in pure black; pure white is Nebula Light's
  field colour alone.
- **Ultra-compact.** Table/list/tree rows ~20px, controls ~28px, radius 3px, 1px borders.
- **One accent** (indigo `#7b8cff` in Dark, blue `#2F6FD6` in Light) for focus, primary actions
  and checked controls; selections in a quieter blue.
- **Semantic color only for status** (state column, badges, icons).
- **Type:** "Segoe UI Variable", "Segoe UI", system-ui. Base 13px; tables 11.5px.
  LCD (subpixel) smoothing on every text node.
  Monospace (JetBrains Mono / Cascadia Code) optional for data columns.

---

## 2. Color tokens

`dark.css` (Nebula Dark) and `light.css` (Nebula Light) define the same tokens, in the same order
and grouping, and `ThemeManager` loads the stylesheet of the scheme in effect after `base.css`:
one replaces the other, never both. The tables below hold every token: what it colours, its value
in each scheme, the token of the colour design it follows (the design's `-app-*` names; *The
design's tokens* at the end of this section maps every one of them) and the contrast noted for
Nebula Light.

- **Dark** is Nebula Dark's value; the design tokens' dark values are not used.
- **Light** is the light value of the design token in the *Design* column, with one exception:
  the field family, white where the design's `-app-bg-field` is an ivory (`#F3F1E8`). *Proposed*
  marks a value derived by the library rather than taken from the design tokens, following their
  logic (white fields, the grey chrome, deep hues that read on light, darker under the pointer),
  and a value the design tokens give only as a proposal.
- **A value that names a token** refers to it, and JavaFX resolves the reference at the coloured
  node. Where Nebula Light tells apart places that share one colour in Nebula Dark, they take a
  token of their own: in `dark.css` it refers to the colour the places had, or holds its
  computation (a `derive`), so Nebula Dark keeps its colours. A light value that is a `derive`
  follows the token it names; the colour it resolves to stands beside it.
- **Contrast** in Nebula Light, after WCAG, against the background the colour sits on: text
  4.5:1, borders and focus 3:1, disabled text exempt.

### Backgrounds
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-background-color` | the window, the root of every themed scene | `#1b1d30` | `#E4E5E8` | `-app-bg-window` | background |
| `-fx-background-color-selected` | no place in the theme, for an application's own use: a veil in the window colour | `rgba(18, 19, 32, 0.78)` | `rgba(228, 229, 232, 0.78)` *proposed* | — | a veil, the window colour at 78 percent as in dark; no text of its own |
| `-fx-canvas-color` | the area behind cards (so surface cards stand out), the group rows of the shortcuts editor, the settings dialog, the image window's image area | `#1b1d30` | `#DCDDE1` *proposed* | `-app-bg-canvas` | background, the design's canvas and raised grey, darker than the window as dark's canvas is darker than its surface; text on it 11.6:1, `-fx-text-secondary-color` 4.4:1, below 4.5:1 |
| `-fx-surface-color` | panels, tool and menu bars, the menus of the menu bar and context menus, tab headers, dialogs, the status bar, cards | `#24273c` | `#E4E5E8` | `-app-bg-surface` | background; in light the same grey as the window, so the chrome reads as one surface, an open menu with the bar it hangs from |
| `-fx-readonly-bg` | read-only text fields, text areas and form fields | `#1e202f` | `#DCDDE1` *proposed* | — | background, grey as the design's field that cannot be edited (`-app-bg-raised`); text on it 11.6:1, the warning outline 4.4:1, the error outline 3.8:1, the focus outline 3.5:1 |
| `-fx-field-color` | every editable surface: text fields, password fields, combo and choice boxes, date pickers, spinners, text areas, the form fields, the zoom group, dropdown lists (a combo box's and a choice box's), the date picker's and the suggestion popup, list, tree and table bodies and the rows of tables | `-fx-surface-color` | `#FFFFFF` | `-app-bg-field` | background, white (the design's ivory replaced); text on it 15.8:1, `-fx-control-border` against it 1.7:1, below 3:1 |
| `-fx-raised-color` | a raised surface: the Markdown view's content area | `-fx-surface-color` | `#EEEFF2` (lighter than the design's `#DCDDE1`) | `-app-bg-raised` | background; text on it 13.7:1, `-fx-text-secondary-color` 5.1:1, inline code 5.4:1, links 6.0:1 |

### Text
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-text-color` | primary text: labels, menu items, the text of inputs, icons on neutral buttons | `#d6d9e6` | `#1F2230` | `-app-text` | 12.5:1 on window and surface, 10.5:1 on hover |
| `-fx-text-secondary-color` | secondary and muted text: form field labels, captions, helper text, counters, idle icons inside fields | `#8a90a8` | `#5E6475` | `-app-text-muted` | 4.7:1 on window and surface; 3.9:1 on hover, below 4.5:1 |
| `-fx-text-placeholder-color` | prompt texts, *Not assigned* in the shortcuts editor | `-fx-text-secondary-color` | `#5A5F6E` | `-app-text-placeholder` | 6.4:1 on the field surface |
| `-fx-text-disabled-color` | a disabled menu item's text, a disabled link's | `-fx-text-secondary-color` | `#7C8090` | `-app-text-disabled` | 3.1:1 on window and surface, where a menu lies, and 3.9:1 on the field surface, before the dimming of a disabled control (disabled text exempt) |

### Accent
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-accent-color` | focus borders, primary buttons, selected toggle buttons, checked boxes and radios, the slider's fill, the selected tab's underline, the quote bar, the shortcuts editor's modified dot | `#7b8cff` | `#2F6FD6` | `-app-focus`; `-app-status-modified` *proposed* | 3.8:1 against window and surface; white text on it 4.8:1 |
| `-fx-accent-hover-color` | the accent fill under the pointer (primary button, selected toggle button, checked box and radio, the slider's thumb), a hyperlink and the shortcuts editor's fix link under the pointer | `#8f9dff` | `derive(-fx-accent-color, -12%)` (`#2962BC`) *proposed* | — | darker under the pointer, as the primary green; white text on it 5.9:1; as text 4.7:1 on window and surface |
| `-fx-accent-pressed-color` | the accent fill pressed (primary button, the slider's thumb) | `#6675ff` | `derive(-fx-accent-color, -23%)` (`#2455A5`) *proposed* | — | white text on it 7.2:1 |
| `-fx-accent-text-color` | text on the accent fill (primary button, selected toggle button, the date picker's selected day) | `#ffffff` | `#FFFFFF` *proposed* | — | white, as on the primary fill; 4.8:1 on `-fx-accent-color` |
| `-fx-accent-tint-color` | an accent tint behind accent text: the form fields' binding chip and bound `{}` icon | `rgba(123, 140, 255, 0.16)` | `#D6E2F8` | `-app-accent-bg` | background |
| `-fx-accent-tint-text-color` | the text and icons on the accent tint: the binding chip's name and x, the bound `{}` icon | `-fx-accent-color` | `#1B4C9E` | `-app-accent-text` | 6.3:1 on the accent tint |
| `-fx-link-color` | the Markdown view's links and images in table cells, hyperlinks, the shortcuts editor's fix | `-fx-accent-color` | `#1F56B3` *proposed* | `-app-link` | 5.5:1 on window and surface, 5.1:1 on the Markdown view's raised content, 6.9:1 on the field surface; a hyperlink under the pointer takes `-fx-accent-hover-color`, 4.7:1 on the window |

### Borders
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-control-border` | every border and divider: fields, buttons, popups, separators, the lines between bars | `#42486e` | `#C2C4CC` | `-app-border`, `-app-border-subtle` | 1.4:1 against window and surface, below 3:1 |
| `-fx-row-divider-color` | the lines between table rows and between a table's columns | `-fx-control-border` | `#DADCE1` | `-app-divider-row` | 1.4:1 against the field surface, 1.2:1 against the odd rows, below 3:1 (a hairline, no boundary) |

### Buttons
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-button-color` | neutral buttons and toggle buttons at rest, column headers, titled pane titles, spinner arrows, the date picker's week numbers, the chosen filter of the shortcuts editor | `#2d334d` | `#DCDDE1` *proposed* | — | background, the raised grey, a step darker than the window as dark's button is a step lighter than its surface; text on it 11.6:1, `-fx-text-secondary-color` 4.4:1, below 4.5:1 |
| `-fx-button-hover-color` | a neutral button under the pointer | `#394160` | `#D0D3DA` | `-app-bg-hover` | background |
| `-fx-button-pressed-color` | a pressed neutral button, a pressed icon button | `#475078` | `#C4C7D0` *proposed* | `-app-bg-pressed` | background; text on it 9.4:1 |

### Selection and hover
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-selection-color` | selected rows and cells of lists, tables, trees and popups, selected text | `#33488a` | `#D3DDF2` | `-app-bg-selected` | background |
| `-fx-selection-hover-color` | a selected table row or cell under the pointer | `#3f57a4` | `#C3D2F0` *proposed* | — | background, a step darker than the selection; text on it 10.4:1, a focused cell's accent outline 3.2:1 |
| `-fx-selection-text-color` | text on the selection | `#eef1fb` | `#1F2230` | `-app-text-strong` | 11.6:1 on selection |
| `-fx-selection-neutral-color` | a neutral selection: the cron input's format selector | `-fx-button-color` | `#D3DDF2` | `-app-bg-selected-neutral` | background; text on it 11.6:1 |
| `-fx-table-row-odd` | the odd rows of tables and tree tables (zebra) | `#2a2d41` | `#F2F3F5` *proposed* | — | background, a neutral light grey, 1.1:1 against the white rows; text on it 14.2:1, `-fx-text-secondary-color` 5.3:1 |
| `-fx-hover-color` | rows, menu items, icon buttons and popup entries under the pointer; in dark a blue tint, NOT white | `rgba(139, 152, 255, 0.09)` | `#D0D3DA` | `-app-bg-hover` | background |

### Shadows and tooltips
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-shadow-color` | the shadow of menus, popups and the slider's thumb | `rgba(0, 0, 0, 0.2)` | `rgba(31, 34, 48, 0.18)` | (popup shadow) | a shadow; offsets and radii as in dark |
| `-fx-shadow-strong-color` | the focused slider thumb's shadow | `rgba(0, 0, 0, 0.3)` | `rgba(31, 34, 48, 0.27)` *proposed* | — | a shadow, one and a half times the other, as in dark |
| `-fx-tooltip-color` | a tooltip's background; JavaFX's own shape, padding, font and the shape of its shadow stay in both schemes | `rgba(30, 30, 30, 0.8)` (JavaFX's) | `#FFFFFF` *proposed* | `-app-bg-field` | background, the white of the fields' popups, not the grey of menus (the design's ivory replaced); 1.3:1 against the window, its shadow sets it apart |
| `-fx-tooltip-text-color` | a tooltip's text | `#ffffff` (JavaFX's) | `#1F2230` *proposed* | `-app-text` | 15.8:1 on the tooltip |
| `-fx-tooltip-base-color` | the colour a tooltip's content derives its text colours from, the tooltip's `-fx-background` | `rgba(30, 30, 30)` (JavaFX's) | `-fx-tooltip-color` *proposed* | `-app-bg-field` | the tooltip's own colour, opaque, as in dark; JavaFX derives dark text on it |
| `-fx-tooltip-shadow-color` | a tooltip's shadow, in JavaFX's shape (a three-pass box, radius 10, 3 pixels down) | `rgba(0, 0, 0, 0.5)` (JavaFX's) | `rgba(31, 34, 48, 0.18)` *proposed* | (popup shadow) | a shadow, the popups' ink at 18 percent |

### Scroll bars, check boxes and toggle switches
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-scrollbar-color` | the scroll bar's thumb | `#4a5078` | `#8F94A1` *proposed* | — | 2.2:1 against the track (2.1:1 in dark) |
| `-fx-scrollbar-hover-color` | the thumb under the pointer | `#6871ae` | `#6F7484` *proposed* | — | 3.4:1 against the track |
| `-fx-scrollbar-track-color` | the scroll bar's track, darker than the surface so the channel reads on surface-coloured content | `#1b1d30` | `#DCDDE1` *proposed* | — | background, the raised grey, darker than the surface as in dark |
| `-fx-checkbox-bg` | the box of check boxes, radio buttons and the Markdown view's task boxes | `#1b1d2d` | `#FFFFFF` *proposed* | — | background, the white of fields |
| `-fx-checkbox-border` | the box's outline | `#575f90` | `#767B8E` *proposed* | — | 4.2:1 against the box, 3.3:1 against window and surface |
| `-fx-checkbox-border-hover` | the outline under the pointer | `#6f78b0` | `#5E6475` *proposed* | — | darker under the pointer; 5.9:1 against the box |
| `-fx-checkbox-mark` | the tick and the radio's dot on the accent | `#ffffff` | `#FFFFFF` *proposed* | — | 4.8:1 on the accent, 5.9:1 on its hover |
| `-fx-switch-track-off` | a toggle switch's track, off | `#42486e` | `#C2C4CC` *proposed* | — | the border grey, as dark pairs it; 1.4:1 against window and surface, below 3:1 (its thumb shows the switch, 4.7:1) |
| `-fx-switch-track-on` | the track, on | `#6675ff` | `#2F6FD6` *proposed* | — | the accent; 3.8:1 against window and surface |
| `-fx-switch-thumb-off` | the thumb, off | `#8a90a8` | `#5E6475` *proposed* | — | the secondary text colour, as dark pairs it; 3.4:1 against its track |
| `-fx-switch-thumb-on` | the thumb, on | `#ffffff` | `#FFFFFF` *proposed* | — | 4.8:1 against its track |

### Semantic status
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-status-normal` | NORMAL — healthy, scheduled; the success button's fill | `#4cd38a` | `#1A7F4B` | `-app-primary` | as text 4.0:1 on window, below 4.5:1; white text on it 5.0:1 |
| `-fx-status-running` | RUNNING — executing now: running text and icons, the running button's fill | `#3fd0a0` | `#0A6E6A` *proposed* | — | a deep teal; as text 4.8:1 on window and surface, 6.1:1 on the field surface; white text on it 6.1:1 |
| `-fx-status-paused` | PAUSED — manually held, also warning: paused and warning text and icons, the warning button's fill, a form field's warning outline and message | `#e0a84c` | `#875A10` *proposed* | — | a deep amber; as text 4.8:1 on window and surface, 6.0:1 on the field surface (also as outline); white text on it 6.0:1 |
| `-fx-status-blocked` | BLOCKED — waiting on a dependency: blocked text and icons | `#c88bff` | `#7A3EC0` *proposed* | — | a deep violet; as text 5.1:1 on window and surface, 6.4:1 on the field surface |
| `-fx-status-error` | ERROR — last run failed: error text and messages, the required marker, destructive menu items, the danger button's fill | `#e56a86` | `#B3261E` | `-app-danger-text` | 5.2:1 on window and surface, 6.5:1 on the field surface; white text on it 6.5:1 |
| `-fx-status-info` | INFO: info icons, the info button's fill | `#5d9bff` | `#1F56B3` *proposed* | — | the design's link blue (`-app-link`); as text 5.5:1 on window and surface, 6.9:1 on the field surface; white text on it 6.9:1 |
| `-fx-status-idle` | IDLE — no schedule: idle text and icons | `#6b7192` | `#5C6280` *proposed* | — | a grey indigo; as text 4.7:1 on window and surface, 6.0:1 on the field surface |
| `-fx-status-button-text-color` | text on the status fills (`.success-button` and its siblings) | `#1b1d2d` | `#FFFFFF` | `-app-on-primary` | 5.0:1 on `-fx-status-normal`, 6.5:1 on `-fx-status-error` |
| `-fx-status-normal-hover` | a success button under the pointer | `derive(-fx-status-normal, 15%)` | `#15703F` | `-app-primary-hover` | white text on it 6.1:1 |
| `-fx-status-normal-pressed` | a pressed success button | `derive(-fx-status-normal, -12%)` | `#116236` *proposed* | `-app-primary-pressed` | white text on it 7.4:1 |
| `-fx-status-running-hover` | the running button under the pointer | `derive(-fx-status-running, 15%)` | `derive(-fx-status-running, -12%)` (`#09615C`) *proposed* | — | darker under the pointer, as the primary green; follows the status colour; white text on it 7.3:1 |
| `-fx-status-running-pressed` | the running button pressed | `derive(-fx-status-running, -12%)` | `derive(-fx-status-running, -23%)` (`#085551`) *proposed* | — | follows the status colour; white text on it 8.7:1 |
| `-fx-status-paused-hover` | the warning button under the pointer | `derive(-fx-status-paused, 15%)` | `derive(-fx-status-paused, -12%)` (`#774F0E`) *proposed* | — | darker under the pointer; white text on it 7.2:1 |
| `-fx-status-paused-pressed` | the warning button pressed | `derive(-fx-status-paused, -12%)` | `derive(-fx-status-paused, -23%)` (`#68450C`) *proposed* | — | white text on it 8.6:1 |
| `-fx-status-error-hover` | a danger button's fill under the pointer | `derive(-fx-status-error, 15%)` | `#9C1F18` *proposed* | `-app-danger-text-hover` | the danger text's colour under the pointer, as the fill; white text on it 8.0:1 |
| `-fx-status-error-pressed` | a pressed danger button's fill | `derive(-fx-status-error, -12%)` | `#8A1D17` *proposed* | — | white text on it 9.3:1 |
| `-fx-status-info-hover` | the info button under the pointer | `derive(-fx-status-info, 15%)` | `derive(-fx-status-info, -12%)` (`#1B4D9E`) *proposed* | — | darker under the pointer; white text on it 8.1:1 |
| `-fx-status-info-pressed` | the info button pressed | `derive(-fx-status-info, -12%)` | `derive(-fx-status-info, -23%)` (`#18448A`) *proposed* | — | white text on it 9.4:1 |

### Danger, error and key caps
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-danger-border` | the danger button's outline | `-fx-control-border` | `#E0B4B0` | `-app-danger-border` | 1.5:1 against window and surface, below 3:1 (the fill bounds the button) |
| `-fx-danger-border-hover` | the danger button's outline under the pointer and pressed | `-fx-control-border` | `#D99A95` | `-app-danger-border-hover` | 1.9:1 against window and surface, below 3:1 (the fill bounds the button) |
| `-fx-danger-text-hover-color` | the icon of a destructive icon button under the pointer (a key-value row's delete, a shortcut's remove) | `-fx-status-error` | `#9C1F18` | `-app-danger-text-hover` | 5.3:1 on hover |
| `-fx-error-border` | the outline of a field with an error, of a key-value key marked missing or duplicate, of a missing binding chip and of a conflicting key cap | `-fx-status-error` | `#C9372C` | `-app-error-border` | 5.2:1 against the field surface |
| `-fx-keycap-border` | the outline of the key caps of the shortcuts editor | `-fx-control-border` | `#B4B7C1` | `-app-keycap-border` | 2.0:1 against the field surface and the key cap, below 3:1 |
| `-fx-keycap-color` | the fill of those key caps | `-fx-button-color` | `#FFFFFF` | `-app-bg-field` | background, white (the design's ivory replaced); text on it 15.8:1 |

### Warning
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-warning-bg` | warning banners and badges | `#362f24` | `#F6E6C1` *proposed* | — | background, a pale amber |
| `-fx-warning-text` | text on the warning background | `#e0a84c` | `#875A10` *proposed* | — | the paused amber, as in dark; 4.9:1 on the warning background |
| `.detail-prod-badge` (a rule of the scheme's stylesheet, not a token) | the production badge's background and text | `#362f24`, `#e0a84c` | `#F6E6C1`, `#875A10` *proposed* | — | the warning colours, as in dark; 4.9:1 |

### Markdown view
| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-code-text-color` | inline code in the Markdown view, and in dark the plain code of its blocks: a soft mint in dark, a deep teal in light, apart from the accent | `#8fd6c8` | `#0F6E5C` *proposed* | `-app-code` | 6.2:1 on the code background, 4.5:1 inline on the raised content |
| `-fx-code-background-color` | the background of code and source blocks, of their copy button, of the image placeholders and error boxes and of the image open button | `#1d1f33` | `#FFFFFF` *proposed* | — | background, the white of fields, lighter than the raised content (1.4:1) so blocks stand apart as in dark, where they are darker |
| `-fx-code-block-text-color` | the plain code of a code block, between its coloured pieces, and a block without colours | `-fx-code-text-color` | `#000000` | — | black, as in Eclipse's light editors (the owner's choice); 21:1 on the code background |
| `-fx-markdown-table-row-odd` | the odd rows of a table in the Markdown view, its `-fx-table-row-odd`: the rows' own colour, no zebra stripes | `-fx-surface-color` | `-fx-field-color` | `-app-bg-field` | background, the white of fields; text on it 15.8:1 |
| `-fx-markdown-search-color` | the highlight of every match of the search but the current one: a muted amber in dark, a pale one in light, apart from the selection's blue | `derive(#e0a84c, -55%)` | `#F3D98A` *proposed* | — | background, a pale amber, apart from the selection's light blue by its hue; text on it 11.4:1 |
| `-fx-markdown-search-text-color` | the characters of those matches, shown on their highlight | `-fx-text-color` | `-fx-text-color` | — | 11.4:1 on the match |
| `-fx-markdown-search-current-color` | the highlight of the search's current match: the amber itself | `#e0a84c` | `#F0A93C` *proposed* | — | background, a full amber, 1.4:1 against the other matches' |
| `-fx-markdown-search-current-text-color` | the characters of the current match, dark on the amber in both schemes | `#1b1d2d` | `#1F2230` *proposed* | — | 7.9:1 on the current match |

### Code colours
The 23 `-fx-code-<category>-color` tokens colour the pieces of a code block coloured by its language
(`CodeCategory`). A base token holds a colour, readable on `-fx-code-background-color` (contrast
5.0:1 or more in dark, 4.6:1 or more in light). In dark a fine token refers to the token it falls
back to, which JavaFX resolves at the coloured node, so a base colour set on one view also changes
that view's fine ones that still refer to it. Light follows the default colours of Eclipse's light
editors (the owner's choice, 2026-11-19): keywords a wine red, strings a blue, comments a green,
fields and constants a deep blue, numbers, types, method names and variables black; some fine
tokens hold a colour of their own there or refer to another category's token. Colours only:
Eclipse's bold keywords are left out.

| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-code-comment-color` | comments (base) | `#8a90a8` | `#3F7F5F` | — | Eclipse's comment green; 4.8:1 on the code background |
| `-fx-code-keyword-color` | keywords of a language (base) | `#c88bff` | `#7F0055` | — | Eclipse's keyword wine red; 10.3:1 on the code background |
| `-fx-code-string-color` | text in quotes (base) | `#e0a84c` | `#2A00FF` | — | Eclipse's string blue; 8.3:1 on the code background |
| `-fx-code-number-color` | numbers (base) | `#ff9e64` | `#000000` | — | black, as Eclipse leaves numbers; 21:1 on the code background |
| `-fx-code-type-color` | type names (base) | `#4cd38a` | `#000000` | — | black, as Eclipse leaves type names; 21:1 on the code background |
| `-fx-code-function-color` | a name followed by `(` (base) | `#5d9bff` | `#000000` | — | black, as Eclipse leaves method names; 21:1 on the code background |
| `-fx-code-variable-color` | Bash variables, SQL parameters, YAML anchors (base) | `#ff8fa3` | `#000000` | — | black, as Eclipse leaves local variables; 21:1 on the code background |
| `-fx-code-property-color` | keys in JSON and YAML (base) | `#7b8cff` | `#0000C0` | — | Eclipse's field blue; 11.9:1 on the code background |
| `-fx-code-tag-color` | XML and HTML tags (base) | `#ff8fa3` | `#3F7F7F` | — | Eclipse's XML tag teal grey; 4.6:1 on the code background |
| `-fx-code-doc-color` | documentation comments such as Javadoc (fine) | `-fx-code-comment-color` | `#3F5FBF` | — | Eclipse's Javadoc slate blue; 5.8:1 on the code background |
| `-fx-code-annotation-color` | Java annotations (fine) | `-fx-code-keyword-color` | `#646464` | — | Eclipse's annotation grey; 5.9:1 on the code background |
| `-fx-code-literal-color` | `true`, `false`, `null`, SQL `NULL` (fine) | `-fx-code-number-color` | `-fx-code-keyword-color` | — | a keyword, as in Eclipse |
| `-fx-code-constant-color` | Java names in capitals only, at least two characters (fine) | `-fx-code-literal-color` | `-fx-code-property-color` | — | a field, as in Eclipse |
| `-fx-code-escape-color` | escapes in strings, XML entities (fine) | `-fx-code-string-color` | `-fx-code-string-color` | — | as its fallback |
| `-fx-code-attribute-color` | XML and HTML attribute names (fine) | `-fx-code-number-color` | `#7F007F` | — | Eclipse's XML attribute purple; 9.5:1 on the code background |
| `-fx-code-operator-color` | operators (fine) | `-fx-code-text-color` | `-fx-code-block-text-color` | — | black, as the plain code |
| `-fx-code-punctuation-color` | brackets, separators (fine) | `-fx-code-text-color` | `#008080` | — | Eclipse's XML bracket teal; 4.8:1 on the code background |
| `-fx-code-heading-color` | Markdown headings (fine) | `-fx-code-function-color` | `-fx-code-function-color` | — | as its fallback: black |
| `-fx-code-link-color` | Markdown links and addresses (fine) | `-fx-code-property-color` | `-fx-code-property-color` | — | as its fallback |
| `-fx-code-code-color` | Markdown inline code and fences (fine) | `-fx-code-string-color` | `-fx-code-string-color` | — | as its fallback |
| `-fx-code-list-color` | Markdown list markers (fine) | `-fx-code-keyword-color` | `-fx-code-keyword-color` | — | as its fallback |
| `-fx-code-quote-color` | Markdown quote lines (fine) | `-fx-code-comment-color` | `-fx-code-comment-color` | — | as its fallback |
| `-fx-code-emphasis-color` | Markdown emphasis and strong emphasis (fine) | `-fx-code-text-color` | `-fx-code-block-text-color` | — | black, as the plain code |

### JavaFX's own tokens
Tokens of JavaFX's default stylesheet, which its own rules read where the theme does not style a
control itself.

| Token | Use | Dark | Light | Design | Contrast in light |
|---|---|---|---|---|---|
| `-fx-base` | JavaFX's base colour, from which it derives the controls the theme does not style | `#24273c` | `#E4E5E8` | `-app-bg-surface` | background; JavaFX derives dark text on it |
| `-fx-control-inner-background` | the inner background of controls JavaFX styles itself, the shortcuts editor's recording field | `#1b1d30` | `#FFFFFF` *proposed* | `-app-bg-field` | background, white (the design's ivory replaced); text on it 15.8:1 |
| `-fx-focus-color` | JavaFX's focus colour | `#7b8cff` | `#2F6FD6` | `-app-focus` | 3.8:1 against window and surface |
| `-fx-faint-focus-color` | JavaFX's faint focus halo, where the theme keeps it | `rgba(123, 140, 255, 0.22)` | `rgba(47, 111, 214, 0.22)` *proposed* | — | the focus blue at 22 percent, as in dark |
| `-fx-mark-color` | the marks JavaFX draws: arrows, ticks | `#d6d9e6` | `#1F2230` | `-app-text` | 12.5:1 on window and surface |
| `-fx-mark-highlight-color` | the highlight under those marks | `#d6d9e6` | `#1F2230` *proposed* | — | the mark colour itself, as in dark |
| `-fx-selection-bar` | the selection where JavaFX styles it | `#33488a` | `#D3DDF2` | `-app-bg-selected` | background |
| `-fx-selection-bar-non-focused` | the selection of a list or table without focus where JavaFX styles it | `#394160` | `#D3DDF2` *proposed* | `-app-bg-selected-neutral` | background; text on it 11.6:1 |

### The design's tokens
Every token of the colour design and the karthu-fx tokens that take its value. A design token
without a karthu-fx token has no place in the library.

| Design token | karthu-fx |
|---|---|
| `-app-bg-window` | `-fx-background-color` |
| `-app-bg-surface` | `-fx-surface-color`, `-fx-base` |
| `-app-bg-raised` | `-fx-raised-color`. The design's group headers of a list are the shortcuts editor's group rows, on `-fx-canvas-color` |
| `-app-bg-field` | `-fx-field-color`, `-fx-keycap-color`, `-fx-tooltip-color`, `-fx-tooltip-base-color`, `-fx-control-inner-background`, `-fx-markdown-table-row-odd`. White, in place of the design's ivory |
| `-app-bg-canvas` | `-fx-canvas-color` |
| `-app-canvas-dot` | none: karthu-fx draws no canvas with a dot grid |
| `-app-border` | `-fx-control-border` |
| `-app-border-subtle` | `-fx-control-border`: the design's light value is the border, so the lines between bars keep it |
| `-app-divider-row` | `-fx-row-divider-color` |
| `-app-border-disabled` | none: the theme dims a disabled control as a whole (opacity 0.5) and keeps its colours |
| `-app-text` | `-fx-text-color`, `-fx-mark-color`, `-fx-tooltip-text-color` |
| `-app-text-strong` | `-fx-selection-text-color`; a hovered item's text is `-fx-text-color`, the same colour in light |
| `-app-text-secondary` | none: karthu-fx draws menu items in `-fx-text-color` and field labels and secondary lines in `-fx-text-secondary-color`, the design's muted text |
| `-app-text-muted` | `-fx-text-secondary-color` |
| `-app-text-placeholder` | `-fx-text-placeholder-color` |
| `-app-text-disabled` | `-fx-text-disabled-color` |
| `-app-icon-disabled` | none, as `-app-border-disabled` |
| `-app-bg-hover` | `-fx-button-hover-color`, `-fx-hover-color` |
| `-app-bg-pressed` | `-fx-button-pressed-color` |
| `-app-bg-selected` | `-fx-selection-color`, `-fx-selection-bar` |
| `-app-bg-selected-neutral` | `-fx-selection-neutral-color`, `-fx-selection-bar-non-focused` |
| `-app-focus` | `-fx-accent-color`, `-fx-focus-color` |
| `-app-accent-bg` | `-fx-accent-tint-color`. The design also marks a highlighted search match with it; the Markdown view's marks stay amber (`-fx-markdown-search-color`), since the accent tint is the selection's light blue |
| `-app-accent-text` | `-fx-accent-tint-text-color` |
| `-app-link` | `-fx-link-color` |
| `-app-code` | `-fx-code-text-color` |
| `-app-primary` | `-fx-status-normal` |
| `-app-primary-hover` | `-fx-status-normal-hover` |
| `-app-primary-pressed` | `-fx-status-normal-pressed` |
| `-app-on-primary` | `-fx-status-button-text-color` |
| `-app-bg-button-disabled` | none, as `-app-border-disabled` |
| `-app-danger-text` | `-fx-status-error` |
| `-app-danger-border` | `-fx-danger-border` |
| `-app-danger-bg-hover` | none: the fill of an outlined Cancel button, while the theme's danger button stays filled; its fill under the pointer is `-fx-status-error-hover` |
| `-app-danger-bg-pressed` | none, as `-app-danger-bg-hover`; the pressed fill is `-fx-status-error-pressed` |
| `-app-danger-border-hover` | `-fx-danger-border-hover` |
| `-app-danger-text-hover` | `-fx-danger-text-hover-color`; `-fx-status-error-hover` *proposed*, the colour as the danger button's fill under the pointer |
| `-app-error-border` | `-fx-error-border` |
| `-app-status-unsaved` | none: karthu-fx marks no tab with unsaved changes |
| `-app-status-modified` | `-fx-accent-color` *proposed*, the colour of the shortcuts editor's modified dot |
| `-app-keycap-border` | `-fx-keycap-border` |
| popup shadow | `-fx-shadow-color`, `-fx-tooltip-shadow-color` |

---

## 3. Density / metrics
| Element | Value |
|---|---|
| Corner radius | `3px` (buttons, fields, cards) |
| Border width | `1px` |
| Table row height | `-fx-cell-size: 20px` |
| Table cell padding | `1 6` |
| Table font | `11.5px` |
| Tree / list cell padding | `2 4` |
| Button / field padding | `4 8` / `4` |
| Control height | ~28px |
| Toolbar padding / spacing | `4` / `4` |
| Tab height | `24px`, tab padding `2 12` |
| Status bar padding | `4 8` |

---

## 4. Component specs

**Buttons** — `.button` default (`-fx-button-color`, 1px border, radius 3, hand cursor);
hover→`-fx-button-hover-color`, pressed→`-fx-button-pressed-color`.
`.primary-button` fills `-fx-accent-color` with `-fx-accent-text-color`.
`.icon-button` transparent, hover→`-fx-hover-color`. Disabled → opacity 0.5, default cursor.
Semantic buttons `.success-button .running-button .warning-button .danger-button .info-button`
fill the matching status token with `-fx-status-button-text-color` text;
hover/pressed fill `-fx-status-<status>-hover` / `-pressed` (in Dark lighter under the pointer
and darker pressed, `derive(token, ±%)`; in Light darker under the pointer and darker still pressed);
the danger button's outline is `-fx-danger-border` (`-fx-danger-border-hover` on hover and pressed).

**Text fields / combo / spinner** — bg `-fx-field-color`, 1px `-fx-control-border`,
radius 3, prompt text `-fx-text-placeholder-color`. Focus swaps border to `-fx-accent-color`.
Read-only → `-fx-readonly-bg`. Disabled text field (and password field) → opacity 0.5, default
cursor, as a disabled button.

**TreeView / ListView** — `-fx-field-color` bg, transparent cells; hover `-fx-hover-color`,
selected `-fx-selection-color` with `-fx-selection-text-color`. Disclosure arrow is
`-fx-text-color`, rotates 90° when expanded. Keep the 2px accent left-marker look for
the active nav item.

**TableView** — `-fx-field-color` bg and rows, 1px border, radius 3, the lines between rows and
columns `-fx-row-divider-color`. Column header uses `-fx-button-color`,
bold `-fx-text-color`, the sort arrow `-fx-text-color`; zebra via `-fx-table-row-odd`. Row hover
`-fx-hover-color`, selected `-fx-selection-color` (+ `:selected:hover` = `-fx-selection-hover-color`).
With cell selection (`getSelectionModel().setCellSelectionEnabled(true)`) a selected cell looks
like a selected row, and the focused cell of a focused table has a 1px `-fx-accent-color` frame.
Color the **State** column with the `.state-*` classes below. The header's menu button
(`setTableMenuButtonVisible(true)`, `.show-hide-columns-button` at the header row's right end, as
wide as a vertical scroll bar) is flat like a column header: `-fx-button-color` with a 1px
`-fx-control-border` line on its left and bottom, `-fx-button-hover-color` on hover, its plus sign
(`.show-hide-column-image`) `-fx-text-secondary-color`, `-fx-text-color` on hover, hand cursor; its
menu of columns is the theme's context menu with check items. The line that shows where a
dragged column header will drop (`.column-resize-line`) is `-fx-accent-color`.

**State cell classes** (in `base.css`):
`.state-normal .state-running .state-paused .state-blocked .state-error .state-idle`
— each sets `-fx-text-fill` to the matching status token (all bold except idle).
Add the class to the cell in your `TableCell.updateItem` / cell factory.

**Tabs** — header bg surface, bottom 1px border; tabs are borderless (no box, no
radius). Selected tab: 2px accent underline and bold `-fx-text-color` label; inactive
labels `-fx-text-secondary-color`, brightening to `-fx-text-color` on hover. The tab
content area has no border. Close button tints to `-fx-text-color` on hover.

**Menus / context menu** — `-fx-surface-color` bg, 1px border, radius 3, soft drop shadow
`dropshadow(gaussian, -fx-shadow-color, 8, 0, 0, 2)`. Item hover/focus → `-fx-hover-color`.
Shortcuts in secondary color; destructive items use `-fx-status-error`; a disabled item's text
`-fx-text-disabled-color`. A menu of commands lies on the surface, the colour of the menu bar it
hangs from: in Nebula Light the grey of the window, apart from the white lists of fields. This
holds for the menus of the menu bar and their sub-menus, context menus, the menus of menu buttons
and the binding list of the form fields. A choice box's list is a list of values and keeps
`-fx-field-color` by its own rule, as a combo box's list.

**Scrollbars** — fixed 12px breadth (explicit `-fx-pref-width`/`-fx-pref-height`;
without it the bars collapse to zero because the arrows are shaped away); track
`-fx-scrollbar-track-color` (darker than surface so the channel reads on
surface-colored content); thumb `-fx-scrollbar-color` inset 2px, hover
`-fx-scrollbar-hover-color`; arrows hidden, but the arrow buttons keep
cross-axis-only padding — ScrollBarSkin derives its breadth and the minimum
thumb length (1.5 × breadth, ≈18px) from the buttons' cross-axis pref size,
so zero-padded buttons let the thumb collapse to sub-pixel on huge
virtualized lists.

**Scroll panes** — `.scroll-pane` sets `-fx-background: -fx-base`, its viewport is transparent.
The theme's skin (`-fx-skin` on `.scroll-pane`, `UncachedScrollPaneSkin`) is JavaFX's own without
the bitmap cache of the viewport: JavaFX draws text into that bitmap in gray where nothing opaque
lies behind it, so text inside scroll panes would lose the LCD smoothing. It covers every scroll
pane of a themed scene, those inside JavaFX's controls (a text area's) included. Scrolling draws
what the viewport shows at every step instead of moving a bitmap, as list, table and tree views
always do: they draw their rows in a virtual flow, which caches nothing, and keep JavaFX's skins.
A scroll pane that needs a skin of its own names it in a rule of its own. The theme must reach a
scene before its first CSS pass (`ThemeManager.apply(Scene)` before the scene is shown): JavaFX's
scroll pane skin, once replaced, keeps listening to its content and reports errors.

**Checkbox / radio / toggle switch** — box `-fx-checkbox-bg` w/ `-fx-checkbox-border` border
(`-fx-checkbox-border-hover` under the pointer), radius 3; selected fills `-fx-accent-color` with
a `-fx-checkbox-mark` mark. Switch: track off `-fx-switch-track-off` / on `-fx-switch-track-on`,
thumb off `-fx-switch-thumb-off` / on `-fx-switch-thumb-on`.

**Dialogs** — pane bg surface, header + button-bar separated by 1px `-fx-control-border`.
Icon colors: info/confirm/input = accent; warning = `-fx-status-paused`;
error = `-fx-status-error`. Primary action = `.primary-button`.

**Status bar** — surface bg, top 1px border, padding `4 8`, 12px secondary text;
optional colored status dot from the status tokens.

**Markdown view** (`MarkdownView`, package `org.karthu.fx.markdown`) — a scrollable document
framed like a text area. Its style classes are a public contract, also listed in the class
JavaDoc: renaming one is a breaking change. Sizes, spacing and fonts in `base.css`, colours
through tokens: the view's own, `-fx-code-text-color`, `-fx-code-background-color`,
`-fx-markdown-table-row-odd`, the 23 `-fx-code-<category>-color` of coloured code and the four
`-fx-markdown-search-` tokens of the search's marks (§2, *Markdown view* and *Code colours*), and
the theme's `-fx-raised-color` (the view's content), `-fx-field-color` (the zoom group, the rows of
tables), `-fx-surface-color`, `-fx-control-border`, `-fx-text-color`, `-fx-text-secondary-color`,
`-fx-accent-color`, `-fx-link-color` (links, images in table cells), the check box tokens (task
boxes), `-fx-hover-color`, `-fx-button-pressed-color`, `-fx-selection-color`,
`-fx-selection-text-color` and, in the full size window of images, `-fx-canvas-color`. Inside
the view a table's `-fx-table-row-odd` is `-fx-markdown-table-row-odd`, which the theme sets to
the rows' colour (no zebra stripes).
Images add no token. Two pseudo-classes: `:loose` on a list, `:focus-within` on the zoom bar's group.

| Style class | On | Look |
|---|---|---|
| `markdown-view` | the view | `-fx-raised-color` bg, 1px `-fx-control-border`, radius 3; preferred size 480 x 360 plus the frame and the zoom bar while it shows, like a text area's (set by the view, not by its content) |
| `markdown-zoom-bar` | the zoom bar on top of the view, outside its scroll pane (it does not scroll) | the tool bar's density: padding 4, 4px between its parts; transparent on the view's background; a 1px `-fx-control-border` line below it (a border); from the left the zoom group, the slider and *Reset*, one line at every width |
| `markdown-zoom-group` | the zoom group: one frame around minus, the percent field with its chevron, and plus | drawn as a text field's frame (`-fx-control-border`, `-fx-field-color`, radius 3), padding 1, exactly as high as a text field; `:focus-within`, the focus border in `-fx-accent-color`, while the field has the focus or the list of zoom levels is open (the bar sets it for the open list) |
| `markdown-zoom-out`, `markdown-zoom-in` | minus and plus, `Button`s that also carry `icon-button`, with the Bootstrap `DASH` and `PLUS` icons | the theme's icon button as high as the group's inside, padding `0 6`, hover `-fx-hover-color`, pressed `-fx-button-pressed-color`; icons 14px in `-fx-text-color`; the group's 1px dividers are their borders in `-fx-control-border` (right of minus, left of plus); disabled at 50 % and 200 %, only the icon dimmed (opacity 0.5, as a disabled icon button's), so the divider keeps full contrast |
| `markdown-zoom-field` | the percent field, a `TextField` | borderless and transparent inside the group, padding `3 1 3 6`, the text (`125%`) right-aligned in the theme's UI font; exactly as wide as `200%` |
| `markdown-zoom-levels` | the chevron in the field's cell, a `Button` that also carries `icon-button`, with the Bootstrap `CHEVRON_DOWN` icon | no divider before it; padding `0 5 0 3`; icon 10px in `-fx-text-secondary-color`, `-fx-text-color` on hover; opens the list of zoom levels, a popup with the combo box popup's class `combo-box-popup`, so it has the theme's popup list look (`.combo-box-popup .list-view`: the current preset in `-fx-selection-color`) |
| `markdown-zoom-slider` | the slider, a `Slider` from 50 to 200 | no tick marks, no labels, padding 0, preferred width 120px, minimum 60px; the theme's track and thumb, the track filled in `-fx-accent-color` from its left end to the middle of the thumb by a region `track-fill` the slider lays into its track (radius 2) |
| `markdown-zoom-reset` | *Reset*, a flat text `Button` | no frame, transparent, padding `4 8`, hover `-fx-hover-color`, pressed `-fx-button-pressed-color`; disabled at 100 %, dimmed as a disabled button |
| `markdown-document` | the box of all blocks inside the view's scroll pane | padding `6 14 14 14`, 8px between blocks; the view's margins (`leftMarginProperty()`, `rightMarginProperty()`), while set, replace the left and right padding, the top and bottom stay |
| `markdown-heading` + `markdown-heading-1` … `-6` | a heading's `TextFlow` | bold, 20 / 16 / 14 / 13 / 13 / 13px; levels 1 and 2 with a 1px `-fx-control-border` line underneath (a border, not a node) |
| `markdown-paragraph` | a paragraph's `TextFlow` | 2px line spacing, wraps to the view's width |
| `markdown-text` | every `Text` run | `-fx-text-color` |
| `markdown-strong`, `markdown-emphasis` | `Text` runs | bold, italic; combined on one run as needed |
| `markdown-code` | `Text` runs | monospace in `-fx-code-text-color`; no background box (a `Text` has none) |
| `markdown-link` | the `Text` runs of a link (inline link, autolink, bare URL), combined with the classes of the styles inside it | `-fx-link-color` (also around code and inside a quote), underlined on `:hover`; hand cursor and the address as tooltip, set by the view |
| `markdown-bullet-list`, `markdown-ordered-list` | a list: a box of its item rows | items 3px apart; with the pseudo-class `:loose` (blank lines between its items or an item's blocks in the source) 8px, like paragraphs |
| `markdown-list-item` | an item row: a box of the item's blocks | left padding 18px (24px in an ordered list), the indent per level, which holds the marker; blocks 3px apart (8px in a loose list) |
| `markdown-list-marker` | an item's marker, a `Label`: `•`, or the number and its delimiter (`3.`, `3)`); a bullet task item has none | `-fx-text-color`; its 6px right padding is the gap to the item's blocks |
| `markdown-task-list` | beside `markdown-bullet-list` or `markdown-ordered-list`: a list with at least one task item (`- [ ]`, `1. [x]`) | in an ordered list its items' left padding is 42px (24px plus box and gap), so the number keeps a plain ordered item's room left of the box and every item's text stays aligned, and a plain item's number there has a 24px right padding (its 6px, the box and the gap), so all numbers stand in one column; a bullet list keeps 18px |
| `markdown-task-item` | beside `markdown-list-item`: a task item's row; `:checked` while the task is done | no look of its own: a done item's text looks like any other, a stylesheet may strike it through or dim it under `:checked` |
| `markdown-task-box` | a task's check box in place of the bullet, or right of the number in an ordered list (the number's right edge, with its 6px padding, at the box), a `Region` with a region `mark` inside for the tick; `:checked` while the task is done | drawn from the check box tokens like the theme's `CheckBox` box: 14 x 14, radius 3, 1px `-fx-checkbox-border` on `-fx-checkbox-bg`; `:checked` background and border `-fx-accent-color`, the tick (the check box's shape, 10 x 10, lifted 1px like the check box's) in `-fx-checkbox-mark`; no hover rule, the default cursor, never focused, the same colours inside a quote. Right-aligned 4px from the item's blocks, so box and gap fill a bullet item's 18px indent, its middle a quarter of the font size above the first line's baseline (the middle of the lowercase letters) |
| `markdown-quote` | a block quote: a box of the quote's blocks | 3px `-fx-accent-color` bar on the left (a border), blocks 12px beyond it and 8px apart; text and list markers in `-fx-text-secondary-color`, code keeps `-fx-code-text-color`, links `-fx-link-color` |
| `markdown-separator` | a thematic break, a horizontal `Separator` | the theme's separator line (1px `-fx-control-border`), padding `4 0` |
| `markdown-code-block` | a fenced code block: a box with a `ScrollPane` of its own | `-fx-code-background-color`, radius 4; the scroll pane and its viewport transparent, a horizontal bar (thumb only, no track) at the bottom edge when a line is wider than the block, never a vertical one; a copy button over its top right corner |
| `markdown-code-copy` | a code block's copy button, a `Button` that also carries `icon-button`, with the Bootstrap clipboard icon | the theme's icon button (padding 4, radius 3, hand cursor), 4px from the block's top and right edges; background `-fx-code-background-color` (masks the code under it), hover adds `-fx-hover-color`, pressed `-fx-button-pressed-color`; icon 14px in `-fx-text-secondary-color`, `-fx-text-color` on hover |
| `markdown-table` | a GFM table, a `TableView`: a column per table column, a row per body row; each header and body cell a `TextFlow` of `Text` runs with the classes above | the theme's table rules (`.table-view`: border, header, hover, selection, sort arrow) plus, inside the view: rows and cells in 13px like the body text (the theme's rows are 11.5px), no zebra stripes, cell padding `4 8` with the text at the top, header labels padded `4 8` and showing the header's formatted text (`-fx-content-display: graphic-only`), `-fx-cell-size: 24px` as the floor of a row's height; the theme's sort arrow and menu button in the header (§4, TableView); selected cells in the theme's cell selection rules (`-fx-selection-color`, the focused cell's accent frame), their runs in `-fx-selection-text-color`, code and links too; with the pseudo-class `:selected-whole` while the text selection passes over the table: every body cell and column header in `-fx-selection-color`, all runs in `-fx-selection-text-color` |
| `markdown-source-block` | a source block: a block the view does not render (an HTML block, indented code) shown as its source lines | like `markdown-code-block`: `-fx-code-background-color`, radius 4, the same scroll pane and bar; no copy button |
| `markdown-block-content` | the `TextFlow` inside a code or source block's scroll pane that holds its text as `Text` runs: `markdown-text` and `markdown-code` in a code block, `markdown-text` alone in a source block. One run for the whole text, except in a code block coloured by its language: a run per piece, which also carries its category's `code-` classes (`code-keyword`, an annotation `code-annotation` and `code-keyword`), and a run per plain stretch between pieces | padding `8 12`; monospace, never wrapped, a tab stop every four columns; plain code in `-fx-code-block-text-color`, a piece in its `-fx-code-<category>-color`, source in `-fx-text-color` |
| `code-comment`, `code-keyword`, `code-string`, `code-number`, `code-type`, `code-function`, `code-variable`, `code-property`, `code-tag` (base), `code-doc`, `code-annotation`, `code-literal`, `code-constant`, `code-escape`, `code-attribute`, `code-operator`, `code-punctuation`, `code-heading`, `code-link`, `code-code`, `code-list`, `code-quote`, `code-emphasis` (fine) | the run of a piece in a code block coloured by its language, beside `markdown-text` and `markdown-code`: the class of its category and of every category its fallback chain passes (a doc comment `code-doc` and `code-comment`, a constant `code-constant`, `code-literal` and `code-number`, a heading `code-heading` and `code-function`, while operator, punctuation and emphasis carry only their own) | `-fx-fill` from the category's `-fx-code-<category>-color` (§2, *Code colours*), under `.markdown-view .markdown-code-block`, base classes first and fine ones after them, so the finest class of a run wins; colour only, no bold, no italic |
| `markdown-selection` | the highlight behind selected characters, a `Path`, in a layer over the runs of a heading's, a paragraph's or a code or source block's flow (an unmanaged `TextFlow`, clipped to the selection) that shows the selected characters again on the highlight. Over a selected image, block or inline, a `Path` of its own over the picture, placeholder or error box | `-fx-fill: -fx-selection-color`; its own property `-fx-highlight-text-fill: -fx-selection-text-color` colours the selected characters. Over an image `-fx-opacity: 0.5` |
| `markdown-search-match` | the highlight behind every match of the search but the current one, a `Path`, in a layer over the runs of a heading's, a paragraph's, a code or source block's or a table's header or body cell's flow (an unmanaged `TextFlow`, clipped to the matches) that shows the matched characters again on it, below the current match's layer and the selection's | `-fx-fill: -fx-markdown-search-color`; `-fx-highlight-text-fill: -fx-markdown-search-text-color` colours the matched characters. In a selected table cell and a table `:selected-whole` the selection shows above it: `-fx-fill: transparent`, `-fx-highlight-text-fill: -fx-selection-text-color` |
| `markdown-search-current` | the highlight behind the current match of the search, a `Path` in a layer of its own like the other matches', above theirs and below the selection's | `-fx-fill: -fx-markdown-search-current-color`; `-fx-highlight-text-fill: -fx-markdown-search-current-text-color`; in a selected table cell and a table `:selected-whole` as `markdown-search-match` there |
| `markdown-image-block` | a block image (an image alone in its paragraph, or alone in a link alone in its paragraph), a `Region` that shows its picture, its placeholder or its error box at its top left | no look of its own: transparent, no padding, so the pointer is over the block exactly while it is over what the block shows |
| `markdown-image` | the picture of a block or inline image, an `ImageView` the view sizes: a bitmap the view shrank by its area average at the shown size in screen pixels, the decoded image where it shows at its natural size or beyond, or a playing GIF | no look of its own. The alt text is its accessible text, the title its tooltip (a linked image without a title the link's address), with a hand cursor inside a link |
| `markdown-image-inline` | an image in a sentence or a heading, a `Region` that is one node of its line's `TextFlow`, its bottom on the baseline, one character for the selection | shrinks to the width its text wraps at. Its placeholder and error box are smaller than a block's (radius 3, padding `0 3`, 4px between icon and text), so the line keeps its height |
| `markdown-image-placeholder` | what an image shows until it is decoded: in a block a `Label` with the Bootstrap `IMAGE` icon and the alt text, inline a `Label` with the icon alone | one quiet line as wide as its content: `-fx-code-background-color`, radius 4, padding `4 8`, 6px between icon and text, text and icon (14px) in `-fx-text-secondary-color` |
| `markdown-image-error` | what an image shows when it cannot be shown (missing, a format the view cannot decode, above the pixel limit): in a block an `HBox` with the Bootstrap `FILE_EARMARK_X` icon, a `Label` with the alt text and one with the note, inline a `Label` with the icon and the alt text, the image's address as the tooltip of both | like the placeholder: `-fx-code-background-color`, radius 4, padding `4 8`, 6px apart, labels and icon (14px) in `-fx-text-secondary-color` |
| `markdown-image-cell` | an image in a table cell, header or body: the Bootstrap `IMAGE` icon (a `FontIcon`) and the alt text (a `markdown-text` run), joined by a no-break space, never a picture | `-fx-link-color`, as a link's runs, inside a quote too, icon 13px. Underlined on `:hover`, with a hand cursor, since a click opens the image. In a selected cell or a table selected whole the icon takes `-fx-selection-text-color` |
| `markdown-image-open` | a block image's open button, a `Button` that also carries `icon-button`, with the Bootstrap fullscreen icon, shown while the pointer is over the image, which opens the full size window | like `markdown-code-copy`: the theme's icon button, 4px from the image's top and right edges, background `-fx-code-background-color`, hover adds `-fx-hover-color`, pressed `-fx-button-pressed-color`, icon 14px in `-fx-text-secondary-color` (`-fx-text-color` on hover), above the selection's highlight |
| `markdown-image-window` | the root of the full size window's scene, a `BorderPane`: the bar on top, the image area below | `-fx-canvas-color` |
| `markdown-image-bar` | the window's bar, an `HBox`: `markdown-image-fit` and `markdown-image-actual-size` (the theme's `Button`s *Fit* and *100 %*), `markdown-image-zoom` (a `Label` with the zoom in percent), and at its right end `markdown-image-copy` and `markdown-image-save` (the theme's `Button`s *Copy image* and *Save as*, disabled, dimmed as a disabled button, while there is no image to copy or no file to save) | like the view's zoom bar: padding 4, spacing 4, a 1px line in `-fx-control-border` below it, on `-fx-surface-color`, the zoom in `-fx-text-secondary-color` |
| `markdown-image-viewer` | the window's image area, which shows the image fitted or zoomed and moves it, with an open hand, closed while dragging | `-fx-canvas-color` |
| `markdown-image-note` | a `Label` in the middle of the image area where no image can show: the note of a missing, unsupported or too large image | `-fx-text-secondary-color` |

Lists: the view lays each marker into its row's left padding, right-aligned against the item's
blocks and on the baseline of their first line, as a browser puts a marker outside the item's box.
So the blocks of all items of a list start at the same place even when `9.` and `10.` differ in
width, and a marker wider than the padding reaches further left instead of being cut. A nested
list sits inside its parent's row, so each level indents by one more padding.

Quotes: the secondary text colour comes from rules under `.markdown-quote`, which outweigh the
general run rules. A run style with a colour of its own (code, and links) needs its own rule under
`.markdown-quote` too, or it turns secondary inside a quote.

Links: a link's runs are `Text` nodes of the paragraph's text flow, so a long link wraps with the
paragraph. The link rule comes after the code rule (in the document and in the quote), so code in
a link takes the link colour and keeps its monospace font. A click hands the address to the
application's handler (`onLinkActionProperty()`); the view never opens anything itself.

Monospace: JavaFX CSS reads only the **first** family of an `-fx-font-family` list and falls
back to the system font if it is missing, so a CSS list cannot express "JetBrains Mono NL, else
Cascadia Mono, else Consolas". The view sets the first installed of these (else JavaFX's
`Monospaced`) on its code runs itself, as an inline style that a stylesheet does not override,
inline code, code blocks and source blocks alike, so `base.css` holds no monospace family. All
three draw no ligatures: Cascadia Code and JetBrains Mono would join `|---|`, `->` or `!=` into
one glyph and hide the source the reader is meant to see. The choice is one internal helper of the
module (`org.karthu.fx.font.MonospaceFont`, not exported), which the cron input of the form
collection uses for its expression too, so both pick the same font the same way.

Code blocks: the block is as tall as its code plus the horizontal bar when the bar shows, so the
bar covers no line and the block never scrolls vertically. The code scrolls sideways through the
padding of `markdown-block-content`, so the bar sits at the block's bottom edge. The mouse wheel
over a block scrolls the document (the block passes every vertical scroll on; with the shortcut
key the view zooms, see *Zoom bar*); shift plus wheel,
which JavaFX delivers as a horizontal scroll, a touchpad's sideways swipe or the bar move the
block. Code inside a quote keeps `-fx-code-text-color` through the quote's code rule. A tab moves
to the next multiple of four code columns, set on the block's flow (`-fx-tab-size` in an
application's stylesheet still wins).

Coloured code: a code block whose fence names a language the view knows (`getCodeLanguages()`,
at first `java`, `json`, `yaml`/`yml`, `sql`, `xml`/`html`/`xhtml`, `bash`/`sh`/`shell` and
`markdown`/`md`) shows a run per piece its lexer found, with the piece's `code-` classes, and a
plain run per stretch between pieces. A block without a language, in one the view does not know,
with more lines than `codeHighlightLineLimitProperty()` (500 by default) or whose lexer fails
stays one run in `-fx-code-block-text-color` (the code colour in dark, black in light). The 23
`-fx-code-<category>-color` tokens are defined at the root in `dark.css` and `light.css`, a base
one with a colour of its scheme, a fine one in dark as a reference to its fallback, and JavaFX resolves such a reference from the coloured run upwards: a
base colour set on one view (`setStyle`, or a stylesheet of the view) also changes that view's
fine categories that still refer to it, and a fine token set alone changes only its own runs.
The `code-` rules in `base.css` sit under `.markdown-view .markdown-code-block`, three classes
like the quote's code rule and after it, so a coloured block in a quote keeps its colours, and
they outweigh an application's plain `.code-keyword { -fx-fill: ... }`: colours change through
the tokens. An application's rule on a `code-` class may still set a weight, a posture or an
underline. Bold and italic show only when the code font has those faces (JavaFX finds none of a
variable font such as Cascadia Mono), an underline always shows. Selecting, copying, the copy
button, sideways scrolling, zoom and LCD text work in a coloured block as in any other.

Copying: a code block's copy button is the block's child after its scroll pane, so it lies over
the code and stays put when the code scrolls sideways; its background is the code's, so the end
of a long first line disappears under it instead of showing through the icon. Its tooltip is
`copyCodeTextProperty()`. The view's context menu is the theme's `.context-menu`; its entries
depend on where it is opened: on a table's cell or header *Copy*, *Copy with header* and *Select
column*, anywhere else (a code or source block, text, between blocks) *Copy*, then, after a
separator, *Copy Markdown*, which copies the whole Markdown source. *Copy* copies the view's
current selection where the menu is opened in it: a table's selected cells; the text selected
across the document on a code or source block or a table it reaches, and anywhere in text. Labels, English by default and
applied to the menu at once when changed: `copyTextProperty()`, `copyWithHeaderTextProperty()`,
`selectColumnTextProperty()`, `copyMarkdownTextProperty()`.
Source blocks have no button.

Selecting: all text the view shows is selected as in a browser, across blocks, and copied with
the shortcut key plus C as plain text with markers and quote prefixes: headings, paragraphs, list
items, quotes, the code of code and source blocks (a double click there selects a word as code has
it, a triple click a line) and tables as a whole. A `TextFlow`, a heading's, a paragraph's or a
code or source block's, cannot colour part of a run (JavaFX ignores a run's own selection inside a
flow), so the view lays a layer over the flow's runs: an unmanaged `TextFlow` with copies of the
runs, clipped to the selection, holding a `Path` (`markdown-selection`) in `-fx-selection-color`,
as a selected list row has it, under the copies. A `Text` has no CSS property for the colour of
its selected characters, so the highlight carries it as `-fx-highlight-text-fill` (a text field's
name for it, `-fx-selection-text-color` here) and the view hands it to the copies. The highlight
thus covers the flow's own selected characters, and the copies show them on it in
`-fx-selection-text-color`. The pointer over text is the text
cursor (set by the view; links keep the hand). A table the selection passes over is selected
whole: its `:selected-whole` pseudo-class draws every cell and header as selected while the
table's own selection of cells stays empty; a press on a cell still selects cells. One selection at
a time across the view: the text selection or a table's cells. An inline image is one character of
its line for the selection, a block image is selected whole as a table is, and both copy as their
alt text. A selected image has a `Path` (`markdown-selection`) of its own over its picture,
placeholder or error box, half see-through (`-fx-opacity: 0.5`), so the image shows under the
selection colour. The layer of the characters around an inline image leaves the image out.

Tables: a table is as tall as its header and all its rows, each row as tall as its tallest cell,
whose text wraps to the column's width; the view computes that height for every width, so a table
never shows a vertical bar. The mouse wheel over a table scrolls the document, as over a code
block. The columns share the view's width as a web browser lays out a table: none narrower than
its longest word, all growing evenly from there, a short column up to the width of its widest
cell on one line, the long columns taking the rest, so no column jumps while the view's width
changes; when every cell fits on one line, the columns share the width left over in proportion
to their widest cells. A table that cannot fit even at its longest words keeps that width and
scrolls sideways in a box of its own, under the theme's horizontal `ScrollBar`, which covers no
row; shift plus wheel, a touchpad's sideways swipe or the bar move it, as a code block. Each
column is aligned as its delimiter cell says (`:--` left, `:-:` centre, `--:` right), header
included. A row's `-fx-cell-size` must stay 24px or more: below 24px JavaFX gives every row
exactly that height, and wrapped text would be cut. The text in a table inside a quote turns
secondary like the quote's text; code and links keep their colours. The reader sorts by a click
on a header (the theme's sort arrow, and its sort dots when shift plus a click adds a second
column), drags headers to move columns and header edges to resize them, and shows and hides
columns through the header's menu button, which lies over the right end of the last column's
header: that header's text wraps left of it, as a sorted header's text wraps left of its arrow.
The reader selects cells as in a spreadsheet (click, drag, shift or the shortcut key plus a
click, shift plus the arrow keys, shift plus space for whole rows, the shortcut key plus A); a
selected cell looks as the theme's cell selection draws it, and since the runs have no `.text`
class, a rule under `.markdown-table .table-cell:selected` gives them the selection's text
colour, code and links included (their own colours would be hard to read on the selection). A
sort clears the selection. The shortcut key plus C copies the selected cells as tab-separated text
(the bounding rectangle over the shown columns, unselected cells empty).

Source blocks: what the view does not render (HTML blocks and indented code) appears as its
source lines in the same box as a code block, so nothing of a document disappears. Its text is a
plain `markdown-text` run in `-fx-text-color`, so it reads as source, not as code; inside a quote
it turns secondary like the quote's text. Lines keep their own indentation and lose the
container's (a list item's indentation, a quote's `>`).

Zoom bar: shown by default (`zoomBarVisibleProperty()`), hidden it is neither drawn nor laid out.
Only the percent field is in the tab order; a click on minus, plus, the chevron, the slider or
*Reset* leaves the focus where it is. When the bar is narrow, the slider shrinks from 120px to
60px, below that it is hidden, then *Reset*; the zoom group always keeps its width and is cut off
by the bar's edge only when the bar is narrower than the group. While the bar shows, the shortcut
key plus the mouse wheel over the document zooms one preset per notch, keeping the point under the
pointer, and the shortcut key plus `+`, `-` and `0` (by the character the key types on the
reader's layout, or the keypad's) zoom while the focus is in the view; the view consumes both, so
nothing scrolls and no menu accelerator with those keys fires. Hidden, the bar takes them along:
the wheel scrolls and the keys pass on. The texts (the tooltips of minus,
plus and the chevron, the *Reset* label) are properties of the view like the copy texts:
`zoomOutTextProperty()`, `zoomInTextProperty()`, `zoomLevelsTextProperty()`,
`resetZoomTextProperty()`. Its scroll pane and those of its code and source blocks keep the
theme's LCD smoothing through the theme's scroll pane skin (*Scroll panes* above).

Images: an image alone in its paragraph is a block (`markdown-image-block`), any other one sits in
its line (`markdown-image-inline`), one in a table cell is an icon and its alt text in the link's
colour (`markdown-image-cell`). An image is never larger than it was on the screen it was taken on
(one image pixel per screen pixel) and shrinks to the text's width and the visible height. The
picture itself has no frame, no background and no padding: a screenshot carries its own edges. The
placeholder and the error box are one quiet line on `-fx-code-background-color` in
`-fx-text-secondary-color`, so a document still loading or with a missing image reads calmly, and
the inline ones are small enough to keep the line's height. The open button over a block image
(`markdown-image-open`) is the code block's copy button with a fullscreen icon: the code background
masks the image under it, and it shows only while the pointer is over the image, so it never
covers the picture for good. A selected image takes the selection's highlight at half opacity
(*Selecting* above). The full size window (`markdown-image-window`) has a bar with the density and
the line of the view's zoom bar on `-fx-surface-color`, the image on `-fx-canvas-color` below it,
and a quiet note where no image can show. No new token: an image's look comes from the code
block's, the link's and the zoom bar's tokens. The texts (the open button's tooltip and the context
menu entry, the window's buttons, the notes of the boxes) are properties of the view like the copy
texts: `openImageTextProperty()`, `fitImageTextProperty()`, `actualImageSizeTextProperty()`,
`copyImageTextProperty()`, `saveImageTextProperty()`, `imageMissingTextProperty()`,
`imageUnsupportedTextProperty()`, `imageTooLargeTextProperty()`.

**Key-value editor** (`KeyValueEditor`, package `org.karthu.fx.components`) — a title, one row
per pair and an add button below, 6px apart; rows 4px apart, a row's columns 6px apart. Rules in
`base.css` under `.key-value-editor`, colours through the theme's tokens only; the text fields and
the CheckBox keep the theme's own rules.

| Style class | On | Look |
|---|---|---|
| `key-value-editor` | the editor, a `VBox` | spacing 6 |
| `key-value-title` | the title `Label` (hidden when the title is empty) | bold, `-fx-text-color` |
| `key-value-rows` | the box of all rows | spacing 4 |
| `key-value-row` | a row, an `HBox`: CheckBox, key, value, lock, delete (CheckBox and lock columns can be hidden) | spacing 6; key and value grow alike (pref width 160) |
| `key-value-row-inactive` | a row whose pair is switched off | its key and value fields at opacity 0.5, still editable; the CheckBox at full strength |
| `key-value-enabled` | a row's `CheckBox` | the theme's check box |
| `key-value-key`, `key-value-value` | a row's text fields; the value a `PasswordField` while the row is masked | the theme's text field; read-only (`:readonly`) when the editor is not editable |
| `key-value-key-missing`, `key-value-key-duplicate` | a key field: an active row with a value but no key, unless `emptyKeyAllowed`; a key another active row has too, while duplicates are forbidden (the empty key counts with `emptyKeyAllowed`) | 1px `-fx-error-border` border, also while focused; marks only, input is never refused |
| `key-value-lock` | a row's lock, a `ToggleButton` bound to the entry's masked flag, Bootstrap `UNLOCK` / `LOCK` | flat like `icon-button` (transparent, hover `-fx-hover-color`, no accent fill when selected); icon 14px in `-fx-text-secondary-color`, closed in `-fx-accent-color` |
| `key-value-delete` | a row's delete `Button`, also `icon-button`, Bootstrap `X` | icon 16px in `-fx-text-secondary-color`, `-fx-danger-text-hover-color` on hover |
| `key-value-add` | the add `Button`, Bootstrap `PLUS` | the theme's button, icon 14px in `-fx-text-color` |

**Shortcuts editor and dialog** (`ShortcutsEditor`, `ShortcutsDialog`, package `org.karthu.fx.command`) —
the user's view of a window's commands and their keys: a bar (search field, the segmented filters
*All / Modified / Conflicts*, *Reset all*), a header row naming the columns in capitals, the list of
group lines and command rows, and a footer with the counter and, in the dialog, *Cancel* and *Apply*.
Rows are 22px high (`-fx-fixed-cell-size`): the dialog's 760 × 660 shows about 18 commands in four
open groups beside two folded ones, and the list scrolls beyond.
The command column takes 55 % of the list's width in the header and in every row, and both indent
the names by 26px, so the titles stand over the columns. Rules in `base.css` under *Shortcuts
editor*, colours through the theme's tokens only, no token of its own: `-fx-surface-color` (the
editor, the dialog), `-fx-field-color` (through the list, its body), `-fx-canvas-color` (the band
of a group's line), `-fx-control-border` (the header's and footer's lines, the filters' frame, the
context mark's frame), `-fx-keycap-border` and `-fx-keycap-color` (the key caps' frame and fill),
`-fx-button-color` (the chosen filter), `-fx-text-color`, `-fx-text-secondary-color`
(the column titles, the group's count, the context mark, the field's hint, an
override's hint, the counter, the row's icons, and through the list's placeholder the empty list's
text), `-fx-text-placeholder-color` (*Not assigned*), `-fx-accent-color` (the modified dot, the
reset icon, the recording field's frame, a conflict's fix, `-fx-accent-hover-color` under the
pointer), `-fx-control-inner-background` (the recording field), `-fx-status-error` (a refused key's
reason, a conflict's key letters and hint, the counter's conflicts), `-fx-error-border` (a
conflict's key cap frames), `-fx-danger-text-hover-color` (the × under the pointer),
`-fx-hover-color` and, through the list, `-fx-selection-color` and `-fx-selection-text-color`. The key caps and the segmented filters are no public controls; their
style classes, like all the others here, are a public contract, also listed in the classes' JavaDoc:
renaming one is a breaking change.

| Style class | On | Look |
|---|---|---|
| `shortcuts-editor` | the editor, a `VBox`: bar, header, list, footer | `-fx-surface-color` |
| `shortcuts-dialog` | the dialog's content, a `VBox` around the editor | `-fx-surface-color` |
| `shortcuts-bar` | the bar, an `HBox` | padding 8, spacing 8 |
| `shortcuts-search` | the search field, a `SearchInput` (keeps `form-search-input`): filters the rows by the command's name, its group, its context's mark and its key's display text, folded as the search inputs fold | the search input's |
| `shortcuts-filter` | the segmented filters, an `HBox` of three `ToggleButton`s in one toggle group, always one of them chosen | joined: one 1px `-fx-control-border` frame, radius 3 at the ends, `-fx-text-secondary-color` text on `-fx-surface-color`, `-fx-hover-color` under the pointer |
| `shortcuts-filter-all`, `shortcuts-filter-modified`, `shortcuts-filter-conflicts` | the filters, each a `ToggleButton`: every row, the rows whose keys differ from their defaults, the rows in a conflict | the chosen one (`:selected`) bold in `-fx-text-color` on `-fx-button-color` |
| `shortcuts-reset-all` | *Reset all*, a `Button`: every key back to its default in the working copy; disabled while every key is its default | flat: transparent, bold, `-fx-hover-color` under the pointer |
| `shortcuts-header` | the header row, an `HBox` | padding `4 8 4 0`, 1px `-fx-control-border` lines above and below; its labels 11px bold `-fx-text-secondary-color` |
| `shortcuts-header-command`, `shortcuts-header-shortcut` | the column titles, `Label`s, in capitals | the command title indented 26px |
| `shortcuts-list` | the list, a `ListView` (keeps `list-view`) | cells 22px high, padding `0 8 0 0` |
| `shortcuts-no-match` | the list's placeholder, a `Label`: *No matching commands* while the search and the filter leave no row; hidden while the registry has no command | the theme's list placeholder, `-fx-text-secondary-color` |
| `shortcuts-group-row` | a group's line, a list cell | a `-fx-canvas-color` band; the list's hover and selection |
| `shortcuts-group-content` | the group line's content, an `HBox`: arrow, name, count | padding `0 0 0 8`, spacing 6 |
| `shortcuts-group-arrow` | its fold arrow, a Bootstrap `CHEVRON_DOWN` icon, `CHEVRON_RIGHT` while folded | 12px, `-fx-text-secondary-color` |
| `shortcuts-group-name`, `shortcuts-group-count` | its name and the number of its commands that show, `Label`s | the name bold, the count `-fx-text-secondary-color` |
| `shortcuts-row` | a command's row, a list cell | the list's cell, its hover and selection |
| `shortcuts-command-column` | the row's command column, an `HBox`: name, dot, mark | padding `0 8 0 26`, spacing 6 |
| `shortcuts-command-name` | the command's name, a `Label` | `-fx-text-color` |
| `shortcuts-modified-dot` | the dot after the name of a modified command, a `Region` | a 6px circle in `-fx-accent-color` |
| `shortcuts-context-mark` | the context's name after the name of a command whose key holds only in a part of the window, a `Label` | 10px, `-fx-text-secondary-color`, 1px `-fx-control-border` frame, radius 3 |
| `shortcuts-shortcut-column` | the row's shortcut column, a `StackPane` holding the key line, *Not assigned* or the recording field | left aligned, grows |
| `shortcuts-key-line` | the key line, an `HBox`: the key caps, then the hint and the fix | spacing 8 |
| `shortcuts-key-caps` | a key's caps, an `HBox` | spacing 3 |
| `shortcuts-key-cap` | one cap, a `Label`: a modifier, the key's name or the character | 11px, padding `0 5`, `-fx-keycap-color` in a 1px `-fx-keycap-border` frame, radius 3; in a conflict framed in `-fx-error-border` and lettered in `-fx-status-error` |
| `shortcuts-hint` | the hint after the key caps, a `Label`: *Also used by …* naming the other commands of a conflict, else *Overrides …* naming the commands of enclosing contexts the key overrides; shows only then | `-fx-text-secondary-color`; in a conflict `-fx-status-error`, also on the selected row; shrinks with an ellipsis |
| `shortcuts-fix` | the conflict's one-click fix after the hint, a `Hyperlink`, not focus traversable: *Remove there* takes the key from the other commands; shows only while one of them is not fixed | the theme's link, `-fx-link-color`, `-fx-accent-hover-color` under the pointer, no padding |
| `shortcuts-not-assigned` | *Not assigned*, a `Label` | `-fx-text-placeholder-color` |
| `shortcuts-row-content` | the row's content, an `HBox`: the columns and the buttons | — |
| `shortcuts-edit`, `shortcuts-remove`, `shortcuts-reset` | the pencil (Bootstrap `PENCIL`), the × (`X`) and the reset (`ARROW_COUNTERCLOCKWISE`), `Button`s beside `icon-button`, not focus traversable | icons 13px in `-fx-text-secondary-color`, the × in `-fx-danger-text-hover-color` under the pointer, the reset in `-fx-accent-color`; pencil and × on the row under the pointer and the selected row, the reset on a modified row, none on a fixed row |
| `shortcuts-recorder` | the recording field, an `HBox` in the shortcut column: the held modifiers as key caps, the message, the hint | `-fx-control-inner-background` in a 1px `-fx-accent-color` frame, radius 3, spacing 6 |
| `shortcuts-recorder-message` | *Press keys…* or a refused key's reason, a `Label` | `-fx-text-color` |
| `shortcuts-recorder-hint` | *Esc to cancel* at the field's end, a `Label` | `-fx-text-secondary-color` |
| `shortcuts-footer` | the footer, an `HBox`: counter, then the buttons | padding 8, spacing 8, a 1px `-fx-control-border` line above |
| `shortcuts-count` | the counter, an `HBox` of `Label`s | `-fx-text-secondary-color` |
| `shortcuts-count-commands`, `shortcuts-count-modified`, `shortcuts-count-conflicts`, `shortcuts-count-not-assigned` | the counter's parts | the conflicts in `-fx-status-error` while there is one |
| `shortcuts-count-separator` | the ` · ` between the parts | — |
| `shortcuts-footer-buttons` | the footer's buttons, an `HBox` at its right end | spacing 8; buttons at least 80px wide |
| `shortcuts-dialog-cancel` | the dialog's *Cancel*, a `Button`, the cancel button (Esc) | the theme's button |
| `shortcuts-dialog-apply` | the dialog's *Apply*, a `Button` beside `primary-button`, not a default button (Enter records a key) | the theme's primary button |

| Pseudo-class | On | Look |
|---|---|---|
| `:modified` | `shortcuts-row`, while the key differs from the command's default | the modified dot and the reset button show |
| `:not-assigned` | `shortcuts-row`, while the command has no key | *Not assigned* in place of the key caps |
| `:fixed` | `shortcuts-row` of a fixed command | no pencil, × or reset |
| `:recording` | `shortcuts-row`, while it records a new key | the recording field in place of the key caps, no buttons |
| `:conflict` | `shortcuts-row`, while another command of the same context, fixed or not, has its key | the key caps and *Also used by …* in `-fx-status-error`, the fix while one of the others is not fixed |
| `:override` | `shortcuts-row`, while a command of an enclosing context (the window, or a part whose node is an ancestor of this part's) has its key | *Overrides …* muted, unless the row is in a conflict too, whose hint it then shows |
| `:conflict` | `shortcuts-count-conflicts`, while there is a conflict | the number in `-fx-status-error` |
| `:collapsed` | `shortcuts-group-row`, while the group is folded; while a search is on, every group with a match is open unless folded in that search | the arrow points right; the group's rows are not in the list |
| `:refused` | `shortcuts-recorder`, while it shows why the last key was refused | the message in `-fx-status-error` |
| `:selected`, `:hover` | `shortcuts-row`, `shortcuts-group-row` (the list cell's own) | the list's selection and hover; the row's pencil and × show |

**Form fields** (package `org.karthu.fx.form`) — a collection of standardized form inputs that
look and behave alike: text, password, number (with spinner arrows), combo, editable combo, text
area, path and cron, each in the same shell, and the form row that holds any of them. Every input
implements `FormInput` (its node, its value, its binding, read-only, its state and the messages it
reports itself). The search field (`SearchInput`) and the search field with suggestions
(`SuggestionInput`) sit in the same shell and look like the inputs next to them, but are no form
inputs: they have no value in the form sense, no row and no binding.
Every input type sits in one shell, `FieldShell`: one border around the JavaFX control it wraps (its
content), an optional leading graphic left of it, its small trailing icons and its own controls, so
the input reads as one control, with the state, read-only, the binding and the drop look shared. Single-line text inputs are built on
`FieldFrame`, the shell around one borderless text field. The look comes from the text field's rules,
never from fixed pixel values: in `base.css` the shell (style class `form-frame`) shares the text
field's rules under *Text fields* (grouped selectors: `.text-field, .form-frame` for the border,
background, radius and padding, `.text-field:focused, .form-frame:focus-within` for the focus border,
and the read-only and disabled rules alike), so a change there carries over to every shell, and a
frame's inner text field keeps the text field's padding. A frame is exactly as high as a plain text
field beside it, in this theme as in any skin that maps its tokens; a skin that overrides
`.text-field` itself overrides `.form-frame` alike. The shell's own rules are in `base.css` under
*Form fields*, in sections (the frame, its trailing icons, the type's own controls, its leading
graphic, its states: warning and error, drop target, read-only; then one section per input: text,
password, number and its spinner arrows, combo, editable combo, text area, path, cron, the search
input and the suggestion input; then the binding and the row); colours
through the theme's tokens only, for every input: `-fx-control-border` (borders, the divider, the
line above the popup's footer), `-fx-field-color` (also the suggestion popup), `-fx-readonly-bg`
(read-only), `-fx-accent-color` (focus, drop target, the search's marks),
`-fx-text-color`, `-fx-text-placeholder-color` (prompts), `-fx-text-secondary-color` (muted icons,
details, the search's counter, the popup's *No matches* and footer), `-fx-status-error`
(error messages, the required mark, a missing chip's text), `-fx-error-border` (the borders of
errors), `-fx-status-paused` (warnings, the theme's warning colour, the search's counter with no
match), `-fx-selection-color` and `-fx-selection-text-color` (the popup's preselected row),
`-fx-accent-tint-color` and `-fx-accent-tint-text-color` (the binding) and, through
`icon-button`, `-fx-hover-color` (also the revealed eye and the popup's row under the pointer), and
`-fx-selection-neutral-color`, the button's `-fx-button-hover-color` and `-fx-button-pressed-color`
(the cron input's format selector). No input
has a token of its own. The style
classes and pseudo-classes are a public contract, also listed in the classes' JavaDoc: renaming one
is a breaking change. Like every karthu-fx control, the components are used on the JavaFX
Application Thread only.

| Class | What it is |
|---|---|
| `FormInput<T>` | the interface every input implements: `getNode()`, `getFocusTarget()` (the node that takes the focus and that a row's label names; by default the node, a frame's text field), `valueProperty()`, `getBinding()`, `readOnlyProperty()`, `stateProperty()`, `getInputMessages()` (the messages the input reports itself, which a row shows above the application's; by default none, an empty list that never changes); disabled is the node's own `disable` |
| `FieldState` | an input's state: `NORMAL`, `WARNING`, `ERROR` |
| `FieldBinding` | an input's binding to a variable, known by name: bindable, bound name, candidates, missing, and its texts |
| `BindingCandidate` | a variable that fits: a name and an optional detail (such as its type) |
| `FieldShell` | the abstract shell of every input type: its content, trailing icons, the type's own controls, the binding's `{}` icon and chip, state, read-only, the drop look |
| `FieldFrame` | the single-line text frame, a `FieldShell` around one text field |
| `FormField` | the row of a form: label, required asterisk, description tooltip, the input, the messages (the input's own, then the application's), dimmed with a disabled input |
| `FieldMessage`, `MessageSeverity` | a message under the input: a text and its severity, `ERROR`, `WARNING`, `INFO` |
| `TextInput` | the text input, a `FieldFrame` and `FormInput<String>`: a single line of text, such as a name or a title |
| `PasswordInput` | the password input, a `FieldShell` and `FormInput<String>`: masked text with an eye that reveals it where the application allows it |
| `NumberInput` | the number input, a `FieldFrame` and `FormInput<String>`: a right-aligned number, its text read as a `BigDecimal` by `toNumber()`; with its arrows on, a spinner |
| `ComboInput<T>` | the combo input, a `FieldShell` and `FormInput<T>`: a choice from a list of values of any type, each shown with its display text, with an optional empty entry |
| `EditableComboInput` | the editable combo input, a `FieldShell` and `FormInput<String>`: free text with a list of suggestions that filters while typing and never overwrites what was typed |
| `TextAreaInput` | the text area input, a `FieldShell` and `FormInput<String>`: several lines, resizable by the user with a grip |
| `PathInput` | the path input, a `FieldFrame` and `FormInput<String>`: a file or directory chooser field |
| `PathMode` | what a path input chooses: `OPEN` (a file to open), `SAVE` (a file to save), `DIRECTORY` |
| `PathChooser` | the dialog a path input opens, replaceable; `PathChooser.Request` is what it is asked, `PathChooser.standard()` JavaFX's file and directory dialogs |
| `PathFilter` | a file filter of the open and save dialogs: a description and its patterns |
| `DirectoryMemory` | where the last used directory is kept: in memory, on a Java Preferences node, or the application's own store; one shared memory per application |
| `CronInput` | the cron input, a `FieldFrame` and `FormInput<String>`: a Unix or Quartz cron expression in monospace with a format selector, reporting what it means or what is wrong as its own message |
| `CronFormat` | the syntax of a cron expression: `UNIX` (the Linux crontab), `QUARTZ` (Quartz's), shown as *Unix* and *Quartz* |
| `SearchInput` | the search field, a `FieldFrame` and no `FormInput`: a magnifier, the text with the placeholder *Search*, and a clear button while there is text; it filters a `FilteredList` with one call, counts the matches in the field and hands out highlight nodes that mark the matches in the application's cells |
| `SuggestionInput<T>` | the search field with a popup of suggestions, a `SearchInput`: the application's own objects as entries, the matching ones in a popup under the field with the typed parts in bold, chosen with the arrows and Enter or a click; the chosen entry as an event and a property |

The showcase's sidebar group *Form Inputs* starts with an *Overview* page: every input type in a
row (*Text Input*, *Password Input*, *Number Input*, *Spinner*, *Combo Input*, *Editable Combo
Input*, *Text Area Input*, *Path Input*, *Cron Input*, *Search Input*), the states default, focused, disabled, error, bound and
read-only in columns, every field filled and bindable, so that the types can be compared: equal
heights, the same icons in the same places, the same look per state. The search input's
row, the last, is built through the shell, as the search input is no form input: its states are the
shell's, and its *Bound* cell stays empty. The *Focused* column
shows the focus border without the focus, which only one field of a page can have; the type names
are never shortened, the fields give way in a narrow window. One page per type follows, in this
order, each with a *States* card (every state side by side, with the type's own), a card that puts
the input beside other fields (all but the text area's page) and a field to *Try it out* with a
switch for each state:

- *Text Input*: beside a plain text field (at rest, with a prompt text, read-only, disabled); the
  try-out field's value shown in quotes with its length.
- *Password Input*: its own states the eye, revealed, read-only and bound with the eye; beside a
  plain text field and JavaFX's password field; a text field under the try-out field to paste into,
  to see what reached the clipboard.
- *Number Input*: its own states decimal, exponent, whole numbers, prompt text and the spinner's
  (spinner, step 0.25, bound, read-only, disabled, outside the range); beside a text field and a
  path input, and a spinner; the try-out switches *Whole numbers*, *Range 0 to 100*, *Arrows* and
  *Step 0.25*, with the showcase playing the application (a text that is no number an error, a
  number outside the range a warning).
- *Combo Input*: its own states the empty entry, with and without labels, a value not in the list;
  beside a plain text field; the try-out switches *Labels* and *Empty entry*.
- *Editable Combo Input*: its own states a text that is no suggestion and without suggestions;
  beside a plain text field and a combo input; a try-out field with fourteen suggestions and the
  switch *Suggestions*.
- *Text Area Input*: its own states no wrapping, more lines than rows, two rows and resized, each
  in a form row; the try-out switches *Wrap text*, *Prompt text* and the rows, and buttons that
  remember, restore and reset the dragged size.
- *Path Input*: beside a plain text field; the modes, filters, base directory, drop and paste, and
  the row's messages.
- *Cron Input*: its own six states first (Unix valid, Quartz valid, focused, an error by a
  Quartz rule, an error by the wrong format, disabled), then bound, variable missing, read-only,
  fixed format and empty with the placeholder; beside a plain text field and a combo input; the
  try-out switch *Format selectable* and a read-out of the field's value, format, problem and
  description.
- *Search Input*, with its own cards instead of the comparison: the states
  empty with the placeholder, focused with text, disabled and read-only; *Filtering a list*, a search
  field over sample workflows whose rows mark the matches; *Suggestions*, simple names (one too long
  for its row, ending in an ellipsis) beside person cards with the footer (*{0} of {1} people*),
  drawn by the page's own cell factory from the public API (the person type, the initials avatar
  and the card are the showcase's, not karthu-fx's); a try-out suggestion input that also filters
  its names, with its chevron, the switches *Disabled*, *Read-only* and *Chevron*, and a read-out of
  its text, its counter, its chosen entry and the last choice its handler heard.

*Shell* (`FieldShell`): its children, left to right, are the type's leading graphic, if it has one
(the search input's magnifier), the content (the control the type wraps),
the trailing icons (`getIcons()`), the type's own controls (such as a divider and a chevron, or
stacked arrows) and the `{}` icon, always last; while bound the chip lies over the content. By
default it lays them out on one line: the content fills the shell left of the trailing nodes, which
sit at the right end in their order, centred vertically, 1px apart, 3px of padding after the last,
each as high as the content allows up to its own maximum height (an icon keeps its height, the
divider spans the content's height); the shell is as high as its content and grows in width only.
A leading graphic sits at the left end, centred vertically in the same way, and the content starts
right after it; it is no control (out of the tab order, mouse transparent, so a press on it focuses
the field as a press on the shell's empty space does), greys out with a disabled shell and stays
while read-only or bound. Without one, every field lays out exactly as before: the content starts
at the shell's left edge.
The right padding is the room after the last trailing node: a shell without one (a text or number
input that is not bindable, a password input without its eye) leaves it out, so its text reaches
the border as in a read-only shell, and a right-aligned number ends 4px from the border at rest and
read-only alike.
The content is the focus target unless the type names another; the icons and own controls are not
focus traversable, so Tab passes a field in one step. The shell styles the icons of `getIcons()`
but never changes their disabling: an icon is disabled by whoever added it (the path input disables
its browse icon while bound). Read-only takes the icons out of the shell; the own controls stay as
the type sets them, so a type hides those that change the value and keeps the others (a text area's
grip). One guard, `userMayChangeValue()` (protected), decides whether the user may change the value
now: not while disabled, read-only or bound; a frame's paste and cut, the path input's dialog and
drop and a type's arrows and lists ask it.

A field type builds on the shell through protected hooks: `setContent(Node)` (the control it wraps,
replaceable, as a password input swaps its masked and plain fields), `setFocusTarget(Node)` (a focus
target other than the content), `setLeadingGraphic(Node)` (one graphic left of the content, such as
a magnifier; null for none), `getOwnControls()` with `createDivider()` and `styleAsIcon(...)`,
`updateContentEditable(boolean)` (how the content and the own controls follow read-only and the
binding; by default a text input control is made editable or not) and `updateContentHidden(boolean)`
(how the value is hidden while bound; by default an empty clip), and, for a shape of its own (a text
area with the icons at the top), `layoutChildren()` and the size methods with the layout helpers
`trailingNodes()`, `layoutTrailing(...)`, `trailingWidth(...)`, `trailingPrefHeight(...)`,
`layoutChip(...)` and `chipInset()`.

*Alike in every type* (what the overview compares, and `FormInputsAlikeTest` checks across all
types): every single-line input is exactly as high as a plain text field in every state (the text
area keeps its rows); the `{}` icon sits at the same place at the right end, as large, beside the
first line; the chip starts where the text starts (4px in, the text field's padding), also in the
combo input, whose combo box has no padding of its own and whose cell holds the text; each state
draws the same border and background; disabled dims every text and icon once, to the opacity of a
disabled button, also the combo input's shown value, which JavaFX's own stylesheet would dim a
second time; the room after the last icon exists only after an icon; the icons, chevrons, arrows,
the eye and the grip are out of the tab order. A type's own shape stays its own: the text area's
icons at the top, the number's right alignment.

*Frame* (`FieldFrame`): the shell's single-line layout around one text field, its content and focus
target. The frame's text field pastes through the protected `pastedText(String)` hook, which the
path input overrides.

| Style class | On | Look |
|---|---|---|
| `form-frame` | the shell (every input type), a `Region` | the text field's, from the rule it shares with `.text-field`: `-fx-field-color` with a 1px `-fx-control-border` (background layers, insets `0, 1`), radius 3; its own padding `0 3 0 0` in every state (the room after the last trailing node, left out by the layout while there is none, as while read-only without icons); on one line it grows in width, never in height |
| `form-shell-content` | the content, the control the type wraps | dimmed once with a disabled shell (opacity 1 inside it); a type removes its control's own frame and background in its own rules |
| `form-frame-text` | a frame's inner `TextField` (keeps `text-field`, also `form-shell-content`) | no border, no background, also while focused; the text field's padding from `.text-field` (`4`), so the text sits where it sits in a plain text field |
| `form-frame-icon` | each trailing icon, a `Button` that also carries `icon-button`, graphic only; also a type's own icon-like control styled with `styleAsIcon(...)` | the theme's icon button (transparent, hover `-fx-hover-color`, hand cursor, radius 3) with padding `3`; icon 14px in `-fx-text-secondary-color`, `-fx-text-color` on hover; its tooltip on hover names its shortcut where it has one (the chevrons and the spinner arrows have no tooltip, as JavaFX's combo box and spinner) |
| `form-shell-divider` | the shell's divider, a `Region` among a type's own controls (`createDivider()`), as before a chevron or the stacked arrows | a 1px line in `-fx-control-border` over the content's height, between the frame's top and bottom borders (background insets `1 0 1 0`) |
| `form-shell-leading` | the shell's leading graphic (`setLeadingGraphic(...)`), a region holding an icon, left of the content, where a type has one | padding `0 3 0 6` (the icon 6px from the outer edge, as a trailing icon ends 6px from the right edge, and with the content's own padding the same gap to the text as from the text to a trailing icon); its icon 14px in `-fx-text-secondary-color`, a trailing icon at rest, no hover; opacity 1 in a disabled frame (dimmed once) |

| Pseudo-class | On | Look |
|---|---|---|
| `:focus-within` | `form-frame` (JavaFX's own) | 1px `-fx-accent-color` border around text and icons together, the focused text field's |
| `:disabled` | `form-frame` | opacity 0.5, as a disabled button; the content (`form-shell-content`, a frame's inner text field), the leading graphic and the icons keep opacity 1, and so do the parts JavaFX's own rules would dim again inside it (a combo box's cell and editor, a text area's grip, the chip), so the shell is dimmed once, not twice (opacity multiplies down the scene graph) |
| `:warning` | `form-frame`, while its state is `WARNING` | 1px `-fx-status-paused` border around text and icons, also while focused (it replaces the focus border) |
| `:error` | `form-frame`, while its state is `ERROR` | 1px `-fx-error-border` border around text and icons, also while focused (it replaces the focus border) |
| `:drop-target` | `form-frame`, while a drag the input accepts hovers over it (the shell's, set by the input through the shell's protected `setDropTarget`, as the path input does; a refused drag sets nothing) | 1px `-fx-accent-color` border around text and icons, as the focus border; during the drag it replaces the warning and error borders, also while focused |
| `:readonly` | `form-frame`, while the input is read-only | the read-only text field's background, `-fx-readonly-bg`, with its 1px `-fx-control-border`; no padding of its own: without icons the layout leaves out the frame's room after them, and an own control that stays (the eye, the grip) keeps it; the focus border and the warning and error borders stay, on the read-only background; the icons are not shown |

*State* (`FieldState`: `NORMAL`, `WARNING`, `ERROR`): every input has one, `FormInput.stateProperty()`,
shown as `:warning` or `:error` (none while normal). Inside a form row the row sets it from all
its messages, the input's own and the application's together: `ERROR` if any message is an error,
else `WARNING` if any is a warning, else `NORMAL`; an info message never colours the input. An
input that stands alone gets its state set directly by the application. Replacing a row's input
moves the state to the new input and sets the old one back to normal.

*Read-only* (`FormInput.readOnlyProperty()`, shown as `:readonly`): the user can select and copy the
value but not change it, for example while a workflow runs. In a frame the text is not editable
(typing, cutting, pasting and deleting change nothing), no icon shows (the trailing icons leave the
shell and come back after, their own properties untouched; the `{}` icon hides; a type's own controls
stay as the type sets them), a bound shell shows its chip without the `×`, and no shortcut, drop or
dialog changes the value or the binding. Unlike a
disabled input, a read-only one is not greyed out and stays in the tab order, so the user can focus
it to copy. The application still sets the value, the binding and the state. Next to a plain text
field: a read-only text field shows no focus border (its `:readonly` rule follows `:focused` in
`base.css`), a read-only frame keeps it.

*Disabled* is the node's own `disable`: the shell and everything in it is dimmed once, as a disabled
button, and takes no input, no shortcut and no drop. A binding list that shows closes, and its
entries change nothing while the shell is disabled. Inside a form row the row is dimmed with it
(see *Row*, `:input-disabled`).

*Keyboard*, at the shell's focus target, a frame's text field (the icons are out of the tab order,
so Tab passes a field in one step; an icon's tooltip names its shortcut where it has one):

| Key | Does | Not while |
|---|---|---|
| Alt+Down | opens the path input's dialog (*Browse… (Alt+Down)*) | disabled, read-only, bound |
| Alt+Down | in a text, password, number or text area input: nothing, as it has no choice to open (the single-line ones pass the key on unconsumed) | |
| Alt+Down, F4, Space | open the combo input's list (Alt+Up too, as in JavaFX's combo box) | disabled, read-only, bound |
| Up, Down | in the combo input's closed field: choose the entry before or after, as JavaFX's combo box does | disabled, read-only, bound |
| Alt+Down, F4 | open the editable combo input's full list (Alt+Up too, as in JavaFX's combo box); typing opens it filtered | disabled, read-only, bound |
| Up, Down, Page Up, Page Down, Home, End | in the editable combo input's open list: move the highlight (JavaFX's list keys), writing nothing into the field | the list closed: the text field's own |
| Enter | in the editable combo input's open list: the highlighted suggestion becomes the text; without one the list only closes | the list closed: the text field's own |
| Escape | in the editable combo input's open list: closes it and keeps the text | |
| Alt+Down | opens the cron input's format list (also while bound: only the expression is bound); in the open list Up and Down choose, Enter and Escape close it, as in JavaFX's combo box | disabled, read-only, the format fixed (then it passes unconsumed) |
| Enter | in the cron input: makes the error of an invalid expression due, and passes on (a dialog's default button still gets it) | |
| Escape | in the search input: empties a field with text and is consumed, so a connected list shows every entry again; in an empty field it passes on, so a dialog still closes | read-only (then it passes on) |
| Down, Alt+Down | in the suggestion input with its popup closed: open the popup, in the empty field with every entry, with text with the matches (typing opens it too) | disabled, read-only (then Down is the text field's) |
| Up, Down, Page Up, Page Down, Home, End | in the suggestion input's open popup: move the preselection one row, the popup's visible row count (eight by default), to the first or the last row, stopping at the ends; the text field keeps the focus and the caret; with a modifier the text field's (Shift+Home selects) | the popup closed: the text field's |
| Enter | in the suggestion input's open popup: applies the preselected entry (its text into the field, the chosen entry, the handler); with nothing matching it only closes the popup | the popup closed: the text field's |
| Escape | in the suggestion input's open popup: closes it and keeps the text; a second Escape empties the field, as in the search input | |
| Tab | in the suggestion input's open popup: closes it without applying, and moves the focus on | |
| Up, Down | step a number input's value while its arrows show (the spinner) | disabled, read-only, bound, the arrows off |
| Ctrl+Space (the Control key on every platform) | opens the binding list (*Bind to a variable (Ctrl+Space)*) | disabled, read-only, the `{}` icon hidden |
| Backspace, Delete | unbind while bound, as the chip's `×` does | disabled, read-only |
| Escape | closes the binding list | |
| Shortcut+A, Shortcut+C, Shortcut+X | the text field's own: select all, copy, cut | bound: select all selects nothing, copy and cut give nothing (the value is hidden); cut also not while disabled or read-only |
| Tab, Shift+Tab in a text area input | move the focus to the next or the previous field, as in every field (never a tab in the text) | |
| Ctrl+Tab in a text area input | inserts a tab in place of the selection | disabled, read-only, bound |

*Row* (`FormField`): a `VBox`, the label line above the input, the messages under it, 4px apart.
It takes any `FormInput` and only displays: the application decides what is required and which
messages (`FieldMessage`, a text and a `MessageSeverity`: `ERROR`, `WARNING`, `INFO`) apply, and
words them; the row shows each exactly as given (no capitalisation), puts no `!` marker beside the
label and never validates. An input may also report messages of its own about its value
(`FormInput.getInputMessages()`, by default none: an empty list that never changes; an input returns
the same list every time): the row shows them first, then the application's (`getMessages()`), in
the same look, follows the input's list as it changes, and moves to the new input's list when the
input is replaced. The label always stands above the input; without a label and not required there
is no label line. The description is the label's tooltip (none while empty), and
the label is the accessible text of the input's focus target (a frame's text field), unless the
application gave that node one of its own. The messages wrap at the row's width and never widen it:
the row is as wide as its label line and its input. A message is never cut off or shortened with
an ellipsis, also in a parent that sizes it without asking for its width, and its tooltip holds its
whole text. The label and the messages are selectable, read-only text (`form-selectable-text`, a
`TextArea` without frame, background, padding or scroll bars that looks like a label): the user
selects them with the mouse and copies them with Shortcut+C or the context menu, for example an
error message into a search. They are not in the focus traversal, and Tab inside one moves on.
While its input is disabled (its own `disable` or through a parent), the row shows `:input-disabled`
and the label, the asterisk and every message are dimmed as the input is, to the opacity of a
disabled button, each once, so the whole row reads as taking no input. The row does not disable its
texts, so they stay selectable and copyable; a row disabled through a parent of its own has its
texts disabled with it, as JavaFX disables every node below.

| Style class | On | Look |
|---|---|---|
| `form-field` | the row, a `VBox` | spacing 4 |
| `form-field-label` | the label, a selectable text (`form-selectable-text`) | `-fx-text-secondary-color`, the text's size |
| `form-field-required` | the asterisk `*` after the label, a `Label`, shown while required | `-fx-status-error`, 2px after the label |
| `form-field-messages` | the box of the messages, shown while there is one | spacing 2 |
| `form-field-message` | each message, a wrapping selectable text (`form-selectable-text`) with its whole text as tooltip, in the list's order | 12px |
| `form-selectable-text` | the label and each message, a read-only `TextArea` | transparent, no padding, no scroll bars, also focused or read-only |

| Pseudo-class | On | Look |
|---|---|---|
| `:error` | `form-field-message` of severity `ERROR` | `-fx-status-error` |
| `:warning` | `form-field-message` of severity `WARNING` | `-fx-status-paused` |
| `:info` | `form-field-message` of severity `INFO` | `-fx-text-secondary-color` |
| `:input-disabled` | `form-field`, while its input's node is disabled, by its own `disable` or through a parent | `form-field-label`, `form-field-required` and every `form-field-message` at opacity 0.5, as a disabled button (no colour of their own); the texts stay selectable |

*Text input* (`TextInput`): a single line of text, such as a name or a title, on the frame without
icons of its own, so it is a plain text field with the shell's `{}` icon, chip and state borders.
The value is the text exactly as it stands in the field: typed text stays as typed (never trimmed or
refused), a paste is inserted unchanged, the empty text is no value; checking it is the
application's, through the input's state. The prompt text is a property, empty by default (null
counts as empty): a text input asks for anything, so the application names what it asks for. It has
every state of the shell (warning, error, bound with the value kept, missing variable, read-only,
disabled) and no state of its own. Keyboard: the text field's own keys, Ctrl+Space and
Backspace/Delete for the binding; it has no choice, so Alt+Down passes it unconsumed. No rule of its
own in `base.css` (section *Text input*): the frame's rules draw it, with the frame's tokens.

| Style class | On | Look |
|---|---|---|
| `form-text-input` | the input, beside `form-frame`; its text field carries the frame's `form-frame-text`. The input never carries `text-input`, JavaFX's class of every text control, which only its inner text field has | the frame's |

A text input has no pseudo-class of its own: the shell's `:warning`, `:error`, `:readonly`, `:bound`
and `:disabled` apply.

*Password input* (`PasswordInput`): a shell around two text fields of one text, one shown at a
time: a masked one (a `PasswordField`, shown by default) and a plain one, shown while the text is
revealed; typing, the caret and the selection carry over. The value is the text as entered; the
prompt text is none unless the application sets one. Revealing is off unless the application allows
it per field (`revealAllowedProperty()`, default false): only then does the eye (Bootstrap `EYE`,
tooltip *Show password*) show as the input's own control, before `{}`. It reveals the text and gives
the field the focus; revealed, it shows the slashed eye (Bootstrap `EYE_SLASH`, tooltip *Hide
password*) on the hover background, and masks the text again. Leaving the field (another node takes
the focus, or the window loses it) masks the text again, and so do binding, disabling and revealing
no longer allowed; `revealedProperty()` tells the state, which only the user changes. A masked text
never reaches the clipboard: copy and cut do nothing, by shortcut (Shortcut+C, Shortcut+X,
Ctrl+Insert, Shift+Delete), by the context menu (whose *Copy* and *Cut* call the same methods) and
by code, and JavaFX's text fields start no drag. Revealed, the text behaves like a text field's. A
paste works masked and revealed, not while disabled, read-only or bound. The focus target is the
field shown, so it moves with revealing; a form row's label names either field. Bound: the text is
masked and hidden, the eye disabled. Read-only: the eye stays (revealing changes no value), the
masked text cannot be copied, the revealed one can. The field has no choice: Alt+Down passes it.
Rules in `base.css` under *Password input*; tokens: those of the frame, and `-fx-hover-color` and
`-fx-text-color` for the revealed eye.

| Style class | On | Look |
|---|---|---|
| `form-password-input` | the field, beside `form-frame` | the frame's, also the room after the eye while read-only |
| `form-password-masked` | the masked field, a `PasswordField` (keeps `text-field`, `password-field`), beside `form-frame-text` | a frame's inner text field: no border, no background |
| `form-password-plain` | the plain field, a `TextField`, shown while revealed, beside `form-frame-text` | a frame's inner text field: no border, no background |
| `form-password-reveal` | the eye, a `Button` beside `form-frame-icon` and `icon-button`, not focus traversable, shown while revealing is allowed | a trailing icon; tooltip *Show password* / *Hide password* |

| Pseudo-class | On | Look |
|---|---|---|
| `:reveal-allowed` | `form-password-input`, while revealing is allowed (the eye shows) | no rule of the theme's: a hook for an application's own stylesheet |
| `:revealed` | `form-password-reveal`, while the text is revealed | the slashed eye on `-fx-hover-color`, the icon in `-fx-text-color`, as under the pointer |

*Number input* (`NumberInput`): a frame for a number, without icons of its own, its text
right-aligned (the prompt text too), so numbers read as numbers. The value is the text exactly as
typed: a text that is no number stays in the field, so the user sees the typo and the application
reports it (as an error under the field, through the row); nothing typed is refused, corrected or
reverted. `toNumber()` gives the text as a `BigDecimal`, empty when it is no number: whitespace
around it is ignored, a dot is the decimal separator, there is no grouping, only ASCII digits count,
and an exponent is read as `BigDecimal` reads it (`1.0E21`, as `Double.toString` writes it); the
number keeps the decimals as written (`1.50` has two). While `wholeNumbersProperty()` is on (default
off), only an optional sign and digits are a number: `1.0` and `1e3` are none. `minProperty()` and
`maxProperty()` (`BigDecimal`, null for no bound) and `stepProperty()` (default 1) are the rules for
stepping the value; they never limit typing, so a typed value outside the range stays as typed, for
the application to report (the showcase reports it as a warning). The prompt text is empty unless
the application sets one, such as the range or the unit. Alt+Down passes the field (it has no
choice); Ctrl+Space, Backspace and Delete are the shell's. While bound, the chip lies at the left,
where it lies in every input. Rules in `base.css` under *Number input*; no token of its own. The
showcase's *Number Input* page shows every state, the number beside a text field and a path input,
and a field to try out (whole numbers, a range) on which the showcase plays the application.

*Spinner* (`arrowsVisibleProperty()`, default off): while on, the number input shows, after the
number, the shell's divider and two small stacked chevrons, up (Bootstrap `chevron-up`) above down
(`chevron-down`), each over half the content's height with its own hover background; the field
stays exactly as high as a text field. A press on an arrow focuses the field and steps once; held,
it repeats after 300 ms, then every 60 ms, as JavaFX's spinner does. Up and Down in the field step
(with a modifier they pass). The mouse wheel never steps, so scrolling a form changes no number. A
step adds or subtracts the step with `BigDecimal` arithmetic and stops at a bound; a number outside
the range (typed, never refused) lands on the nearer bound; an empty or invalid text gets the start
value, 0 forced into the range, and the next step goes on from there. The result is written without
exponent, with as many decimals as the number or the step has (`1.50` up by 1 is `2.50`, the start
value with the step `0.25` is `0.00`), the caret after it. A step of zero or below (null counts as
1), a minimum above the maximum and a number of more than 1000 digits written out give no rule to
follow: the step changes nothing. Bound, the arrows are disabled (dimmed as disabled icons);
read-only, the divider and the arrows are gone; disabled, the arrows are dimmed once with the field.
The arrows have no tooltip, as JavaFX's spinner. Rules in `base.css` under *Number input: spinner
arrows*, with the icon button's tokens (`-fx-hover-color`, `-fx-text-secondary-color`,
`-fx-text-color`) and the divider's `-fx-control-border`; no token of its own. The *Number Input*
page shows the spinner's states, a spinner beside the other fields and the try-out switches *Arrows*
and *Step 0.25*; the overview has a *Spinner* row.

| Style class | On | Look |
|---|---|---|
| `form-number-input` | the field, beside `form-frame` | the frame's; its inner text field (`form-frame-text`) right-aligned (`-fx-alignment: center-right`) |
| `form-shell-divider` | the spinner's divider, before the arrows (the shell's) | the shell's divider |
| `form-number-arrows` | the box of the two arrows, a `VBox`, an own control of the shell | spans the content's height; each arrow takes half of it |
| `form-number-up`, `form-number-down` | the up and the down arrow, `Button`s beside `icon-button` and `form-frame-icon`, graphic only, out of the tab order | the trailing icon's look with padding `0 4`, radius 2, the chevron centred at 10px; hover background `-fx-hover-color` over the arrow's half |

| Pseudo-class | On | Meaning |
|---|---|---|
| `:spinner` | `form-number-input`, while its arrows show (arrows on and not read-only) | no rule of the theme depends on it |

Otherwise the shell's `:warning`, `:error`, `:readonly`, `:bound` and `:disabled` apply.

*Combo input* (`ComboInput<T>`): a choice from a list, a shell around a non-editable `ComboBox<T>`
whose own frame, background, padding, arrow and focus look are hidden, so the field looks like a
text field; a chevron (Bootstrap `CHEVRON_DOWN`, styled as a trailing icon, no divider) sits right
before `{}`. The value is of any type (`valueProperty()`, null for none); the list offers
`getItems()` in their order, each shown with its display text (`displayTextProperty()`, a function
from value to text, by default `toString()`), so codes read as labels. The field shows the value's
display text, also for a value that is not in the list; without a value the prompt text (*Select a
value*) in the prompt colour. An optional choice (`emptyAllowedProperty()`) starts its list with the
empty entry (*(none)*, `emptyTextProperty()`), which sets the value to null. A click anywhere on the
field opens the list (a hand pointer over it), on the combo box, the chevron and beside them; a click
whose press closed the open list only closes it. Alt+Down, F4 and Space open it too (see
*Keyboard*). Bound, the chevron is disabled and no list opens, and the chip starts where the value's
text starts, 4px in as in every input; read-only, the display text is shown as text to select and
copy in place of the combo box, without chevron and `{}`. Disabled, the shown value is dimmed once,
as a disabled text field's text: JavaFX's own stylesheet dims a disabled list cell that shows a
chosen entry, so the cell keeps opacity 1 inside the dimmed shell. Rules in `base.css` under *Combo
input*; tokens: `-fx-text-placeholder-color` (the prompt), and the frame's. Every combo box that is a
shell's content (`form-shell-content`) loses its own look there, and its cells keep opacity 1 in a
disabled shell, so the editable combo input shares the rules, as it shares the one rule of the text
in place of the combo box (`form-combo-text`).

| Style class | On | Look |
|---|---|---|
| `form-combo-input` | the field, beside `form-frame` | the frame's; a hand pointer, the default one while bound, read-only or disabled |
| `form-combo-chevron` | the chevron, an own control of the shell, beside `form-frame-icon` and `icon-button` | the frame's trailing icon; disabled while bound, not shown while read-only |
| `form-combo-display` | the cell in the field that shows the value, the combo box's button cell (keeps `list-cell`) | the combo box's cell: padding `4`, so the text sits where a text field's does; `-fx-text-color`; opacity 1 in a disabled field (dimmed once, with the shell) |
| `form-combo-text` | the read-only text in place of the combo box, a `TextField` (keeps `text-field`, also `form-shell-content`) | no border, no background, also focused; the text field's padding |

| Pseudo-class | On | Look |
|---|---|---|
| `:prompt` | `form-combo-display`, while it shows the prompt text (no value) | `-fx-text-placeholder-color`, a text field's prompt colour |

*Editable combo input* (`EditableComboInput`): free text with suggestions, a shell around an editable
`ComboBox<String>` whose own frame, background, padding and arrow are hidden by the combo input's
rules (every combo box in a shell), so the field looks like a text field; the shell's divider and the
combo input's chevron sit right before `{}`. The value is the editor's text exactly as typed (null
counts as empty), as in a text input; it need not be a suggestion. `getSuggestions()` are texts:
typing (also deleting and pasting) opens the list with the suggestions that contain the text,
ignoring case letter by letter (no locale's rules: `ß` matches only `ß`), all of them while the text
is empty; when none contains it the list closes. The chevron and Alt+Down (F4, Alt+Up) open the full
list. In the open list the keys of JavaFX's list move the highlight without writing anything into the
field; Enter or a click chooses a suggestion, which becomes the text with the caret after it; Escape
closes the list and keeps the text; nothing is ever filled in by itself. A click whose press closed
the open list only closes it; leaving the field closes it. The prompt text is empty unless the
application sets one, as in a text input. Bound, the chevron is disabled and no list opens;
read-only, the divider and the chevron are gone and no list opens. While read-only or bound, a plain
text field (`form-combo-text`, as in the combo input) stands in for the combo box, so the hidden text
can be neither selected nor copied while bound and is selectable and copyable while read-only; the
focus moves with it. The filter is a pure rule in the non-exported package
`org.karthu.fx.form.suggestion`. Rules in `base.css` under *Editable combo input*; no token of its own
(the frame's, the divider's `-fx-control-border`, the icon's).

| Style class | On | Look |
|---|---|---|
| `form-editable-combo-input` | the field, beside `form-frame` | the frame's; the editor a text field's text cursor, never the combo box's hand |
| `form-shell-divider` | the divider before the chevron (the shell's) | the shell's divider; not shown while read-only |
| `form-combo-chevron` | the chevron (the combo input's), an own control of the shell, beside `form-frame-icon` and `icon-button` | the frame's trailing icon; disabled while bound, not shown while read-only |
| `form-combo-text` | the text in place of the combo box while read-only or bound, a `TextField` (keeps `text-field`, also `form-shell-content`) | no border, no background, also focused; the text field's padding |

The combo box's editor keeps JavaFX's `text-field` (transparent, padding `4`, from *ComboBox* in
`base.css`), so the text sits where a text field's does; in a disabled field it keeps opacity 1, so
the field is dimmed once. An editable combo input has no pseudo-class of its own: the shell's
`:warning`, `:error`, `:readonly`, `:bound` and `:disabled` apply.

*Text area input* (`TextAreaInput`): several lines in the shell. Its content is a text area whose
own frame, background and padding are gone, also while focused or read-only, so the shell draws the
border, the focus border, the state borders and the read-only background around it; its text sits
4px in, where a text field's sits, and its scroll bars show inside the shell. The value is the text
as typed, line breaks included. Lines wrap (`wrapTextProperty()`, default true), the field is four
rows high (`prefRowCountProperty()`) unless the user resized it, and the prompt text shows while it
is empty. The rows are measured as the area lays its lines out, in its font, so every text area of
one font is equally high wherever it stands (JavaFX's text area keeps a measure from before the
theme's styles came in, which made fields of one form differ by 6px). The `{}` icon (and any trailing icon) sits at the top right, in a band as high as the first
line with its room above and below, so it sits where it sits in a single-line field however tall
the field is; the grip sits at the bottom right, both in a column right of the text; while bound,
the chip lies at the top left in the same band. The grip (Bootstrap `GRIP_HORIZONTAL` turned to the
corner's diagonal, the resize cursor, tooltip *Drag to resize (double-click: default size)*) drags
the width and the height: the width never exceeds the room the parent gives (from the field's left
edge to the end of the parent's content, and a maximum width the application set), the height never
falls below two rows, and a double click goes back to the default size. `userSizeProperty()` holds
the dragged size, null for the default size; the application may store it and set it to restore it,
karthu-fx stores nothing. While set, it is the field's preferred size and its maximum width, so a
parent that fills its width does not stretch the field, and its width is bounded at every layout as a
drag is: a restored size, or a parent that narrows after a drag, never makes the field wider than
the room the parent gives (the property keeps the size as set); while null the field fills such a
parent, as a text area does. A preferred width the application sets on the field itself wins over the user's
size, as for any region. Read-only keeps the grip (resizing changes no value) and the room after it;
disabled dims the grip with the shell and it does nothing. Tab and Shift+Tab move the focus on,
Ctrl+Tab inserts a tab.

| Style class | On | Look |
|---|---|---|
| `form-text-area-input` | the field, beside `form-frame` | the frame's, also the room after the grip while read-only, as the grip stays |
| `form-text-area-text` | the text area (keeps `text-area`, also `form-shell-content`) | no border, background or padding, also while focused or read-only; its scroll pane without background and padding, its content transparent with padding `4` |
| `form-text-area-grip` | the grip at the bottom right, a `StackPane` holding its icon, not focus traversable | padding `3`, cursor `se-resize`; icon 14px in `-fx-text-secondary-color`, turned −45°, `-fx-text-color` on hover; in a disabled field opacity 1 (dimmed once) and the default cursor |

*Path input* (`PathInput`): a frame with the browse icon as its trailing icon, which shows what the
field wants: a file (Bootstrap `FILE_EARMARK`) in the open and save modes, a folder (Bootstrap
`FOLDER`) in the directory mode, changing with the mode. It
opens the file or directory dialog of its `PathMode` (open, save, directory), as Alt+Down in
the field does; its tooltip is *Browse… (Alt+Down)*. The dialog is the field's `PathChooser`,
JavaFX's file and directory dialogs unless the application replaces it. The value is the text as
typed. The prompt text is *Select a file* (open, save) or *Select a folder* (directory) unless the
application sets one.

*File filters* (`PathFilter`, `getFilters()`): the open and save dialogs list the field's filters in
their order with the first selected, then *All files* (pattern `*`) as the last, so that a file with
an unexpected extension can still be chosen; the application never adds *All files* itself. Without
filters the dialog shows every file without a filter list; the directory mode ignores the filters.
Filters change the dialog only, never the field's look.

*Base directory and last directory* (`baseDirectoryProperty()`, `DirectoryMemory`): a choice below
the field's base directory is entered relative to it with `/` (`files/data.csv`, the base itself as
`.`), any other absolute. The dialog starts in the folder of the path in the field, else in the last
used directory, else in the base directory, else in the home folder; saving suggests the file name in
the field. A choice or a drop remembers a directory itself and a file its folder, as the file
system says; a path that does not exist yet (a new file to save) remembers its folder, in the
directory mode the path itself. The last directory lives in a `DirectoryMemory`, one shared by all path inputs of an
application. It starts in memory; an application keeps it across restarts by setting
`DirectoryMemory.preferences(...)` on its own Java Preferences node as the shared memory at start-up
(the showcase does), and a host without Java Preferences (such as a native image) sets a memory on
its own store. Neither changes the field's look. Details in the JavaDoc of `PathInput` and
`DirectoryMemory`.

*Drop, paste and long paths*: a file dragged onto the field (in the directory mode a directory) is
entered like a dialog choice, relative below the base and remembered; while the field accepts the
drag, the frame shows `:drop-target`, the accent border. It accepts exactly one existing entry of its
mode's kind, whatever the filters say, and not while disabled, read-only or bound; a refused drag
looks like no drag and passes the field, so a container around it may still take it. A path pasted in one pair of double quotes (the Windows Explorer's *Copy as path*) loses the quotes,
any other paste stays as it is. When the field loses the focus, its value changes while it has none,
or a choice or drop is entered, the caret goes to the end, so a narrow field shows the file name; the
text's tooltip shows the whole value while there is one.

| Style class | On | Look |
|---|---|---|
| `form-path-input` | the field, beside `form-frame` | the frame's |
| `form-path-browse` | the browse icon (a file, or a folder in the directory mode), beside `form-frame-icon` | the frame's trailing icon |

| Pseudo-class | On | Meaning |
|---|---|---|
| `:open`, `:save`, `:directory` | `form-path-input`, exactly one at a time | the field's mode: open a file (the default), save a file, choose a directory; no rule of the theme depends on it |

*Cron input* (`CronInput`): a schedule as a cron expression, on the frame: the expression in the
text field, then the format selector, then `{}`, no divider. The text is in the module's monospace
family (see *Markdown view*, Monospace), set on the text field inline, the family only, so it keeps
the frame's size; its line is lower than the frame's own font's, so the input keeps the height of a
plain text field, the text centred. The value is the expression exactly as typed (spaces, lower
case); a paste turns tabs and line breaks into spaces, which JavaFX's text field would drop, joining
the fields. `formatProperty()` (`CronFormat`, never null: null counts as `QUARTZ`, the default) is
the syntax the input checks the text against with rules of its own (non-exported package
`org.karthu.fx.form.cron`); `formatSelectableProperty()` (default true) lets the application fix it.
`problemProperty()` and `descriptionProperty()` (read-only) follow the text and the format at once:
the message of the first broken rule, or the plain text of a valid expression, both empty while the
text is empty (whitespace only counts as empty) or the input bound. The placeholder names the
format's fields: `unixPromptTextProperty()` (*min hour day month weekday*) and
`quartzPromptTextProperty()` (*sec min hour day month weekday [year]*). The info line is the input's
own message (`getInputMessages()`, at most one): the description as an info message at once, also
while typing; the problem as an error message once due — when another node takes the focus from the
text field, on Enter, on a change of the format (the user's choice or the application's), and when
the value is set other than by typing (so a broken saved schedule shows when the form opens) — then
live until the text is valid or empty, after which a new error waits for the next due point; nothing
while empty or bound. A row shows it above the application's messages and colours the frame by it.
Only the user's choice of a format converts a valid expression of the old format when nothing is
lost (the same schedule, single spaces, weekdays as names: `0 9 * * 1-5` becomes
`0 0 9 ? * MON-FRI`, and back `0 9 * * MON-FRI`); anything else (an invalid text, Unix's *day 1 or
Monday*, a weekday step, Quartz's seconds, year, `L`, `W`, `#`) stays as it is and is checked
against the new format. Nothing is converted while bound or while the application has bound the
value. A format the application sets (`setFormat`, a binding) never converts: the text stays and is
checked against the new format, its error due at once, as for a value the application sets. So an
application that loads a saved value and its format shows the same in either order and never
rewrites the stored expression, not even through a value bound both ways.

The format selector is a small non-editable combo box of the two formats: JavaFX's frame and arrow
gone, a background of its own, radius 3, the format's name and a small chevron (Bootstrap
`CHEVRON_DOWN`), centred in the frame and lower than it, exactly as wide as its widest entry, so the
field does not change with the format. A click opens its list, Alt+Down in the text field too (see
*Keyboard*); it is not focus traversable and never takes the focus, which stays in the text field. It
stays usable while bound (only the expression binds). While the format is fixed or the input
read-only, the format's name stands in its place as plain muted text, without chevron and hover, and
nothing opens. Read-only: the text selectable and copyable, `{}` hidden, the info line as usual.
Disabled: text, selector and `{}` dimmed once with the whole row (`:input-disabled`), the info line
included. Rules in `base.css` under *Cron input*; tokens: the frame's, and
`-fx-selection-neutral-color`, `-fx-button-hover-color`, `-fx-button-pressed-color` (the selector), `-fx-text-color` (its name),
`-fx-text-secondary-color` (its chevron, the plain format text); no token of its own.

| Style class | On | Look |
|---|---|---|
| `form-cron-input` | the input, beside `form-frame`; its text field carries the frame's `form-frame-text` | the frame's; the text in the monospace family at the frame's size |
| `form-cron-format` | the format selector, a `ComboBox` (keeps `combo-box`), an own control of the shell, shown while the format is selectable and the input not read-only | `-fx-selection-neutral-color`, no border, radius 3, padding 0, `-fx-button-hover-color` under the pointer, `-fx-button-pressed-color` while its list shows; its cell padding `1 5 1 6`, the name in `-fx-text-color`, 4px gap, the chevron 10px in `-fx-text-secondary-color` (`-fx-text-color` under the pointer); JavaFX's arrow gone; opacity 1 in a disabled frame (dimmed once) |
| `form-cron-format-text` | the format's name as plain text, a `Label`, an own control of the shell in the selector's place while the format is fixed or the input read-only | `-fx-text-secondary-color`, padding `0 6`, no background, no hover; opacity 1 in a disabled frame |

A cron input has no pseudo-class of its own: the shell's `:warning`, `:error`, `:readonly`,
`:bound` and `:disabled` apply.

*Search input* (`SearchInput`): a search field in the look of the form fields, on the frame: the
magnifier (Bootstrap `SEARCH`) as the shell's leading graphic, the text with the placeholder
*Search*, and the clear button `×` (Bootstrap `X`) at the right end, an own control of the shell
styled as a trailing icon, shown while there is text and the field is neither disabled nor
read-only. It is no `FormInput`: it has no value in the form sense, no label row, no messages of its
own, and its binding stays not bindable, so the `{}` icon never shows. The field does not enforce
it: its binding is the shell's, whose properties an application can still set, so an application
leaves it so (a search field made bindable or bound would show the `{}` icon or the chip like any
field). `textProperty()` is the text
field's own text; `promptTextProperty()` defaults to *Search* (null counts as empty), so an
application can translate it. A click on the `×` empties the field and keeps the focus in the text
field, or gives it to the text field; its tooltip and accessible text are `clearTextProperty()`
(default *Clear*). Escape empties a field with text and is consumed; in an empty
field, and while read-only, it changes nothing and passes on, so a dialog around the field still
closes. The magnifier, the `×` and the arrows are out of the tab order: Tab passes the field in one
step.
Read-only: the text can be selected and copied, not changed, the `×` hides, the magnifier stays.
Disabled, also through a parent: the field is dimmed once with the magnifier, as a disabled button,
and the `×` hides.

The text is a search, matched by one rule (non-exported package `org.karthu.fx.form.search`): it
is split at any run of whitespace into words, and an entry matches when every word occurs as a
contiguous part of its text, case and accents folded on both sides (*zurich* finds *Zürich*,
*zürich* finds *Zurich*; *ß* and *ss*, ligatures and other scripts beyond their accents are not
folded). An empty search, or whitespace only, matches every entry and marks nothing. A connected
`MarkdownView` folds alike but takes the text as one piece, not as words.

The field filters a list or searches a view, one at a time: connecting one ends the other.

- **List filter:** `filter(FilteredList<T> list, Function<? super T, String> text)` connects the
  field to one list, giving the text of each entry (the one its row shows): the field sets the
  list's predicate on every change of the text, the list filtered by the current text at once. A
  second call replaces the connection, `filter(null, null)` ends it; the list connected before gets
  every entry back (its predicate null) and no longer follows the text. The lists hold the field
  only weakly, so a long-lived application list never keeps a dropped field alive.
- **View search:** `search(MarkdownView view)` connects the field to a Markdown view: the view takes
  the field's text as its search text, and from then on the two stay equal both ways, the empty
  field being the view's `null` search. A second call replaces the connection, `search(null)` ends
  it; the view connected before keeps its search and no longer follows the text. A view whose
  search text is bound one way is refused (`IllegalArgumentException`). While there is text, Enter
  goes to the view's next match and Shift+Enter to its previous one, in the view's direction, both
  consumed, also without matches, so a default button does not fire; in a `SuggestionInput` Enter
  applies the preselected entry while the popup shows. Two arrows between the counter and the `×`
  go through the matches with the mouse: the up arrow (Bootstrap `CHEVRON_UP`) to the match above,
  towards the document's start, the down arrow (`CHEVRON_DOWN`) to the match below, whatever the
  direction; each one's tooltip and accessible text name what it does in the current direction,
  `previousMatchTextProperty()` (*Previous match*) or `nextMatchTextProperty()` (*Next match*). The
  arrows are disabled without matches and with the field, and work while it is read-only. Text of
  whitespace only is no search for the view: the field shows neither the arrows nor the counter for
  it, as for an empty field. Control+F and F3 are the application's. The view holds the field only
  weakly.
- **Counter:** while a list is connected and there is text, or a view and there is text other than
  whitespace only, a counter between the text and the `×` shows the state of the search through
  `matchCountFormatProperty()`, default *{0} of {1}*: for a list the matches (the filtered list's
  size) and all entries (its source's size), for a view the current match counted from 1 (0 without
  matches) and the number of matches, *3 of 12*. `{0}` and `{1}` are replaced wherever they occur,
  everything else shows as it is (null shows nothing), so a translation never fails. It follows the
  text, the filtered list and the source list, also a change the filtered list does not show, or the
  view's matches and current match. With no match it carries `:no-match`, in the warning colour. It
  stays while read-only and disabled (it changes nothing), dimmed once with a disabled field.
- **Marks:** `highlight(String text)` returns a new `TextFlow` (`search-highlight`) for the
  application's own cells, whose runs (`Text`, `search-text`) show the text with every occurrence
  of every word of the search in runs of their own (`search-match`), overlapping and touching ones
  merged, in the positions of the original text (a mark on *Zürich* covers *Zü* exactly). It
  follows the search text by itself; the field holds it only through a weak listener, so a cell
  that drops it for a new one leaks nothing. An empty text is one empty run, which keeps a line's
  height. One node per text and cell: it shows the text it was created with. A text the
  application shows bold, such as a name, is a highlight node with the style class `search-strong`
  added: its runs bold, its marks underlined, as a bold mark would not show in a bold text.

Rules in `base.css` under *Search input*: the counter's and the marks'; the frame's, the leading
graphic's and the trailing icon's draw the rest, the arrows included. Tokens: `-fx-text-secondary-color` (the counter),
`-fx-status-paused` (the counter with no match), `-fx-text-color` (the runs), `-fx-accent-color`
(the marks); no token of its own.

| Style class | On | Look |
|---|---|---|
| `form-search-input` | the field, beside `form-frame`; its text field carries the frame's `form-frame-text` | the frame's |
| `form-search-icon` | the magnifier, a `StackPane` holding the icon, the shell's leading graphic beside `form-shell-leading` | the leading graphic's: 6px from the outer edge, 14px in `-fx-text-secondary-color`; dimmed once with a disabled field |
| `form-search-count` | the counter, a `Label`, an own control of the shell before the arrows and the `×`, not focus traversable; shown while a list is connected and there is text, or a view and there is text other than whitespace only | `-fx-text-secondary-color`, the text's size, padding `0 4`; opacity 1 in a disabled frame (dimmed once) |
| `form-search-up` | the up arrow (Bootstrap `CHEVRON_UP`), a `Button` beside `form-frame-icon` and `icon-button`, an own control of the shell after the counter, not focus traversable; tooltip *Previous match* forward, *Next match* backward; shown while a view is connected and there is text other than whitespace only | the frame's trailing icon; disabled without matches (the icon button's opacity 0.5), dimmed once with a disabled field |
| `form-search-down` | the down arrow (Bootstrap `CHEVRON_DOWN`), a `Button` beside `form-frame-icon` and `icon-button`, an own control of the shell after the up arrow, not focus traversable; tooltip *Next match* forward, *Previous match* backward; shown while a view is connected and there is text other than whitespace only | as `form-search-up` |
| `form-search-clear` | the clear button `×`, a `Button` beside `form-frame-icon` and `icon-button`, an own control of the shell, last before `{}` (which never shows), not focus traversable; tooltip *Clear* | the frame's trailing icon (hover `-fx-hover-color`, the icon in `-fx-text-color` under the pointer); shown while there is text and the field is neither disabled nor read-only |
| `search-highlight` | a highlight node, a `TextFlow` from `highlight(...)`, wherever the application puts it | — |
| `search-text` | each run of a highlight node, a `Text` | `-fx-text-color` |
| `search-match` | a marked run, beside `search-text` | in an application's cell (a `search-highlight` outside a suggestion popup): bold in `-fx-accent-color`; in a suggestion popup see *Suggestion input* |
| `search-strong` | set by the application on a highlight node whose text it shows bold, such as the name on a card, beside `search-highlight` | its runs bold, its marks (`search-match`) underlined as well, wherever it stands; no colour of its own |

| Pseudo-class | On | Look |
|---|---|---|
| `:no-match` | `form-search-count`, while nothing matches (*0 of 2*, with a view *0 of 0*) | `-fx-status-paused` |

Besides, the shell's `:readonly` and `:disabled` apply.

*Suggestion input* (`SuggestionInput<T>`): a search input with a popup of suggestions under it,
everything of the search input included (magnifier, placeholder, `×`, Escape, the matching rule; it
can also filter a list and hand out highlight nodes). `getItems()` are the application's own
objects, none null; of each entry `itemTextProperty()` gives the text that goes into the field and
that its row shows (default `String.valueOf`), `searchTextsProperty()` the texts the search is
matched against (default the item text alone; give every text the row shows, so every match shows
its mark).

- **Opening:** typing (also deleting and pasting) opens the popup once the text holds a word: the
  matching entries in the order of the items, the first preselected; it follows the typing, the
  first match preselected again, and closes when no word is left. Down or Alt+Down open it too: in
  the empty field with every entry, with text with its matches, and so does a click on the chevron
  (see *Chevron*). It never opens on the focus alone,
  never for a text the field puts in itself or the application sets while the text field has no
  focus (one set while it has the focus counts as typed), never while disabled or read-only. With no
  match it stays open with one line, `noMatchesTextProperty()` (default *No matches*), in the list's
  place.
- **Popup:** under the field, as wide as the field, up to `visibleRowCountProperty()` rows (default
  8; below 1 counts as 1), each as high as it is, then it scrolls; Page Up and Page Down move as many
  rows. The height is measured from the rows' own heights, already before the list has laid out a
  row, so the popup's window opens at its final height. Its content is a `PopupControl`'s, built at
  once; its window shows only in a shown window.
  The list never takes the focus: the field handles the keys (see *Keyboard*), so the text field
  keeps the focus and the caret. A click on a row applies its entry. It closes when the text field
  loses the focus, the window moves, the field becomes disabled or read-only, or the mouse is pressed
  in the window outside the field and the popup (the press still acts there); a press in the field
  itself, such as one that moves the caret, keeps it open, and the `×` closes it as it empties the
  field.
- **Chevron:** off by default, so the field lays out as a search input; `chevronVisibleProperty()`
  turns it on (since 1.1.0-RC2): the combo input's chevron (Bootstrap `CHEVRON_DOWN`, a trailing
  icon) at the field's right end, after the `×`, so the field works as a filterable combo box for
  the mouse too. A click opens the popup as Down does (every entry in the empty field, the matches
  with text) and gives the text field the focus; a click while the popup is open only closes it, the
  text kept (its press, in the field, kept the popup open). Out of the tab order; greyed out while
  the field is disabled or read-only, when a click does nothing. Its tooltip and accessible text are
  `chevronTextProperty()`, default *Show suggestions*; null or empty, no tooltip.
- **Rows:** by default the item text on one line through `highlight(...)`, its marks bold; a text
  longer than the row is clipped after the last character that fits whole, an ellipsis (`…`,
  `form-suggestion-ellipsis`) after it, the highlight node keeping the whole text and its marks.
  `cellFactoryProperty()`, a plain JavaFX `Callback<ListView<T>, ListCell<T>>` (null for the
  default row), draws the application's own rows, of any height, such as a card of a person, each
  text through `highlight(...)` (a bold name with `search-strong`). The application's cell does
  nothing for clicks or the selection: the field makes a click on any row apply its entry, and the
  theme draws any row (`list-cell`) in the popup's look, the preselected one (`:selected`) in the
  selection colours, the runs of its highlight nodes in `-fx-selection-text-color`, the marks keeping
  their look. A new factory builds the shown rows anew.
- **No row wider than the popup**: every row, the default one and the
  application's, gets the list's width (beside the vertical scroll bar while the list scrolls),
  whatever it asks for: the field sets each row's preferred width to zero, so no row pushes the
  list wider, and the popup never scrolls sideways. The row clips or wraps what does not fit, as it
  chooses (a highlight node, a text flow, wraps by itself); the popup's height is measured with the
  rows at that width.
- **Footer:** off by default; `footerVisibleProperty()` turns it on, under the rows (also under *No
  matches*). On the left the matches and all entries through `footerCountFormatProperty()`, default
  *{0} of {1}* (filled like the search input's counter: *2 of 14*, *2 of 14 people*), following the
  typing and the entries; on the right `footerHintTextProperty()`, default *↑↓ select · Enter apply ·
  Esc close*. Null shows nothing.
- **Applying**, in this order: the popup closes; the text becomes the item text, the caret at its
  end, without the popup opening again and without the chosen entry being cleared;
  `chosenItemProperty()` becomes the entry; `onItemChosenProperty()`, a `Consumer<T>`, receives it.
- **Chosen entry:** read and write. Any other change of the text, the user's or the application's,
  makes it null, so the field and the chosen entry never contradict each other. Set by the
  application, an entry puts its item text into the field without opening the popup (one that shows
  closes); null leaves the text alone. A bound chosen entry is left alone. The handler hears only the
  user's choices.
- **Changing entries** (items, item text, search texts) while the popup is open filter it again; the
  preselection stays on the same entry (`equals`) while it matches, else goes to the first.

Rules in `base.css` under *Suggestion input*, after the search input's: the popup's frame, its list
and rows, the line *No matches*, the marks in the popup and the footer; the chevron is drawn by the
frame's trailing icon rules. Tokens: `-fx-control-border`
(the popup's frame, the line above the footer), `-fx-field-color` (the popup's frame),
`-fx-hover-color` (a row under the pointer), `-fx-selection-color` and `-fx-selection-text-color` (the preselected row),
`-fx-text-color` (a row's marks), `-fx-text-secondary-color` (*No matches*, the footer); no token of
its own.

| Style class | On | Look |
|---|---|---|
| `form-suggestion-input` | the field, beside `form-search-input` and `form-frame` | the search input's |
| `form-suggestion-chevron` | the chevron (Bootstrap `CHEVRON_DOWN`), a `Button` beside `form-frame-icon` and `icon-button`, the shell's last own control, after the `×`, not focus traversable; tooltip *Show suggestions*; shown while turned on | the frame's trailing icon, as the combo input's `form-combo-chevron`; disabled while read-only (the icon button's opacity 0.5), dimmed once with a disabled field |
| `form-suggestion-popup` | the popup's content, a `VBox` holding the list or the line | the popup's frame, as a combo box's list: `-fx-control-border` 1px around `-fx-field-color`, radius 3, padding 1 |
| `form-suggestion-list` | the popup's list, a `ListView` (keeps `list-view`), never focused; its rows list cells (`list-cell`), by default the item text through `highlight(...)`, else the application's cells | no frame, no background of its own; a row padding `4 8`, `-fx-hover-color` under the pointer, the preselected row (`:selected`) `-fx-selection-color` |
| `form-suggestion-ellipsis` | the ellipsis of a default row whose text does not fit, a `TextFlow` beside `search-highlight` holding one run (`search-text`), shown only while the text is clipped | the look of the row's plain runs, by the rules of `search-highlight` and `search-text`; no rule of its own |
| `form-suggestion-empty` | the line *No matches*, a `Label` in the list's place | `-fx-text-secondary-color`, padding `4 8` |
| `form-suggestion-footer` | the footer under the list or the line, an `HBox`, shown while turned on | a 1px `-fx-control-border` line above it, padding `4 8`, spacing 12 |
| `form-suggestion-footer-count` | the footer's count on its left, a `Label`, never shortened | `-fx-text-secondary-color`, 12px |
| `form-suggestion-footer-hint` | the footer's key hints on its right, a `Label`, shortened first where the room is short | `-fx-text-secondary-color`, 12px |
| `search-match` | a marked run in the popup (a `search-highlight` inside `form-suggestion-popup`) | bold in `-fx-text-color`, also underlined in a `search-strong` highlight node; on the preselected row the runs in `-fx-selection-text-color`, the marks keeping their look |

A suggestion input adds no pseudo-class of its own: the search input's and the shell's apply.

*Binding* (`FieldBinding`, one per input through `FormInput.getBinding()`): an input can take its
value from a variable, which karthu-fx knows by name only. The application fills the candidates
(`BindingCandidate`: a name and an optional detail, such as the type), sets the bound name to show a
binding and the missing flag when its variable is gone, and listens to the bound name. Binding never
changes the value: while bound it is kept, hidden, and shows again after unbinding. The texts are
properties of the binding with English defaults: *Bind to a variable (Ctrl+Space)*, *Remove
binding*, *No variable fits*, *This variable does not exist*.

Where the binding appears:

- **In a shell** (every input type on `FieldShell`, built): the `{}` icon (Bootstrap `BRACES`) is
  the shell's last trailing node while the binding is bindable or bound and the input is not
  read-only. It and Ctrl+Space in the field open the binding list under the icon. While bound, a
  chip with the bound name and a small `×` (Bootstrap `X`) lies over the hidden content where its
  text starts, and the `{}` icon takes the bound look. The input disables its own icons and
  controls that act on the value (the path input's browse icon); the shell leaves an application's
  icons as the application set them. The hidden value cannot be selected, copied or cut. The focus
  target stays where it is (a frame's text field): a click on the chip focuses it, Backspace or
  Delete unbind, as the `×` and *Remove binding* do. Read-only, the `{}` icon hides and the chip has
  no `×`.
- **Without a shell** (switches, lists; the rule, not built yet): the `{}` icon sits at the right
  end of the form row's label row, and the chip replaces the input while bound. It reads the same
  `FieldBinding`, uses the same classes, texts and look (`form-frame-bind`, `form-chip`,
  `form-chip-remove`, the binding list) and follows the same read-only rule; it comes with the
  first component that needs it.

The binding list is a context menu: one entry per candidate (its name, then its detail muted, the
bound one checked), or the disabled *No variable fits* without candidates; while bound, a separator
and *Remove binding* follow. Choosing an entry binds or switches, *Remove binding* unbinds; the
user's binding, switching and unbinding clear the missing flag before they set the bound name.
Rules in `base.css` under *Frame: binding*; tokens: `-fx-accent-tint-color` (added for the binding),
`-fx-accent-tint-text-color`, `-fx-status-error`, `-fx-error-border`, `-fx-field-color`,
`-fx-readonly-bg`, `-fx-hover-color`,
`-fx-text-color`, `-fx-text-secondary-color`.

| Style class | On | Look |
|---|---|---|
| `form-frame-bind` | the `{}` icon, beside `form-frame-icon` and `icon-button` | a trailing icon; while bound the icon in `-fx-accent-tint-text-color` on `-fx-accent-tint-color` (the hover tint on top) |
| `form-chip` | the chip, a `Label` with the bound name, over the hidden text, where the text starts | `-fx-accent-tint-text-color` text on `-fx-accent-tint-color`, radius 3, padding `0 1 0 6` (`0 6 0 6` in a read-only frame, without the `×`); as high as the text, centred, never higher than the text field's content; shortened with an ellipsis when the room is short |
| `form-chip-remove` | the chip's `×`, its graphic, a `Button` that also carries `icon-button`, not focus traversable; none in a read-only frame | padding `0 1`, radius 2, icon 12px in `-fx-accent-tint-text-color`; tooltip *Remove binding* |
| `form-binding-menu` | the binding list, a `ContextMenu` (keeps `context-menu`) | the theme's context menu |
| `form-binding-candidate` | a candidate's entry, a `CheckMenuItem` whose graphic holds the texts | the theme's check menu item; its graphic 8px from the check |
| `form-binding-name` | the candidate's name, a `Label` in the entry's graphic | `-fx-text-color` |
| `form-binding-detail` | the candidate's detail, a `Label` after the name (none without a detail) | `-fx-text-secondary-color`, 12px after the name |
| `form-binding-remove` | the entry *Remove binding*, after a separator, while bound | the theme's menu item |
| `form-binding-empty` | the entry *No variable fits*, disabled, without candidates | the theme's disabled menu item |

| Pseudo-class | On | Look |
|---|---|---|
| `:bound` | `form-frame`, while bound | the `{}` icon's bound look; the chip shows, the text is hidden |
| `:missing` | `form-chip`, while the bound variable is missing | outlined: a 1px `-fx-error-border` border on `-fx-field-color` (`-fx-readonly-bg` in a read-only frame), text and `×` in `-fx-status-error`; tooltip *This variable does not exist* |
| `:disabled` | `form-frame` | the chip and its `×` keep opacity 1 inside the dimmed frame, so they are dimmed once |

---

## 5. Notes for implementation
- Window chrome: native OS title bar — style content only (no custom title bar). JavaFX does not
  colour the title bar: it stays as the platform draws it, in both schemes.
- Two stylesheets, load order: `base.css` then the scheme in effect (`dark.css` or `light.css`).
- The scheme: an application chooses it for all its windows with
  `ThemeManager.setAppearance(Appearance)`: `LIGHT`, `DARK` (the default, while the application
  chooses nothing) or `SYSTEM`, which follows the light or dark setting the
  platform reports, also when it changes while the application runs (Dark where the platform
  reports none). A change restyles every themed scene at once, shown or not, with its open
  popups, menus and tooltips. `ThemeManager.colorSchemeProperty()` holds the scheme in effect,
  System resolved, for colours an application sets in code.
- Pseudo-classes `:light` and `:dark`: the root of every themed scene carries the one of the
  scheme in effect, also a root that replaces it, so an application gives a colour of its own a
  value per scheme as a token of its own (never an `-fx-` name), under `.root:light` and
  `.root:dark`, and its rules use the token. Its stylesheets go on the scene after
  `ThemeManager.apply(Scene)`, which replaces the scene's stylesheets; a change of the scheme
  replaces only the scheme's stylesheet and leaves the application's in place.
- The pseudo-class sits on the roots of themed scenes only. A control's popup (a context menu, a
  tooltip, a combo box's list) looks its colours up through the control that owns it, so the
  application's tokens reach it. A plain `javafx.stage.Popup` is a window with a scene of its own:
  its content's styleable parents end at that scene's root, which carries no `:light` or `:dark`,
  so a token the application defines only under `.root:light` and `.root:dark` does not reach it.
  Theme the popup's scene too, `ThemeManager.apply(popup.getScene())` before it first shows: its
  root then carries the pseudo-class and follows every change, and the popup takes its owner's
  stylesheets, the application's included, each time it shows. Or define the tokens on a style
  class the popup's content carries, from the theme's own tokens, which reach it in both schemes.
- A new token goes into `dark.css` and `light.css` alike, at the same place: the two define the
  same tokens in the same order, which a test checks. A rule beyond `.root` in one (the production
  badge's) has its counterpart in the other.
- A native image that names the theme's resources one by one (GraalVM's resource configuration)
  names all three stylesheets of `/org/karthu/fx/theme/css/`: `base.css`, `dark.css` and
  `light.css`. Without `light.css` the theme has no colours to load for Light and logs the missing
  stylesheet.
- Smoothing: `-fx-font-smoothing-type: lcd` belongs to `Text` alone and is not inherited, so
  `base.css` sets it on `.text` (the text inside labels, buttons and inputs) and on the type
  `Text` (plain text nodes, such as a `TextFlow`'s runs), never on `.root` or a control. JavaFX
  falls back to gray by itself where LCD cannot work (a transparent background), and so in a node
  cached as a bitmap, which is why the theme's scroll pane skin caches no viewport (*Scroll panes*).
  Don't cache a node with text (`setCache(true)`) in the theme's scenes.
- No Modena fallback: `ThemeManager.apply(Scene)` loads `base.css` + the stylesheet of the scheme
  in effect, `dark.css` (the default) or `light.css`, and a change of the scheme replaces the one
  with the other in every themed scene.
- Don't introduce new literal hex in component rules — add a token instead.
