package com.goldapi.sdk;

import java.util.Map;

/**
 * 实时行情（每个品种 13 个字段）。
 *
 * <p>字段与接口返回一一对应，价格均为 double，涨跌幅 / 更新时间为字符串。
 * 实例不可变，可安全跨线程共享。
 */
public final class Quote {

    private final String goldId;
    private final String variety;
    private final String varietyName;
    private final double lastPrice;
    private final double buyPrice;
    private final double sellPrice;
    private final double highPrice;
    private final double lowPrice;
    private final double openPrice;
    private final double yesyPrice;
    private final double changePrice;
    private final String changeMargin;
    private final String updateTime;

    private Quote(String goldId, String variety, String varietyName,
                  double lastPrice, double buyPrice, double sellPrice,
                  double highPrice, double lowPrice, double openPrice,
                  double yesyPrice, double changePrice,
                  String changeMargin, String updateTime) {
        this.goldId = goldId;
        this.variety = variety;
        this.varietyName = varietyName;
        this.lastPrice = lastPrice;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.openPrice = openPrice;
        this.yesyPrice = yesyPrice;
        this.changePrice = changePrice;
        this.changeMargin = changeMargin;
        this.updateTime = updateTime;
    }

    /** 从接口返回的原始 Map 构造（SDK 内部使用）。 */
    static Quote fromMap(Map<String, Object> m) {
        return new Quote(
                Values.asString(m.get("goldId")),
                Values.asString(m.get("variety")),
                Values.asString(m.get("varietyName")),
                Values.asDouble(m.get("lastPrice")),
                Values.asDouble(m.get("buyPrice")),
                Values.asDouble(m.get("sellPrice")),
                Values.asDouble(m.get("highPrice")),
                Values.asDouble(m.get("lowPrice")),
                Values.asDouble(m.get("openPrice")),
                Values.asDouble(m.get("yesyPrice")),
                Values.asDouble(m.get("changePrice")),
                Values.asString(m.get("changeMargin")),
                Values.asString(m.get("updateTime")));
    }

    /** 品种 ID，如 "hf_GC" */
    public String getGoldId() {
        return goldId;
    }

    /** 品种英文代码，如 "COMEX黄金" */
    public String getVariety() {
        return variety;
    }

    /** 品种中文名称，如 "纽约金" */
    public String getVarietyName() {
        return varietyName;
    }

    /** 最新价 */
    public double getLastPrice() {
        return lastPrice;
    }

    /** 买入价 */
    public double getBuyPrice() {
        return buyPrice;
    }

    /** 卖出价 */
    public double getSellPrice() {
        return sellPrice;
    }

    /** 当日最高价 */
    public double getHighPrice() {
        return highPrice;
    }

    /** 当日最低价 */
    public double getLowPrice() {
        return lowPrice;
    }

    /** 开盘价（现货类品种可能为 0） */
    public double getOpenPrice() {
        return openPrice;
    }

    /** 昨收价（涨跌基准价） */
    public double getYesyPrice() {
        return yesyPrice;
    }

    /** 涨跌额 = 最新价 - 昨收价 */
    public double getChangePrice() {
        return changePrice;
    }

    /** 涨跌幅，如 "-3.33%" */
    public String getChangeMargin() {
        return changeMargin;
    }

    /** 行情更新时间（北京时间） */
    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public String toString() {
        return "Quote{goldId=" + goldId
                + ", varietyName=" + varietyName
                + ", lastPrice=" + lastPrice
                + ", changePrice=" + changePrice
                + ", changeMargin=" + changeMargin
                + ", updateTime=" + updateTime + "}";
    }

    /** 内部类型转换工具。 */
    static final class Values {
        private Values() {
        }

        static String asString(Object v) {
            return v == null ? "" : String.valueOf(v);
        }

        static double asDouble(Object v) {
            if (v == null) {
                return 0D;
            }
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.isEmpty()) {
                    return 0D;
                }
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException ignored) {
                    return 0D;
                }
            }
            return 0D;
        }
    }
}
