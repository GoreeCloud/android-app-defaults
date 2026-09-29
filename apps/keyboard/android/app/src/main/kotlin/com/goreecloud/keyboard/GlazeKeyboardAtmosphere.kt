package com.goreecloud.keyboard

/**
 * Non-semantic GLAZE UI V1.2 Frosted Optical atmosphere/material boundary for
 * GoreeCloud Keyboard.
 *
 * V1.2's governing rule is: Neutral glass is the material. Color is an accent.
 * KeyboardView consumes the neutral material palette from [GlazeKeyboardTokens]. This
 * object records the additional V1.2 authority boundary so future visual work cannot
 * silently restore V1.1 chromatic substrate tinting or turn atmosphere into privacy,
 * security, identity, selection, focus, recovery, availability, or input meaning.
 *
 * Environmental aura remains optional and external to the substrate. No editor/content
 * sampling, remote color derivation, persistent sample history, semantic inference,
 * telemetry, network lookup, or animated atmosphere is authorized by this mapping.
 */
internal object GlazeKeyboardAtmosphere {
    const val DefaultMaterialTintContribution = 0f

    const val TealAsBaseMaterialAllowed = false
    const val GreenAsBaseMaterialAllowed = false
    const val AquaAsBaseMaterialAllowed = false
    const val AmberAsBaseMaterialAllowed = false
    const val BrandColorMayDefineSubstrate = false
    const val SemanticColorMayDefineSubstrate = false

    const val EnvironmentalAuraOptional = true
    const val EnvironmentalAuraMayPassThroughBackdrop = true
    const val EnvironmentalAuraMustRemainOutsideSubstrate = true

    const val EnvironmentalColorMemoryEnabled = false
    const val RemoteColorDerivationAllowed = false
    const val PersistentSampleHistoryAllowed = false
    const val SemanticInferenceAllowed = false
    const val AnimatedAtmosphereEnabled = false
}
