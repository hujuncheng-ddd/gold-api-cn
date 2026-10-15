package com.goldapi.examples;

import com.goldapi.sdk.GoldApiClient;
import com.goldapi.sdk.GoldApiException;
import com.goldapi.sdk.Quote;

import java.util.Map;

/**
 * 实时行情示例（完整 11 字段输出）。
 *
 * 运行前设置环境变量：
 *
 * <pre>export GOLD_API_KEY=gk_your_appkey</pre>
 */
public final class RealtimeExample {

    public static void main(String[] args) {
        String appkey = System.getenv("GOLD_API_KEY");
        if (appkey == null || appkey.trim().isEmpty()) {
            System.err.println("请先设置环境变量 GOLD_API_KEY"
                    + "（https://www.gold-api.cn/client/register.php 免费注册获取）");
            System.exit(1);
        }

        GoldApiClient client = new GoldApiClient(appkey);
        try {
            // 一次请求拿 4 个品种：黄金T+D / 黄金现货 / 白银现货 / 纽约金
            Map<String, Quote> quotes = client.realtime("1051", "1061", "1062", "hf_GC");

            if (quotes.isEmpty()) {
                System.out.println("未查询到行情，请检查 goldId 是否正确");
                return;
            }

            System.out.printf("%-10s %12s %10s %10s %10s %10s %10s %10s %10s %10s  %s%n",
                    "品种", "最新价", "涨跌额", "涨跌幅", "昨收", "开盘", "最高", "最低", "买入", "卖出", "更新时间");
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < 130; i++) {
                line.append('-');
            }
            System.out.println(line);
            for (Quote q : quotes.values()) {
                System.out.printf("%-10s %12.2f %10.2f %10s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f  %s%n",
                        q.getVarietyName(), q.getLastPrice(), q.getChangePrice(), q.getChangeMargin(),
                        q.getYesyPrice(), q.getOpenPrice(), q.getHighPrice(), q.getLowPrice(),
                        q.getBuyPrice(), q.getSellPrice(), q.getUpdateTime());
            }

            System.out.println();
            System.out.println("配额信息: " + client.getLastRateLimit());
            System.out.println("requestId: " + client.getLastRequestId());
        } catch (GoldApiException e) {
            System.err.println("调用失败: " + e.getMessage());
            if (e.isRateLimit()) {
                System.err.println("（配额/频率限制类错误，请稍后重试或升级套餐）");
            }
            System.exit(1);
        }
    }

    private RealtimeExample() {
    }
}
