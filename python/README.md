# gold-api (Python)

> GoldAPI (gold-api.cn) 贵金属行情 Python SDK —— 一次请求，批量拿回黄金 / 白银 / 铂金 / 钯金等 **70+ 品种**行情

## 安装

```bash
pip install gold-api
```

零第三方依赖，Python 3.7+。

## 快速开始

```python
from gold_api import GoldApiClient, GoldApiException

client = GoldApiClient("gk_your_appkey")   # https://www.gold-api.cn/client/register.php 免费注册获取

try:
    quotes = client.realtime(["1051", "1061", "hf_GC"])  # 黄金T+D + 黄金现货 + 纽约金
    for q in quotes.values():
        print(q["varietyName"], q["lastPrice"], q["changeMargin"])

    print(client.last_rate_limit())        # {'billing': 'member', 'daily_limit': '1000', ...}
except GoldApiException as e:
    print(e.msg_id, e.http_code, e, e.request_id)
    if e.is_rate_limit():
        ...  # 退避重试或提醒升级配额
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
