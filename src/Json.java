import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON parser (objects, arrays, strings, integers/doubles, true/false/null). */
final class Json {
    private final String s;
    private int p;

    private Json(String s) { this.s = s; }

    static Object parse(String text) {
        Json j = new Json(text);
        Object v = j.value();
        j.ws();
        if (j.p != j.s.length()) throw new IllegalArgumentException("trailing characters in JSON");
        return v;
    }

    static String quote(String x) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    private void ws() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; }

    private Object value() {
        ws();
        if (p >= s.length()) throw new IllegalArgumentException("unexpected end of JSON");
        char c = s.charAt(p);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", p)) { p += 4; return Boolean.TRUE; }
        if (s.startsWith("false", p)) { p += 5; return Boolean.FALSE; }
        if (s.startsWith("null", p)) { p += 4; return null; }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        p++;
        ws();
        if (s.charAt(p) == '}') { p++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            expect(':');
            m.put(k, value());
            ws();
            if (s.charAt(p) == ',') { p++; continue; }
            expect('}');
            return m;
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        p++;
        ws();
        if (s.charAt(p) == ']') { p++; return l; }
        while (true) {
            l.add(value());
            ws();
            if (s.charAt(p) == ',') { p++; continue; }
            expect(']');
            return l;
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(p++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = s.charAt(p++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> { sb.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; }
                default -> sb.append(e);
            }
        }
    }

    private Number number() {
        int st = p;
        boolean frac = false;
        while (p < s.length()) {
            char c = s.charAt(p);
            if (c == '.' || c == 'e' || c == 'E') frac = true;
            else if (!(c == '-' || c == '+' || (c >= '0' && c <= '9'))) break;
            p++;
        }
        if (st == p) throw new IllegalArgumentException("bad JSON value at " + p);
        String t = s.substring(st, p);
        return frac ? (Number) Double.valueOf(t) : (Number) Long.valueOf(t);
    }

    private void expect(char c) {
        if (p >= s.length() || s.charAt(p) != c) throw new IllegalArgumentException("expected '" + c + "' at " + p);
        p++;
    }
}
