/**
 * gold-api —— GoldAPI (gold-api.cn) 贵金属行情 Node.js SDK
 *
 * 官网:   https://www.gold-api.cn
 * 文档:   https://www.gold-api.cn/docs.php
 * 注册:   https://www.gold-api.cn/client/register.php
 *
 * 零依赖（使用 Node 18+ 内置 fetch），ESM 导出。
 */

/** 单次请求最多品种数（平台限制） */
export const MAX_IDS_PER_REQUEST = 50;

export const DEFAULT_BASE_URL = 'https://www.gold-api.cn';

const RATE_LIMIT_MSG_IDS = new Set([
  'DAILY_LIMIT_EXCEEDED',
  'MONTHLY_LIMIT_EXCEEDED',
  'QPS_LIMIT_EXCEEDED',
  'VARIETY_QPS_EXCEEDED',
]);

const RATE_LIMIT_HEADER_MAP = {
  'x-billing': 'billing',
  'x-ratelimit-daily-limit': 'daily_limit',
  'x-ratelimit-daily-remaining': 'daily_remaining',
  'x-ratelimit-qps-limit': 'qps_limit',
  'x-ratelimit-variety-qps': 'variety_qps',
};

/** GoldAPI 调用异常 */
export class GoldApiException extends Error {
  /**
   * @param {string} message
   * @param {{msgId?: string|null, requestId?: string|null, httpCode?: number}} [options]
   */
  constructor(message, options = {}) {
    super(message);
    this.name = 'GoldApiException';
    /** 平台错误码，如 DAILY_LIMIT_EXCEEDED；网络失败时为 null */
    this.msgId = options.msgId ?? null;
    /** 平台 requestId，排查问题时提供给平台方 */
    this.requestId = options.requestId ?? null;
    /** HTTP 状态码，网络失败时为 0 */
    this.httpCode = options.httpCode ?? 0;
  }

  /** 是否为配额/频率类错误（可退避重试或提醒升级配额） */
  isRateLimit() {
    return RATE_LIMIT_MSG_IDS.has(this.msgId);
  }
}

/** GoldAPI 客户端 */
export class GoldApiClient {
  /**
   * @param {string} appkey 平台签发的 AppKey；仅调用 varieties() 等公开接口时可传空
   * @param {{baseUrl?: string, timeout?: number}} [options] timeout 单位毫秒，默认 5000
   */
  constructor(appkey = '', { baseUrl = DEFAULT_BASE_URL, timeout = 5000 } = {}) {
    this.appkey = appkey;
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.timeout = timeout;
    /** @type {Headers|null} 最近一次响应头 */
    this._lastHeaders = null;
  }

  /**
   * 实时行情（支持批量，单次最多 50 个品种）
   *
   * @param {string|string[]} goldIds 如 '1061,1062' 或 ['1061', 'hf_GC']
   * @returns {Promise<Object<string, Object>>} 以 goldId 为键的行情对象
   * @throws {GoldApiException}
   */
  async realtime(goldIds) {
    if (Array.isArray(goldIds)) goldIds = goldIds.join(',');
    goldIds = String(goldIds).trim();
    if (!goldIds) throw new TypeError('goldIds 不能为空');
    if (goldIds.split(',').length > MAX_IDS_PER_REQUEST) {
      throw new RangeError(`单次最多 ${MAX_IDS_PER_REQUEST} 个品种，请分批请求`);
    }
    const result = await this.#request('/api/v1/gold/realtime', { goldid: goldIds }, true);
    return result.dtList || {};
  }

  /**
   * 单品种行情快捷方法，品种不存在/已下架时返回 null
   *
   * @param {string} goldId
   * @returns {Promise<Object|null>}
   */
  async realtimeOne(goldId) {
    const list = await this.realtime(String(goldId));
    return list[String(goldId)] ?? null;
  }

  /**
   * 品种列表（无需认证，可直接用于前端下拉框）
   *
   * @param {string} [market] 按市场筛选，如 '上金所'、'外盘'；缺省为全部
   * @returns {Promise<Array<Object>>}
   */
  async varieties(market) {
    const query = market ? { market } : {};
    const result = await this.#request('/api/v1/gold/varieties', query, false);
    return result.dtList || [];
  }

  /** 最近一次请求的 requestId（联系平台排查问题时请提供该值） */
  get lastRequestId() {
    return this._lastHeaders?.get('x-request-id') ?? '';
  }

  /**
   * 最近一次请求的配额/限流信息（来自响应头）
   *
   * @returns {Object} 如 { billing: 'member', daily_limit: '1000', daily_remaining: '973', ... }
   */
  getLastRateLimit() {
    const info = {};
    if (!this._lastHeaders) return info;
    for (const [raw, key] of Object.entries(RATE_LIMIT_HEADER_MAP)) {
      const v = this._lastHeaders.get(raw);
      if (v != null) info[key] = v;
    }
    return info;
  }

  /* ---------------- 内部实现 ---------------- */

  /**
   * @param {string} path
   * @param {Object<string, string>} query
   * @param {boolean} withAuth
   * @returns {Promise<Object>} 成功响应中的 result 对象
   */
  async #request(path, query, withAuth) {
    const url = new URL(this.baseUrl + path);
    for (const [k, v] of Object.entries(query)) url.searchParams.set(k, v);

    const headers = { Accept: 'application/json' };
    if (withAuth) {
      if (!this.appkey) {
        throw new TypeError(
          'appkey 不能为空，请传入平台签发的 AppKey ' +
          '(https://www.gold-api.cn/client/register.php 免费注册)');
      }
      // 走请求头认证，避免 appkey 出现在 URL / 访问日志中
      headers['X-API-KEY'] = this.appkey;
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeout);
    let resp;
    try {
      resp = await fetch(url, { headers, signal: controller.signal });
    } catch (err) {
      const reason = err?.cause?.message || err?.message || String(err);
      throw new GoldApiException(`网络请求失败: ${reason}`);
    } finally {
      clearTimeout(timer);
    }

    this._lastHeaders = resp.headers;

    let data;
    try {
      data = await resp.json();
    } catch {
      throw new GoldApiException(`响应不是合法 JSON (HTTP ${resp.status})`, {
        httpCode: resp.status,
      });
    }

    if (data.success !== '1') {
      throw new GoldApiException(data.msg || '未知错误', {
        msgId: data.msgId,
        requestId: data.requestId,
        httpCode: resp.status,
      });
    }

    return data.result || {};
  }
}

export default GoldApiClient;
