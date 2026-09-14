# Qvety brand assets

Mark: "Cut Q" — a solid disc with the counter and the tail cut out of it as negative
space. One shape, one colour, no inner detail, never mirrored.

Geometry (viewBox 0 0 64 64):
  disc     circle cx=31 cy=30 r=23
  counter  circle cx=31 cy=30 r=8.5            (cut)
  tail     line (31,30) -> (50,49), width 9,   (cut)
           round cap, exits the bowl at 45 degrees
Content bbox: x 8, y 7, w 46.5, h 46.5.

## Files
mark.svg / mark-white.svg / mark-black.svg     mark only
wordmark.svg                                   wordmark only, ink
lockup-ltr.svg / lockup-rtl.svg                horizontal, primary teal
lockup-ltr-black.svg / lockup-rtl-black.svg    horizontal, print
lockup-stacked.svg / lockup-stacked-white.svg  stacked, login and splash
print/logo-black.png                           1200px, transparent, invoice footer
../favicon.svg                                 browser
../apple-touch-icon.png                        180x180, white on primary
../icons/icon-192.png  icon-512.png            white on teal-soft
../icons/icon-maskable-512.png                 mark inside central 80%
../og-image.png                                1200x630 link preview

## Outstanding
1. The wordmark in the lockup SVGs is live IBM Plex Sans text, not outlines. Plex's Q
   has a short straight tail and does not match the mark. Redraw the Q by hand, then
   convert all wordmark text to paths before these ship.
2. favicon.ico is not in this set — generate it from favicon.svg at 16/32/48 with any
   ICO packer; the SVG is the source of truth.
