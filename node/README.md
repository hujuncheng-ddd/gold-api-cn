# gold-api (Node.js)

> GoldAPI (gold-api.cn) 贵金属行情 Node.js SDK —— 一次请求，批量拿回黄金 / 白银 / 铂金 / 钯金等 **70+ 品种**行情

## 安装

```bash
npm install gold-api
```

零依赖（Node 18+ 内置 fetch），ESM。

## 快速开始

```js
import { GoldApiClient, GoldApiException } from 'gold-api';

const client = new GoldApiClient('gk_your_appkey');  // https://www.gold-api.cn/client/register.php 免费注册获取

try {
  const quotes = await client.realtime(['1061', 'hf_GC', 'nf_AU0']); // 黄金现货 + 纽约金 + 沪金主力
  for (const q of Object.values(quotes)) {
    console.log(q.varietyName, q.lastPrice, q.changeMargin);
  }

  console.log(client.getLastRateLimit());  // { billing: 'member', daily_limit: '1000', ... }
} catch (e) {
  if (e instanceof GoldApiException) {
    console.error(e.msgId, e.httpCode, e.message, e.requestId);
    if (e.isRateLimit()) { /* 退避重试或提醒升级配额 */ }
  }
}
```

更多用法（单品种、品种列表、配额感知）见[仓库根目录 README](../README.md)。

## 链接

| 资源 | 地址 |
|---|---|
| 官网 | https://www.gold-api.cn |
| 接口文档 | https://www.gold-api.cn/docs.php |
| 免费注册 | https://www.gold-api.cn/client/register.php |

## License

MIT
