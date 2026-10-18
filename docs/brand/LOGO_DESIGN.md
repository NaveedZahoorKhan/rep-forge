# Rep Forge logo v2

Created with the built-in image generation tool. The RF silhouette is now reproduced as a native Android vector in `app/src/main/res/drawable/ic_launcher_foreground.xml` for crisp rendering at every density.

## Android integration

- Launcher: separate charcoal background and teal foreground adaptive-icon layers, plus a monochrome layer for themed icons.
- Splash: the same vector on charcoal, using the existing AndroidX splash-screen integration in both light and dark mode. No artificial launch delay.
- Colors: `#2DD4BF` foreground and `#0B1117` background, defined in `res/values/colors.xml`.
- The foreground is padded to keep the silhouette within launcher masks. The generated PNG remains the design reference; Android uses the vector rather than a flattened square bitmap.
- Sizing follows [Android adaptive-icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

## Initial prompt

Use case: logo-brand.
Create a finished, original logo for Rep Forge, an Android strength-training and workout logging app. Asset: a single premium app-icon logo, square composition, no presentation board.
Design a bold custom geometric R monogram with a subtle F revealed in its negative space. Its form should suggest a forged weight plate and repeated upward progression, while remaining a simple memorable abstract lettermark. Strong compact silhouette, broad clean strokes, deliberate negative-space cuts, optically balanced geometry, confident diagonal leg. Avoid making it look like a generic dumbbell clip-art icon. Prioritize immediate clarity at 24–48px.
Use one flat bright mint-teal #2DD4BF mark centered on a full-bleed deep charcoal #0B1117 square background. Mark occupies roughly 58–62% of canvas width, with generous adaptive-icon-safe margins. Straight-on perfectly flat vector-like graphic, razor sharp edges and solid fills. No rounded-square outer frame; the entire canvas is the icon background.
No wordmark, no additional text, no tiny details, no border, no shadows, no gradients, no glow, no bevel, no mockup, no photographic texture, no watermark. The result should feel modern, durable, focused and distinctive for a serious but approachable fitness product.

## Refinement prompt

Refine this Rep Forge RF monogram logo. Preserve the main interlocking R and F silhouette and centered square composition. Rebuild it as an impeccably clean FLAT two-color logo: one solid uniform mint teal fill (#2DD4BF) for the mark, and a solid opaque deep charcoal (#0B1117) full square background, including every negative space. Remove ALL cyan/green fringe, splatter, glow, noise, outlines, texture, gradients and bevels. Edges must be smooth mathematically clean vector-like contours. Keep generous margins and no text. This is the actual clean app icon artwork, not a mockup. The entire image must be opaque.
