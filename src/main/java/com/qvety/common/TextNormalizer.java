package com.qvety.common;

import java.text.Normalizer;

/**
 * Folds a name for search, duplicate detection, and sorting. The displayed value is never folded;
 * the folded form is stored beside it. Rules (docs/domain/search-and-normalization.md): NFC, alef
 * variants to bare alef, ta marbuta to ha, alef maqsura to ya, tashkeel and kashida removed,
 * Arabic-Indic digits to ASCII, whitespace trimmed and collapsed, Latin lowercased.
 */
public final class TextNormalizer {

    private static final int DROP = -1;

    private TextNormalizer() {}

    public static String fold(String raw) {
        if (raw == null) {
            return "";
        }
        var text = Normalizer.normalize(raw, Normalizer.Form.NFC);
        var out = new StringBuilder(text.length());
        var pendingSpace = false;
        for (int i = 0; i < text.length(); ) {
            var cp = text.codePointAt(i);
            i += Character.charCount(cp);
            var mapped = map(cp);
            if (mapped == DROP) {
                continue;
            }
            if (Character.isWhitespace(mapped)) {
                pendingSpace = true;
                continue;
            }
            if (pendingSpace && !out.isEmpty()) {
                out.append(' ');
            }
            pendingSpace = false;
            out.appendCodePoint(mapped);
        }
        return out.toString();
    }

    private static int map(int cp) {
        return switch (cp) {
            case 0x0622, 0x0623, 0x0625, 0x0671 -> 0x0627;   // آ أ إ ٱ -> ا
            case 0x0629 -> 0x0647;                           // ة -> ه
            case 0x0649 -> 0x064A;                           // ى -> ي
            case 0x0640 -> DROP;                             // kashida (tatweel)
            default -> {
                if ((cp >= 0x064B && cp <= 0x0652) || cp == 0x0670) {
                    yield DROP;                              // harakat, shadda, sukun, superscript alef
                }
                if (cp >= 0x0660 && cp <= 0x0669) {
                    yield '0' + (cp - 0x0660);               // Arabic-Indic digits
                }
                if (cp >= 0x06F0 && cp <= 0x06F9) {
                    yield '0' + (cp - 0x06F0);               // Extended Arabic-Indic digits
                }
                yield Character.toLowerCase(cp);
            }
        };
    }
}
