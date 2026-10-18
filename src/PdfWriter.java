import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A very small PDF writer for the wellness export: A4 pages, Helvetica text, simple lines and rectangles. It has no
 * dependencies, so Kalma can make a printable file without a PDF library. Text is written in WinAnsi, so Filipino
 * letters such as ñ work. Characters outside it (emoji, arrows) are written as "?".
 * Use it top to bottom: heading(), text(), bullet(), row(), chart(). Pages are added automatically.
 */
final class PdfWriter {
    static final double W = 595, H = 842, MARGIN = 54;
    private static final double LEFT = MARGIN, RIGHT = W - MARGIN, TOP = H - MARGIN, BOTTOM = MARGIN + 18;

    /** Helvetica widths (1000 units per em) for ASCII 32 to 126. */
    private static final int[] WIDTHS = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};

    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder cur;
    private double y;
    private final String footer;

    PdfWriter(String footer) {
        this.footer = footer;
        newPage();
    }

    // ----- measuring and encoding -----
    static double width(String s, double size) {
        double w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += c >= 32 && c <= 126 ? WIDTHS[c - 32] : 556;
        }
        return w * size / 1000.0;
    }

    /** Keeps only what WinAnsi can show. Line breaks and tabs become spaces. */
    static String clean(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') b.append(' ');
            else if (c == '\u2019' || c == '\u2018') b.append('\'');
            else if (c == '\u201C' || c == '\u201D') b.append('"');
            else if (c == '\u2013' || c == '\u2014') b.append('-');
            else if (c == '\u2022') b.append('-');
            else if (c >= 32 && c <= 126) b.append(c);
            else if (c >= 160 && c <= 255) b.append(c);
            else if (Character.isSurrogate(c)) { if (Character.isHighSurrogate(c)) b.append('?'); }
            else b.append('?');
        }
        return b.toString();
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
            else if (c > 126) b.append(String.format("\\%03o", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    static List<String> wrap(String text, double size, double maxWidth) {
        List<String> out = new ArrayList<>();
        String t = clean(text).trim();
        if (t.isEmpty()) { out.add(""); return out; }
        StringBuilder line = new StringBuilder();
        for (String word : t.split(" +")) {
            // a single very long word is cut so it can never run off the page
            while (width(word, size) > maxWidth) {
                int cut = word.length();
                while (cut > 1 && width(word.substring(0, cut), size) > maxWidth) cut--;
                if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                out.add(word.substring(0, cut));
                word = word.substring(cut);
            }
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (width(tryLine, size) <= maxWidth) { line.setLength(0); line.append(tryLine); }
            else { out.add(line.toString()); line.setLength(0); line.append(word); }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    // ----- page handling -----
    private void newPage() {
        cur = new StringBuilder();
        pages.add(cur);
        y = TOP;
    }

    private void need(double h) { if (y - h < BOTTOM) newPage(); }

    double remaining() { return y - BOTTOM; }

    // ----- drawing -----
    private void show(double x, double yy, String s, double size, boolean bold, double r, double g, double b) {
        cur.append(String.format(Locale.ROOT, "BT /%s %.1f Tf %.3f %.3f %.3f rg %.1f %.1f Td (%s) Tj ET\n",
                bold ? "F2" : "F1", size, r, g, b, x, yy, esc(s)));
    }

    void space(double h) { y -= h; if (y < BOTTOM) newPage(); }

    void heading(String s, double size) {
        need(size * 2.2);
        space(size * 0.4);
        show(LEFT, y - size, clean(s), size, true, 0.1, 0.1, 0.1);
        y -= size * 1.5;
    }

    /** Section title with a thin line under it. */
    void section(String s) {
        need(40);
        space(8);
        show(LEFT, y - 13, clean(s), 13, true, 0.1, 0.1, 0.1);
        y -= 18;
        cur.append(String.format(Locale.ROOT, "0.85 0.85 0.8 RG 0.8 w %.1f %.1f m %.1f %.1f l S\n", LEFT, y, RIGHT, y));
        y -= 8;
    }

    void text(String s, double size, boolean bold, double indent) { text(s, size, bold, indent, 0.2); }

    void muted(String s, double size) { text(s, size, false, 0, 0.45); }

    private void text(String s, double size, boolean bold, double indent, double gray) {
        for (String line : wrap(s, size, RIGHT - LEFT - indent)) {
            need(size * 1.4);
            show(LEFT + indent, y - size, line, size, bold, gray, gray, gray);
            y -= size * 1.4;
        }
    }

    void bullet(String s, double size) {
        List<String> lines = wrap(s, size, RIGHT - LEFT - 14);
        for (int i = 0; i < lines.size(); i++) {
            need(size * 1.4);
            if (i == 0) show(LEFT + 2, y - size, "-", size, false, 0.2, 0.2, 0.2);
            show(LEFT + 14, y - size, lines.get(i), size, false, 0.2, 0.2, 0.2);
            y -= size * 1.4;
        }
    }

    /** A table row. cols are x offsets from the left margin and widths; long text wraps inside its column. */
    void row(double[] xs, double[] ws, String[] cells, double size, boolean bold) {
        List<List<String>> wrapped = new ArrayList<>();
        int lines = 1;
        for (int i = 0; i < cells.length; i++) {
            List<String> l = wrap(cells[i], size, ws[i]);
            wrapped.add(l);
            lines = Math.max(lines, l.size());
        }
        double h = lines * size * 1.35 + 4;
        need(h);
        if (bold) {
            cur.append(String.format(Locale.ROOT, "0.99 0.96 0.83 rg %.1f %.1f %.1f %.1f re f\n", LEFT, y - h + 2, RIGHT - LEFT, h));
        }
        for (int i = 0; i < cells.length; i++)
            for (int k = 0; k < wrapped.get(i).size(); k++)
                show(LEFT + xs[i] + 3, y - size - 1 - k * size * 1.35, wrapped.get(i).get(k), size, bold, 0.15, 0.15, 0.15);
        y -= h;
        cur.append(String.format(Locale.ROOT, "0.9 0.9 0.88 RG 0.5 w %.1f %.1f m %.1f %.1f l S\n", LEFT, y + 1, RIGHT, y + 1));
    }

    /**
     * Mood and stress by day. Each value is 1 to 5 or null for "no check-in that day". Points are drawn for every
     * check-in and each point is joined to the previous check-in by a line, even when days with no check-in sit
     * between them (those days are skipped, not filled in, so the horizontal spacing still shows the gap).
     */
    void chart(List<String> labels, Double[] mood, Double[] stress, double height) {
        double h = height + 38;
        need(h);
        double x0 = LEFT + 26, x1 = RIGHT - 4, top = y - 6, bottom = top - height;
        for (int v = 1; v <= 5; v++) {
            double yy = bottom + (v - 1) / 4.0 * (height);
            cur.append(String.format(Locale.ROOT, "0.9 0.9 0.88 RG 0.5 w %.1f %.1f m %.1f %.1f l S\n", x0, yy, x1, yy));
            show(LEFT + 8, yy - 3, String.valueOf(v), 8, false, 0.45, 0.45, 0.45);
        }
        int n = labels.size();
        for (int s = 0; s < 2; s++) {
            Double[] data = s == 0 ? mood : stress;
            String col = s == 0 ? "0.85 0.65 0.05" : "0.1 0.1 0.1";
            cur.append(col).append(" RG ").append(col).append(" rg 1.4 w\n");
            Double prev = null;
            double px = 0, py = 0;
            for (int i = 0; i < n; i++) {
                if (data[i] == null) continue;
                double px2 = n == 1 ? (x0 + x1) / 2 : x0 + (x1 - x0) * i / (n - 1.0);
                double py2 = bottom + (data[i] - 1) / 4.0 * height;
                if (prev != null) cur.append(String.format(Locale.ROOT, "%.1f %.1f m %.1f %.1f l S\n", px, py, px2, py2));
                cur.append(String.format(Locale.ROOT, "%.1f %.1f 3 3 re f\n", px2 - 1.5, py2 - 1.5));
                prev = data[i];
                px = px2;
                py = py2;
            }
        }
        if (n > 0) {
            show(x0, bottom - 11, clean(labels.get(0)), 8, false, 0.45, 0.45, 0.45);
            String last = clean(labels.get(n - 1));
            show(x1 - width(last, 8), bottom - 11, last, 8, false, 0.45, 0.45, 0.45);
        }
        y = bottom - 16;
        show(x0, y - 8, "Gold: mood (5 is best)    Black: stress (5 is highest)    Days with no check-in are skipped", 8, false, 0.45, 0.45, 0.45);
        y -= 16;
    }

    // ----- output -----
    byte[] toBytes() {
        List<String> objs = new ArrayList<>();
        int pageCount = pages.size();
        // 1 catalog, 2 pages, 3 font F1, 4 font F2, then per page: page object and content stream
        objs.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) kids.append(5 + i * 2).append(" 0 R ");
        objs.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < pageCount; i++) {
            StringBuilder content = new StringBuilder(pages.get(i));
            content.append(String.format(Locale.ROOT, "BT /F1 8 Tf 0.5 0.5 0.5 rg %.1f 30 Td (%s) Tj ET\n", LEFT, esc(clean(footer))));
            String pn = "Page " + (i + 1) + " of " + pageCount;
            content.append(String.format(Locale.ROOT, "BT /F1 8 Tf 0.5 0.5 0.5 rg %.1f 30 Td (%s) Tj ET\n", RIGHT - width(pn, 8), pn));
            objs.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + (6 + i * 2) + " 0 R >>");
            byte[] body = content.toString().getBytes(StandardCharsets.ISO_8859_1);
            objs.add("<< /Length " + body.length + " >>\nstream\n" + content + "endstream");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        int[] offsets = new int[objs.size()];
        for (int i = 0; i < objs.size(); i++) {
            offsets[i] = out.size();
            write(out, (i + 1) + " 0 obj\n" + objs.get(i) + "\nendobj\n");
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) x.append(String.format("%010d 00000 n \n", off));
        x.append("trailer\n<< /Size ").append(objs.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(out, x.toString());
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream o, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        o.write(b, 0, b.length);
    }
}
