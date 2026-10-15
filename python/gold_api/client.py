"""GoldAPI 数据服务平台 Python 客户端。

官网:   https://www.gold-api.cn
文档:   https://www.gold-api.cn/docs.php
注册:   https://www.gold-api.cn/client/register.php
"""

import json
import urllib.error
import urllib.parse
import urllib.request

from .exceptions import GoldApiException

DEFAULT_BASE_URL = "https://www.gold-api.cn"

#: 单次请求最多品种数（平台限制）
MAX_IDS_PER_REQUEST = 50

_RATE_LIMIT_HEADER_MAP = {
    "x-billing": "billing",
    "x-ratelimit-daily-limit": "daily_limit",
    "x-ratelimit-daily-remaining": "daily_remaining",
    "x-ratelimit-qps-limit": "qps_limit",
    "x-ratelimit-variety-qps": "variety_qps",
}


class GoldApiClient(object):
    """GoldAPI 客户端。

    用法::

        from gold_api import GoldApiClient

        client = GoldApiClient("gk_your_appkey")
        quotes = client.realtime(["1061", "hf_GC"])   # 黄金现货 + 纽约金
    """

    def __init__(self, appkey="", base_url=DEFAULT_BASE_URL, timeout=10):
        """
        :param appkey: 平台签发的 AppKey；仅调用 varieties() 等公开接口时可传空
        :param base_url: 接口根地址，默认线上地址
        :param timeout: 请求超时（秒）
        """
        self.appkey = appkey
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self._last_headers = {}

    # ------------------------------------------------------------------
    # 公开 API
    # ------------------------------------------------------------------

    def realtime(self, gold_ids):
        """实时行情（支持批量，单次最多 50 个品种）。

        :param gold_ids: 品种 ID，如 ``"1061,1062"`` 或 ``["1061", "hf_GC"]``
        :return: dict，以 goldId 为键；goldId 不存在时该键缺失
        :raises GoldApiException: 认证失败 / 配额超限 / 网络错误等
        """
        if isinstance(gold_ids, (list, tuple)):
            gold_ids = ",".join(str(g) for g in gold_ids)
        gold_ids = str(gold_ids).strip()
        if not gold_ids:
            raise ValueError("gold_ids 不能为空")
        if len(gold_ids.split(",")) > MAX_IDS_PER_REQUEST:
            raise ValueError(
                "单次最多 %d 个品种，请分批请求" % MAX_IDS_PER_REQUEST)

        result = self._request(
            "/api/v1/gold/realtime", {"goldid": gold_ids}, with_auth=True)
        return result.get("dtList") or {}

    def realtime_one(self, gold_id):
        """单品种行情快捷方法。品种不存在/已下架时返回 None。"""
        gold_id = str(gold_id)
        return self.realtime(gold_id).get(gold_id)

    def varieties(self, market=None):
        """品种列表（无需认证，可直接用于前端下拉框）。

        :param market: 按市场筛选，如 ``"上金所"``、``"外盘"``；None 为全部
        :return: list，元素形如 ``{"goldId": "1061", "name": "黄金现货", ...}``
        """
        params = {"market": market} if market else None
        result = self._request("/api/v1/gold/varieties", params, with_auth=False)
        return result.get("dtList") or []

    @property
    def last_request_id(self):
        """最近一次请求的 requestId（联系平台排查问题时请提供该值）。"""
        return self._last_headers.get("x-request-id", "")

    def last_rate_limit(self):
        """最近一次请求的配额/限流信息（来自响应头）。

        :return: dict，如 ``{"billing": "member", "daily_limit": "1000", ...}``
        """
        info = {}
        for raw, key in _RATE_LIMIT_HEADER_MAP.items():
            if raw in self._last_headers:
                info[key] = self._last_headers[raw]
        return info

    # ------------------------------------------------------------------
    # 内部实现
    # ------------------------------------------------------------------

    def _request(self, path, params, with_auth):
        url = self.base_url + path
        if params:
            url += "?" + urllib.parse.urlencode(params)

        headers = {"Accept": "application/json"}
        if with_auth:
            if not self.appkey:
                raise ValueError(
                    "appkey 不能为空，请传入平台签发的 AppKey "
                    "(https://www.gold-api.cn/client/register.php 免费注册)")
            # 走请求头认证，避免 appkey 出现在 URL / 访问日志中
            headers["X-API-KEY"] = self.appkey

        req = urllib.request.Request(url, headers=headers)
        try:
            resp = urllib.request.urlopen(req, timeout=self.timeout)
        except urllib.error.HTTPError as e:
            # 平台错误走非 2xx 状态码
            self._last_headers = {k.lower(): v for k, v in e.headers.items()}
            body = e.read().decode("utf-8", "replace")
            try:
                data = json.loads(body)
            except ValueError:
                raise GoldApiException(
                    "响应不是合法 JSON: %s" % body[:200], http_code=e.code)
            if data.get("success") == "0":
                raise GoldApiException.from_response(data, http_code=e.code)
            raise GoldApiException(body[:200], http_code=e.code)
        except urllib.error.URLError as e:
            raise GoldApiException("网络请求失败: %s" % (e.reason,))

        with resp:
            self._last_headers = {k.lower(): v for k, v in resp.getheaders()}
            body = resp.read().decode("utf-8", "replace")

        try:
            data = json.loads(body)
        except ValueError:
            raise GoldApiException(
                "响应不是合法 JSON: %s" % body[:200])

        if data.get("success") != "1":
            raise GoldApiException.from_response(data)

        return data.get("result") or {}
