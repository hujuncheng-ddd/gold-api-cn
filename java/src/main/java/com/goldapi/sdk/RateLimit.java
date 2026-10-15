package com.goldapi.sdk;

/**
 * 最近一次请求的配额 / 限流信息（来自响应头，会员通道）。
 *
 * <p>调用 {@link GoldApiClient#getLastRateLimit()} 获取，可用于客户端自行控制请求节奏。
 */
public final class RateLimit {

    private final String billing;
    private final String dailyLimit;
    private final String dailyRemaining;
    private final String qpsLimit;
    private final String varietyQps;

    RateLimit() {
        this("", "", "", "", "");
    }

    RateLimit(String billing, String dailyLimit, String dailyRemaining,
              String qpsLimit, String varietyQps) {
        this.billing = billing;
        this.dailyLimit = dailyLimit;
        this.dailyRemaining = dailyRemaining;
        this.qpsLimit = qpsLimit;
        this.varietyQps = varietyQps;
    }

    /** 通道类型：member=会员通道, variety=单品通道 */
    public String getBilling() {
        return billing;
    }

    /** 日配额 */
    public String getDailyLimit() {
        return dailyLimit;
    }

    /** 今日剩余配额 */
    public String getDailyRemaining() {
        return dailyRemaining;
    }

    /** 每秒请求限制 */
    public String getQpsLimit() {
        return qpsLimit;
    }

    /** 单品通道限速 */
    public String getVarietyQps() {
        return varietyQps;
    }

    @Override
    public String toString() {
        return "RateLimit{billing=" + billing
                + ", dailyLimit=" + dailyLimit
                + ", dailyRemaining=" + dailyRemaining
                + ", qpsLimit=" + qpsLimit
                + ", varietyQps=" + varietyQps + "}";
    }
}
