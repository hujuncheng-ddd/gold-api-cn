/**
 * 演示：获取品种列表（无需 AppKey，可直接用于前端下拉框）
 *
 * 运行: node examples/varieties.js [市场名称]
 * 示例: node examples/varieties.js 上金所
 */
import { GoldApiClient } from 'gold-api';

const market = process.argv[2] || undefined;

const client = new GoldApiClient(); // varieties 接口无需认证

const items = await client.varieties(market);

if (!items.length) {
  console.error('未查询到品种');
  process.exit(1);
}

console.log('%-10s %-14s %-10s %s', 'goldId', '名称', '市场', '分类');
for (const v of items) {
  console.log(
    `${v.goldId.padEnd(10)} ${v.name.padEnd(14)} ${v.market.padEnd(10)} ${v.category}`
  );
}
console.log(`\n共 ${items.length} 个品种`);
