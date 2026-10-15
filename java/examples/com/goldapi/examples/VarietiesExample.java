package com.goldapi.examples;

import com.goldapi.sdk.GoldApiClient;
import com.goldapi.sdk.GoldApiException;
import com.goldapi.sdk.Variety;

import java.util.List;

/**
 * 品种列表示例（无需 AppKey）。
 *
 * 可选参数：市场名，如 "上金所"、"外盘"、"现货"；缺省为全部。
 */
public final class VarietiesExample {

    public static void main(String[] args) {
        // varieties 接口无需认证，appkey 传空即可
        GoldApiClient client = new GoldApiClient("");
        try {
            String market = args.length > 0 ? args[0] : null;
            List<Variety> varieties = client.varieties(market);

            System.out.println("共 " + varieties.size() + " 个品种"
                    + (market == null ? "" : "（市场: " + market + "）"));
            for (Variety v : varieties) {
                System.out.printf("%-10s %-12s %-8s %s%n",
                        v.getGoldId(), v.getName(), v.getMarket(), v.getCategory());
            }
        } catch (GoldApiException e) {
            System.err.println("调用失败: " + e.getMessage());
            System.exit(1);
        }
    }

    private VarietiesExample() {
    }
}
