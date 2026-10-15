// Package goldapi 是 GoldAPI 数据服务平台（https://www.gold-api.cn）的官方 Go SDK。
//
// 用法:
//
//	client := goldapi.NewClient("gk_your_appkey")          // https://www.gold-api.cn/client/register.php 免费注册获取
//	quotes, err := client.Realtime("1051", "1061", "hf_GC") // 黄金T+D + 黄金现货 + 纽约金
package goldapi

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
)

const (
	// DefaultBaseURL 接口根地址
	DefaultBaseURL = "https://www.gold-api.cn"

	// MaxIDsPerRequest 单次请求最多品种数（平台限制）
	MaxIDsPerRequest = 50
)

// Quote 实时行情，每个品种 11 个字段
type Quote struct {
	GoldID       string  `json:"goldId"`       // 品种 ID
	Variety      string  `json:"variety"`      // 品种英文代码
	VarietyName  string  `json:"varietyName"`  // 品种中文名称
	LastPrice    float64 `json:"lastPrice"`    // 最新价
	BuyPrice     float64 `json:"buyPrice"`     // 买入价
	SellPrice    float64 `json:"sellPrice"`    // 卖出价
	HighPrice    float64 `json:"highPrice"`    // 当日最高价
	LowPrice     float64 `json:"lowPrice"`     // 当日最低价
	OpenPrice    float64 `json:"openPrice"`    // 开盘价（现货类品种可能为 0）
	YesyPrice    float64 `json:"yesyPrice"`    // 昨收价（涨跌基准价）
	ChangePrice  float64 `json:"changePrice"`  // 涨跌额 = 最新价 - 昨收价
	ChangeMargin string  `json:"changeMargin"` // 涨跌幅，如 "-3.33%"
	UpdateTime   string  `json:"updateTime"`   // 行情更新时间（北京时间）
}

// Variety 品种信息
type Variety struct {
	GoldID   string `json:"goldId"`
	Name     string `json:"name"`
	Market   string `json:"market"`
	Category string `json:"category"`
}

// RateLimit 最近一次请求的配额 / 限流信息（来自响应头，会员通道）
type RateLimit struct {
	Billing        string `json:"billing,omitempty"`         // member=会员通道, variety=单品通道
	DailyLimit     string `json:"daily_limit,omitempty"`     // 日配额
	DailyRemaining string `json:"daily_remaining,omitempty"` // 今日剩余
	QPSLimit       string `json:"qps_limit,omitempty"`       // 每秒限制
	VarietyQPS     string `json:"variety_qps,omitempty"`     // 单品通道限速
}

// Client GoldAPI 客户端，可并发复用（并发安全）
type Client struct {
	appkey     string
	baseURL    string
	httpClient *http.Client

	mu            sync.Mutex
	lastRateLimit RateLimit
	lastRequestID string
}

// NewClient 创建客户端。
// appkey 由平台签发（https://www.gold-api.cn/client/register.php 免费注册获取），
// 仅调用 Varieties 等公开接口时可传空字符串。
func NewClient(appkey string) *Client {
	return &Client{
		appkey:     appkey,
		baseURL:    DefaultBaseURL,
		httpClient: &http.Client{Timeout: 5 * time.Second},
	}
}

// SetBaseURL 自定义接口根地址（默认 https://www.gold-api.cn）
func (c *Client) SetBaseURL(u string) *Client {
	c.baseURL = strings.TrimRight(u, "/")
	return c
}

// SetHTTPClient 自定义 *http.Client（用于代理、超时精细控制）
func (c *Client) SetHTTPClient(hc *http.Client) *Client {
	c.httpClient = hc
	return c
}

// Realtime 批量实时行情，单次最多 50 个品种。
// 返回以 goldId 为键的行情 map；goldId 不存在或已下架时该键缺失。
//
//	client.Realtime("1051", "hf_GC")          // 单个或多个参数
//	client.Realtime(ids...)                   // 已有 slice 时展开传入
func (c *Client) Realtime(goldIDs ...string) (map[string]*Quote, error) {
	if len(goldIDs) == 0 {
		return nil, fmt.Errorf("goldapi: goldIDs 不能为空")
	}
	if len(goldIDs) > MaxIDsPerRequest {
		return nil, fmt.Errorf("goldapi: 单次最多 %d 个品种，当前 %d 个，请分批请求",
			MaxIDsPerRequest, len(goldIDs))
	}

	var result struct {
		DtList map[string]*Quote `json:"dtList"`
	}
	if err := c.do("/api/v1/gold/realtime",
		url.Values{"goldid": {strings.Join(goldIDs, ",")}}, true, &result); err != nil {
		return nil, err
	}
	return result.DtList, nil
}

// RealtimeOne 单品种行情快捷方法。goldId 不存在或已下架时返回 (nil, nil)。
func (c *Client) RealtimeOne(goldID string) (*Quote, error) {
	list, err := c.Realtime(goldID)
	if err != nil {
		return nil, err
	}
	return list[goldID], nil
}

// Varieties 品种列表（无需认证），market 为空时返回全部，如 "上金所"、"外盘"、"现货"。
func (c *Client) Varieties(market string) ([]*Variety, error) {
	query := url.Values{}
	if market != "" {
		query.Set("market", market)
	}

	var result struct {
		DtList []*Variety `json:"dtList"`
	}
	if err := c.do("/api/v1/gold/varieties", query, false, &result); err != nil {
		return nil, err
	}
	return result.DtList, nil
}

// LastRateLimit 最近一次请求的配额 / 限流信息，可用于客户端自行控制节奏
func (c *Client) LastRateLimit() RateLimit {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.lastRateLimit
}

// LastRequestID 最近一次请求的 requestId（联系平台排查问题时请提供该值）
func (c *Client) LastRequestID() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.lastRequestID
}

/* =====================================================================
 *  内部实现
 * ===================================================================== */

type apiResponse struct {
	Success   string          `json:"success"`
	MsgID     string          `json:"msgId"`
	Msg       string          `json:"msg"`
	RequestID string          `json:"requestId"`
	Result    json.RawMessage `json:"result"`
}

func (c *Client) do(path string, query url.Values, withAuth bool, out interface{}) error {
	u := c.baseURL + path
	if len(query) > 0 {
		u += "?" + query.Encode()
	}

	req, err := http.NewRequest(http.MethodGet, u, nil)
	if err != nil {
		return &Error{Msg: "构造请求失败: " + err.Error()}
	}
	req.Header.Set("Accept", "application/json")
	if withAuth {
		if c.appkey == "" {
			return fmt.Errorf("goldapi: appkey 不能为空，请传入平台签发的 AppKey")
		}
		// 走请求头认证，避免 appkey 出现在 URL / 访问日志中
		req.Header.Set("X-API-KEY", c.appkey)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return &Error{Msg: "网络请求失败: " + err.Error()}
	}
	defer resp.Body.Close()

	c.captureHeaders(resp)

	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		return &Error{Msg: "读取响应失败: " + err.Error(), HTTPCode: resp.StatusCode}
	}

	var envelope apiResponse
	if err := json.Unmarshal(raw, &envelope); err != nil {
		return &Error{
			Msg:      fmt.Sprintf("响应不是合法 JSON: %.200s", string(raw)),
			HTTPCode: resp.StatusCode,
		}
	}
	if envelope.Success != "1" {
		e := &Error{
			MsgID:     envelope.MsgID,
			Msg:       envelope.Msg,
			HTTPCode:  resp.StatusCode,
			RequestID: envelope.RequestID,
		}
		if e.MsgID == "" {
			e.MsgID = fmt.Sprintf("HTTP_%d", resp.StatusCode)
		}
		if e.Msg == "" {
			e.Msg = http.StatusText(resp.StatusCode)
		}
		return e
	}

	if out == nil || len(envelope.Result) == 0 {
		return nil
	}
	if err := json.Unmarshal(envelope.Result, out); err != nil {
		return &Error{Msg: "解析 result 失败: " + err.Error(), HTTPCode: resp.StatusCode}
	}
	return nil
}

func (c *Client) captureHeaders(resp *http.Response) {
	rl := RateLimit{
		Billing:        resp.Header.Get("X-Billing"),
		DailyLimit:     resp.Header.Get("X-RateLimit-Daily-Limit"),
		DailyRemaining: resp.Header.Get("X-RateLimit-Daily-Remaining"),
		QPSLimit:       resp.Header.Get("X-RateLimit-QPS-Limit"),
		VarietyQPS:     resp.Header.Get("X-RateLimit-Variety-QPS"),
	}

	c.mu.Lock()
	defer c.mu.Unlock()
	c.lastRateLimit = rl
	c.lastRequestID = resp.Header.Get("X-Request-Id")
}
