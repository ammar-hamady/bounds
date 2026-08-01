package com.example.bounds.ui.theme

import androidx.compose.ui.graphics.Color

// ── Amber accent ──────────────────────────────────────────────────────────────
val Amber       = Color(0xFFF5A623)
val AmberDim    = Color(0xFF2A1F0E) // badge / icon background tint

// ── Surface hierarchy ─────────────────────────────────────────────────────────
val BgPrimary   = Color(0xFF111111) // page background
val BgSurface   = Color(0xFF1C1C1E) // cards, list groups
val BgSheet     = Color(0xFF171717) // bottom-sheet background
val BgElevated  = Color(0xFF222222) // nested surfaces / icon backgrounds
val BgBanner    = Color(0xFF1E1E1E) // pill / banner background

// ── Borders & dividers ────────────────────────────────────────────────────────
val BorderDim   = Color(0xFF2A2A2A)
val DividerLine = Color(0xFF222222)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary = Color.White
val TextMuted   = Color(0xFF888888)
val TextSubtle  = Color(0xFF555555)

// ── Semantic ──────────────────────────────────────────────────────────────────
val ErrorRed    = Color(0xFFE53935)

// ── Legacy aliases (kept so Theme.kt compiles without changes) ────────────────
val Orange80         = Amber
val OrangeGrey80     = TextMuted
val Orange40         = Amber
val Orange100        = AmberDim
val DarkSurface      = BgPrimary
val SurfaceVariant   = BgSurface
