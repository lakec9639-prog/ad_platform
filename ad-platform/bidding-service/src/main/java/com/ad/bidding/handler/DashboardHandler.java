package com.ad.bidding.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class DashboardHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "text/html")
                .end(HTML);
    }

    private static final String HTML = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>LUMI ADX — 实时投放仪表盘</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<style>
*{margin:0;padding:0;box-sizing:border-box;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}
body{background:#0f172a;color:#e2e8f0;padding:20px}
h1{font-size:24px;margin-bottom:20px;color:#38bdf8}
h1 span{font-size:14px;color:#64748b;font-weight:400}
.stats-bar{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-bottom:24px}
.stat-card{background:#1e293b;border-radius:10px;padding:16px;text-align:center}
.stat-card .value{font-size:28px;font-weight:700;color:#38bdf8}
.stat-card .label{font-size:12px;color:#64748b;margin-top:4px}
.stat-card.win .value{color:#4ade80}
.stat-card.latency .value{color:#fbbf24}
.stat-card.qps .value{color:#c084fc}
.strategy-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px;margin-bottom:24px}
.strategy-card{background:#1e293b;border-radius:10px;padding:16px;position:relative;border-left:4px solid #38bdf8}
.strategy-card.s4{border-left-color:#ef4444}
.strategy-card.s3{border-left-color:#f97316}
.strategy-card.s1{border-left-color:#22c55e}
.strategy-card.s2{border-left-color:#a855f7}
.strategy-card.s5{border-left-color:#64748b}
.strategy-card h3{font-size:14px;margin-bottom:8px}
.strategy-card .metrics{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;font-size:13px}
.strategy-card .metrics div span{display:block;color:#64748b;font-size:11px}
.table-wrap{background:#1e293b;border-radius:10px;padding:16px;overflow-x:auto}
table{width:100%;border-collapse:collapse;font-size:13px}
th,td{padding:8px 12px;text-align:right;border-bottom:1px solid #334155}
th{color:#64748b;font-weight:600;font-size:11px;text-transform:uppercase}
td:first-child{text-align:left;font-weight:600}
tr:hover{background:#1a2332}
.info{color:#64748b;font-size:12px;margin-top:12px;text-align:center}
.bad{color:#ef4444}
.good{color:#4ade80}
.ok{color:#fbbf24}
</style>
</head>
<body>
<h1>📊 LUMI ADX 实时投放仪表盘 <span id="elapsed">0s</span></h1>
<div class="stats-bar" id="statsBar"></div>
<div class="strategy-grid" id="strategyGrid"></div>
<div class="table-wrap"><table><thead><tr>
<th>策略</th><th>请求</th><th>胜出</th><th>胜率</th><th>曝光</th><th>点击</th><th>CTR</th><th>转化</th><th>CVR</th><th>消耗(¥)</th><th>延迟(ms)</th>
</tr></thead><tbody id="tableBody"></tbody></table></div>
<div class="info">每2秒自动刷新 · 数据纯内存统计，重启即重置</div>
<script>
function fmt(n){if(n==null)return'-';if(typeof n==='number'&&n>1e6)return(n/1e6).toFixed(1)+'M';if(typeof n==='number'&&n>1e3)return(n/1e3).toFixed(1)+'K';return n?.toLocaleString()??'-'}
function pct(n){return n!=null?n.toFixed(1)+'%':'-'}
function ms(n){return n!=null?n.toFixed(1):'-'}
function bar(){fetch('/stats').then(r=>r.json()).then(d=>{
document.getElementById('elapsed').textContent=Math.floor(d.elapsedSec/60)+'m'+d.elapsedSec%60+'s';
document.getElementById('statsBar').innerHTML=
`<div class="stat-card qps"><div class="value">${fmt(Math.round(d.qps))}</div><div class="label">QPS</div></div>
<div class="stat-card"><div class="value">${fmt(d.totalRequests)}</div><div class="label">总请求</div></div>
<div class="stat-card win"><div class="value">${fmt(d.totalWins)}</div><div class="label">胜出 (${pct(d.winRate)})</div></div>
<div class="stat-card latency"><div class="value">${ms(d.avgLatencyMs)}</div><div class="label">平均延迟</div></div>`;
let grid='',rows='';const names={1:'高价值人群(S1)',2:'新品破圈(S2)',3:'竞品截流(S3)',4:'弃单重定向(S4)',5:'智能通投(S5)'};
Object.entries(d.strategies).forEach(([k,s])=>{
const n=names[s.strategyId]||'策略'+s.strategyId;
grid+=`<div class="strategy-card s${s.strategyId}"><h3>${n}</h3><div class="metrics">
<div><b>${fmt(s.impressions)}</b><span>曝光</span></div>
<div><b>${fmt(s.clicks)}</b><span>点击</span></div>
<div><b>${pct(s.ctr)}</b><span>CTR</span></div>
<div><b>${fmt(s.conversions)}</b><span>转化</span></div>
<div><b>${pct(s.cvr)}</b><span>CVR</span></div>
<div><b>¥${s.totalSpend.toFixed(2)}</b><span>消耗</span></div>
</div></div>`;
rows+=`<tr><td>${n}</td><td>${fmt(s.bids)}</td><td>${fmt(s.wins)}</td><td class="${s.winRate>30?'good':s.winRate>10?'ok':'bad'}">${pct(s.winRate)}</td><td>${fmt(s.impressions)}</td><td>${fmt(s.clicks)}</td><td class="${s.ctr>1?'good':s.ctr>0.3?'ok':'bad'}">${pct(s.ctr)}</td><td>${fmt(s.conversions)}</td><td class="${s.cvr>5?'good':s.cvr>1?'ok':'bad'}">${pct(s.cvr)}</td><td>¥${s.totalSpend.toFixed(2)}</td><td>${ms(s.avgLatencyMs)}</td></tr>`;
});
document.getElementById('strategyGrid').innerHTML=grid;
document.getElementById('tableBody').innerHTML=rows;
}).catch(()=>{});
}
bar();setInterval(bar,2000);
</script>
</body>
</html>
""";
}
