package goldapi

import "fmt"

// Error 平台业务错误（认证失败 / 配额超限等）与 SDK 层网络、解析错误。
// 业务错误携带平台返回的 MsgID / RequestID，便于排查。
type Error struct {
	MsgID     string // 平台错误码，如 DAILY_LIMIT_EXCEEDED（网络/解析类错误时为空或 HTTP_xxx）
	Msg       string // 错误描述
	HTTPCode  int    // HTTP 状态码（网络失败时为 0）
	RequestID string // 平台 requestId
}

func (e *Error) Error() string {
	if e.MsgID != "" {
		return fmt.Sprintf("goldapi: [%s] %s (requestId: %s)", e.MsgID, e.Msg, e.RequestID)
	}
	return "goldapi: " + e.Msg
}

// IsRateLimit 是否为配额 / 频率限制类错误（可用于退避重试判断）
func (e *Error) IsRateLimit() bool {
	switch e.MsgID {
	case "DAILY_LIMIT_EXCEEDED", "MONTHLY_LIMIT_EXCEEDED",
		"QPS_LIMIT_EXCEEDED", "VARIETY_QPS_EXCEEDED",
		"1009", "1010", "1011", "1015":
		return true
	}
	return false
}
