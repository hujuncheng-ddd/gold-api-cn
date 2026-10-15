/**
 * 演示：批量获取实时行情
 *
 * 运行前:
 *   1. npm install gold-api   (或在本目录 npm link)
 *   2. 免费注册获取 AppKey: https://www.gold-api.cn/client/register.php
 *   3. export GOLD_API_KEY=gk_xxxxxxxx  (或直接替换下方占位符)
 *
 * 运行: node examples/realtime.js
 */
import { GoldApiClient, GoldApiException } from 'gold-api';

const appkey = process.env.GOLD_API_KEY || 'gk_你的appkey';

const client = new GoldApiClient(appkey);

try {
  // 一次请求拿 4 个品种：黄金现货 / 白银现货 / 纽约金 / 沪金主力
  const quotes = await client.realtime(['1061', '1062', 'hf_GC', 'nf_AU0']);

  const ids = Object.keys(quotes);
  if (ids.length === 0) {
    console.error('未查询到行情，请检查 goldId 是否正确');
    process.exit(1);
  }

  // 每个品种返回的完整字段（11 个）：
  //   lastPrice 最新价 / changePrice 涨跌额 / changeMargin 涨跌幅
  //   yesyPrice 昨收 / openPrice 开盘 / highPrice 最高 / lowPrice 最低
  //   buyPrice 买入价 / sellPrice 卖出价 / updateTime 更新时间
  const num = (v, w = 10) => (v == null ? '-'.padStart(w) : Number(v).toFixed(2).padStart(w));
  const head = ['品种', '最新价', '涨跌额', '涨跌幅', '昨收', '开盘', '最高', '最低', '买入价', '卖出价', '更新时间'];
  console.log(head[0].padEnd(12) + head.slice(1, -1).map(h => h.padStart(10)).join(' ') + '  ' + head[10]);
  console.log('-'.repeat(122));
  for (const q of Object.values(quotes)) {
    console.log(
      q.varietyName.padEnd(12) +
      [num(q.lastPrice), num(q.changePrice, 9), String(q.changeMargin).padStart(9),
       num(q.yesyPrice), num(q.openPrice), num(q.highPrice), num(q.lowPrice),
       num(q.buyPrice), num(q.sellPrice)].join(' ') +
      '  ' + q.updateTime
    );
  }

  // 配额 / 限流信息（来自响应头）
  console.log('\n配额信息:', client.getLastRateLimit());
} catch (e) {
  if (e instanceof GoldApiException) {
    console.error(`调用失败 [${e.msgId ?? '-'}] HTTP ${e.httpCode}: ${e.message} (requestId: ${e.requestId ?? '-'})`);
    if (e.isRateLimit()) {
      console.error('提示: 已触发配额/频率限制，可降低调用频率或提升配额');
    }
    process.exit(1);
  }
  throw e;
}
