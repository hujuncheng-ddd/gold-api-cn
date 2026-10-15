package com.goldapi.sdk;

import java.util.Map;

/**
 * 品种信息（varieties 接口返回项）。
 */
public final class Variety {

    private final String goldId;
    private final String name;
    private final String market;
    private final String category;

    private Variety(String goldId, String name, String market, String category) {
        this.goldId = goldId;
        this.name = name;
        this.market = market;
        this.category = category;
    }

    /** 从接口返回的原始 Map 构造（SDK 内部使用）。 */
    static Variety fromMap(Map<String, Object> m) {
        return new Variety(
                Quote.Values.asString(m.get("goldId")),
                Quote.Values.asString(m.get("name")),
                Quote.Values.asString(m.get("market")),
                Quote.Values.asString(m.get("category")));
    }

    /** 品种 ID，如 "1061" */
    public String getGoldId() {
        return goldId;
    }

    /** 品种名称，如 "黄金现货" */
    public String getName() {
        return name;
    }

    /** 所属市场，如 "现货"、"上金所"、"外盘" */
    public String getMarket() {
        return market;
    }

    /** 分类，如 "黄金" */
    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "Variety{goldId=" + goldId + ", name=" + name
                + ", market=" + market + ", category=" + category + "}";
    }
}
