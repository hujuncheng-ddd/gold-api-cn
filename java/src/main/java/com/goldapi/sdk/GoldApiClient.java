package com.goldapi.sdk;

import com.goldapi.sdk.internal.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GoldAPI 数据服务平台 Java 客户端。
 *
 * <p>官网: <a href="https://www.gold-api.cn">https://www.gold-api.cn</a><br>
 * 文档: <a href="https://www.gold-api.cn/docs.php">https://www.gold-api.cn/docs.php</a><br>
 * 注册: <a href="https://www.gold-api.cn/client/register.php">https://www.gold-api.cn/client/register.php</a>
 *
 * <p>用法（API 与官方 PHP / Python / Node.js / Go SDK 完全对齐）:
 *
 * <pre>{@code
 * GoldApiClient client = new GoldApiClient("gk_your_appkey");
 * Map<String, Quote> quotes = client.realtime("1051", "1061", "hf_GC"); // 黄金T+D + 黄金现货 + 纽约金
 * }</pre>
 *
 * <p>客户端实例线程安全，可全局复用；appkey 通过 {@code X-API-KEY} 请求头传递，
 * 不会出现在 URL / 访问日志中。
 */
public final class GoldApiClient {

    /** 接口根地址 */
    public static final String DEFAULT_BASE_URL = "https://www.gold-api.cn";

    /** 单次请求最多品种数（平台限制） */
    public static final int MAX_IDS_PER_REQUEST = 50;

    private final String appkey;
    private final String baseUrl;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    private volatile RateLimit lastRateLimit = new RateLimit();
    private volatile String lastRequestId = "";

    /**
     * @param appkey 平台签发的 AppKey（https://www.gold-api.cn/client/register.php 免费注册获取）；
     *               仅调用 {@link #varieties()} 等公开接口时可传空字符串
     */
    public GoldApiClient(String appkey) {
        this(appkey, DEFAULT_BASE_URL, 10_000, 10_000);
    }

    /**
     * @param appkey 平台签发的 AppKey
     * @param baseUrl 接口根地址，默认 {@link #DEFAULT_BASE_URL}
     */
    public GoldApiClient(String appkey, String baseUrl) {
        this(appkey, baseUrl, 10_000, 10_000);
    }

    /**
     * @param appkey               平台签发的 AppKey
     * @param baseUrl              接口根地址
     * @param connectTimeoutMillis 连接超时（毫秒）
     * @param readTimeoutMillis    读超时（毫秒）
     */
    public GoldApiClient(String appkey, String baseUrl, int connectTimeoutMillis, int readTimeoutMillis) {
        this.appkey = appkey == null ? "" : appkey.trim();
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = (base.isEmpty() ? DEFAULT_BASE_URL : base).replaceAll("/+$", "");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    // ------------------------------------------------------------------
    // 公开 API
    // ------------------------------------------------------------------

    /**
     * 批量实时行情，单次最多 {@value #MAX_IDS_PER_REQUEST} 个品种。
     *
     * @param goldIds 品种 ID，如 {@code "1051"}、{@code "hf_GC"}，可变参数或集合传入
     * @return 以 goldId 为键的行情 Map（保持接口返回顺序）；goldId 不存在或已下架时该键缺失
     * @throws GoldApiException 认证失败 / 配额超限 / 网络错误等
     */
    public Map<String, Quote> realtime(String... goldIds) {
        if (goldIds == null) {
            throw new IllegalArgumentException("goldIds 不能为空");
        }
        return realtime(Arrays.asList(goldIds));
    }

    /**
     * 批量实时行情（集合版本），单次最多 {@value #MAX_IDS_PER_REQUEST} 个品种。
     */
    public Map<String, Quote> realtime(List<String> goldIds) {
        List<String> ids = normalizeIds(goldIds);

        Map<String, Object> result = doGet("/api/v1/gold/realtime",
                encodeQuery("goldid", join(ids)), true);

        Map<String, Quote> quotes = new LinkedHashMap<String, Quote>();
        Object dtList = result.get("dtList");
        if (dtList instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) dtList).entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) entry.getValue();
                    quotes.put(entry.getKey(), Quote.fromMap(m));
                }
            }
        }
        return quotes;
    }

    /**
     * 单品种行情快捷方法。
     *
     * @param goldId 品种 ID
     * @return 行情对象；goldId 不存在或已下架时返回 {@code null}
     * @throws GoldApiException 认证失败 / 配额超限 / 网络错误等
     */
    public Quote realtimeOne(String goldId) {
        Map<String, Quote> quotes = realtime(goldId);
        return quotes.get(goldId);
    }

    /**
     * 品种列表（无需认证，可直接用于前端下拉框）。
     *
     * @return 全部品种
     * @throws GoldApiException 网络错误等
     */
    public List<Variety> varieties() {
        return varieties(null);
    }

    /**
     * 品种列表（无需认证，可直接用于前端下拉框）。
     *
     * @param market 按市场筛选，如 {@code "上金所"}、{@code "外盘"}、{@code "现货"}；null 为全部
     * @return 品种列表
     * @throws GoldApiException 网络错误等
     */
    public List<Variety> varieties(String market) {
        String query = market == null || market.trim().isEmpty()
                ? "" : encodeQuery("market", market.trim());

        Map<String, Object> result = doGet("/api/v1/gold/varieties", query, false);

        List<Variety> varieties = new ArrayList<Variety>();
        Object dtList = result.get("dtList");
        if (dtList instanceof List) {
            for (Object item : (List<?>) dtList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) item;
                    varieties.add(Variety.fromMap(m));
                }
            }
        }
        return varieties;
    }

    /**
     * 最近一次请求的配额 / 限流信息（来自响应头，会员通道），可用于客户端自行控制节奏。
     */
    public RateLimit getLastRateLimit() {
        return lastRateLimit;
    }

    /**
     * 最近一次请求的 requestId（联系平台排查问题时请提供该值）。
     */
    public String getLastRequestId() {
        return lastRequestId;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private static List<String> normalizeIds(List<String> goldIds) {
        if (goldIds == null || goldIds.isEmpty()) {
            throw new IllegalArgumentException("goldIds 不能为空");
        }
        List<String> ids = new ArrayList<String>();
        for (String id : goldIds) {
            if (id == null) {
                continue;
            }
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("goldIds 不能为空");
        }
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            throw new IllegalArgumentException(
                    "单次最多 " + MAX_IDS_PER_REQUEST + " 个品种，当前 " + ids.size() + " 个，请分批请求");
        }
        return ids;
    }

    private static String join(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private static String encodeQuery(String key, String value) {
        try {
            return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 必然可用
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doGet(String path, String query, boolean withAuth) {
        String url = baseUrl + path + (query.isEmpty() ? "" : "?" + query);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMillis);
            conn.setReadTimeout(readTimeoutMillis);
            conn.setRequestProperty("Accept", "application/json");
            if (withAuth) {
                if (appkey.isEmpty()) {
                    throw new IllegalStateException(
                            "appkey 不能为空，请传入平台签发的 AppKey"
                                    + "（https://www.gold-api.cn/client/register.php 免费注册获取）");
                }
                // 走请求头认证，避免 appkey 出现在 URL / 访问日志中
                conn.setRequestProperty("X-API-KEY", appkey);
            }

            int status = conn.getResponseCode();
            captureHeaders(conn);

            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = readAll(stream);

            Map<String, Object> data;
            try {
                data = Json.parseObject(body);
            } catch (RuntimeException e) {
                throw new GoldApiException(
                        "响应不是合法 JSON: " + abbreviate(body), null, null, status);
            }

            if (!"1".equals(Quote.Values.asString(data.get("success")))) {
                throw GoldApiException.fromResponse(data, status);
            }

            Object result = data.get("result");
            return result instanceof Map ? (Map<String, Object>) result : Collections.<String, Object>emptyMap();
        } catch (IOException e) {
            throw new GoldApiException("网络请求失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void captureHeaders(HttpURLConnection conn) {
        lastRequestId = orEmpty(conn.getHeaderField("X-Request-Id"));
        lastRateLimit = new RateLimit(
                orEmpty(conn.getHeaderField("X-Billing")),
                orEmpty(conn.getHeaderField("X-RateLimit-Daily-Limit")),
                orEmpty(conn.getHeaderField("X-RateLimit-Daily-Remaining")),
                orEmpty(conn.getHeaderField("X-RateLimit-QPS-Limit")),
                orEmpty(conn.getHeaderField("X-RateLimit-Variety-QPS")));
    }

    private static String orEmpty(String v) {
        return v == null ? "" : v;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            stream.close();
        }
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200);
    }
}
