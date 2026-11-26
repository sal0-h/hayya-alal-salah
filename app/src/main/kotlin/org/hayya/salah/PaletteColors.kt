// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

internal data class PaletteColors(val surface: Int, val ink: Int, val secondary: Int, val line: Int, val accent: Int)

internal fun paletteColors(palette: Palette, dark: Boolean): PaletteColors = when (palette) {
    // The TEAL key is retained for existing preferences and Direct Boot configuration.
    Palette.TEAL -> if (dark) PaletteColors(0xff14211c.toInt(), 0xffe7f0e9.toInt(), 0xffadc3b5.toInt(), 0xff3b5145.toInt(), 0xffa0d7b3.toInt())
        else PaletteColors(0xfff5f7f0.toInt(), 0xff1c2b23.toInt(), 0xff496052.toInt(), 0xffcbd7cd.toInt(), 0xff2b654b.toInt())
    Palette.INK -> if (dark) PaletteColors(0xff142033.toInt(), 0xffe9eff9.toInt(), 0xffb3c2d8.toInt(), 0xff3d506c.toInt(), 0xffa6c8ff.toInt())
        else PaletteColors(0xfff3f6fc.toInt(), 0xff1c2e47.toInt(), 0xff485c79.toInt(), 0xffc7d3e6.toInt(), 0xff204d86.toInt())
    Palette.PLUM -> if (dark) PaletteColors(0xff251c2a.toInt(), 0xfff6eaf4.toInt(), 0xffd2b8ce.toInt(), 0xff5a4358.toInt(), 0xffe5aadc.toInt())
        else PaletteColors(0xfffaf4f8.toInt(), 0xff36243a.toInt(), 0xff69536b.toInt(), 0xffe1cfe0.toInt(), 0xff743768.toInt())
}
