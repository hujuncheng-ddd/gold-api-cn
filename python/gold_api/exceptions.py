"""GoldAPI 贵金属行情 SDK 异常。"""

RATE_LIMIT_MSG_IDS = frozenset({
    "DAILY_LIMIT_EXCEEDED",
    "MONTHLY_LIMIT_EXCEEDED",
    "QPS_LIMIT_EXCEEDED",
    "VARIETY_QPS_EXCEEDED",
})


class GoldApiException(Exception):
    """GoldAPI 调用异常。

    属性:
        msg_id:      平台错误码，如 ``DAILY_LIMIT_EXCEEDED``；网络失败时为 None
        request_id:  平台 requestId，排查问题时提供给平台方；网络失败时为 None
        http_code:   HTTP 状态码，网络失败时为 0
    """

    def __init__(self, message, msg_id=None, request_id=None, http_code=0):
        super().__init__(message)
        self.msg_id = msg_id
        self.request_id = request_id
        self.http_code = http_code

    @classmethod
    def from_response(cls, data, http_code=0):
        """从平台失败响应构造异常。"""
        return cls(
            data.get("msg") or "未知错误",
            msg_id=data.get("msgId"),
            request_id=data.get("requestId"),
            http_code=http_code,
        )

    def is_rate_limit(self):
        """是否为配额/频率类错误（可退避重试或提醒升级配额）。"""
        return self.msg_id in RATE_LIMIT_MSG_IDS

    def __repr__(self):
        return "GoldApiException(msg_id=%r, http_code=%r, message=%r)" % (
            self.msg_id, self.http_code, self.args[0] if self.args else "")
