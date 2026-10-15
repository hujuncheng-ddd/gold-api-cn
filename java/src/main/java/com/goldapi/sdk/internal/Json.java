package com.goldapi.sdk.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 解析器（SDK 内部使用，避免引入第三方依赖）。
 *
 * <p>支持 RFC 8259 的对象 / 数组 / 字符串（含 unicode 转义）/ 数字 / true / false / null，
 * 解析结果映射为 {@code Map<String, Object>} / {@code List<Object>} / {@code String} /
 * {@code Long} / {@code Double} / {@code Boolean} / {@code null}。
 */
public final class Json {

    private Json() {
    }

    /** 解析任意 JSON 文本。 */
    public static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("JSON 文本为 null");
        }
        Parser p = new Parser(text);
        Object value = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw p.error("尾部存在多余字符");
        }
        return value;
    }

    /** 解析 JSON 对象，非对象时抛出异常。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("期望 JSON 对象");
        }
        return (Map<String, Object>) value;
    }

    static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return pos >= text.length();
        }

        Object parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw error("意外的输入结束");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            pos++; // 跳过 '{'
            skipWhitespace();
            if (!atEnd() && text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (atEnd() || text.charAt(pos) != '"') {
                    throw error("期望对象键字符串");
                }
                String key = parseString();
                skipWhitespace();
                if (atEnd() || text.charAt(pos) != ':') {
                    throw error("期望 ':'");
                }
                pos++;
                map.put(key, parseValue());
                skipWhitespace();
                if (atEnd()) {
                    throw error("未闭合的对象");
                }
                char c = text.charAt(pos);
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == '}') {
                    pos++;
                    return map;
                }
                throw error("期望 ',' 或 '}'");
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<Object>();
            pos++; // 跳过 '['
            skipWhitespace();
            if (!atEnd() && text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (atEnd()) {
                    throw error("未闭合的数组");
                }
                char c = text.charAt(pos);
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == ']') {
                    pos++;
                    return list;
                }
                throw error("期望 ',' 或 ']'");
            }
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            pos++; // 跳过 '"'
            while (!atEnd()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (atEnd()) {
                    break;
                }
                char esc = text.charAt(pos++);
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > text.length()) {
                            throw error("非法 \\u 转义");
                        }
                        try {
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("非法 \\u 转义");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("非法转义字符 \\" + esc);
                }
            }
            throw error("未闭合的字符串");
        }

        private Object parseNumber() {
            int start = pos;
            if (!atEnd() && text.charAt(pos) == '-') {
                pos++;
            }
            while (!atEnd() && isDigit(text.charAt(pos))) {
                pos++;
            }
            boolean isDouble = false;
            if (!atEnd() && text.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (!atEnd() && isDigit(text.charAt(pos))) {
                    pos++;
                }
            }
            if (!atEnd() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (!atEnd() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                    pos++;
                }
                while (!atEnd() && isDigit(text.charAt(pos))) {
                    pos++;
                }
            }
            String num = text.substring(start, pos);
            if (num.isEmpty() || num.equals("-")) {
                throw error("非法数字");
            }
            if (isDouble) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private void expect(String word) {
            if (!text.startsWith(word, pos)) {
                throw error("期望 " + word);
            }
            pos += word.length();
        }

        void skipWhitespace() {
            while (!atEnd()) {
                char c = text.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException("JSON 解析错误（位置 " + pos + "）: " + message);
        }
    }
}
