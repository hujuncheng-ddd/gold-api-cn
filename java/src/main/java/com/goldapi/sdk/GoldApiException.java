package com.goldapi.sdk;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GoldAPI 调用异常（认证失败 / 配额超限 / 网络错误等）。
 *
 * <p>业务错误携带平台返回的 msgId / requestId，便于联系平台排查。
 */
public class GoldApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 配额 / 频率限制类错误码（可退避重试或提醒升级配额） */
    private static final Set<String> RATE_LIMIT_MSG_IDS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "DAILY_LIMIT_EXCEEDED",
                    "MONTHLY_LIMIT_EXCEEDED",
                    "QPS_LIMIT_EXCEEDED",
                    "VARIETY_QPS_EXCEEDED",
                    "1009", "1010", "1011", "1015")));

    private final String msgId;
    private final String requestId;
    private final int httpCode;

    public GoldApiException(String message) {
        this(message, null, null, 0);
    }

    public GoldApiException(String message, String msgId, String requestId, int httpCode) {
        super(message);
        this.msgId = msgId;
        this.requestId = requestId;
        this.httpCode = httpCode;
    }

    /** 从平台失败响应构造异常（SDK 内部使用）。 */
    static GoldApiException fromResponse(Map<String, Object> data, int httpCode) {
        String msg = data.get("msg") == null ? "未知错误" : String.valueOf(data.get("msg"));
        String msgId = data.get("msgId") == null ? ("HTTP_" + httpCode) : String.valueOf(data.get("msgId"));
        String requestId = data.get("requestId") == null ? "" : String.valueOf(data.get("requestId"));
        return new GoldApiException(format(msgId, msg, requestId), msgId, requestId, httpCode);
    }

    private static String format(String msgId, String msg, String requestId) {
        if (msgId != null && !msgId.isEmpty()) {
            return "[" + msgId + "] " + msg + " (requestId: " + requestId + ")";
        }
        return msg;
    }

    /** 平台错误码，如 DAILY_LIMIT_EXCEEDED；网络 / 解析类错误时为 null 或 HTTP_xxx */
    public String getMsgId() {
        return msgId;
    }

    /** 平台 requestId，排查问题时提供给平台方；网络失败时为 null */
    public String getRequestId() {
        return requestId;
    }

    /** HTTP 状态码，网络失败时为 0 */
    public int getHttpCode() {
        return httpCode;
    }

    /** 是否为配额 / 频率限制类错误（可用于退避重试判断）。 */
    public boolean isRateLimit() {
        return msgId != null && RATE_LIMIT_MSG_IDS.contains(msgId);
    }
}
