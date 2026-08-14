# -*- coding: utf-8 -*-
"""数据分析脚本：历史投放明细聚合分析（面试项目文档数据支撑）
数据来源：docs/ref/客户历史数据_v3.csv（180 行计划×日期粒度）
产出：总览 / 分渠道 / 分计划 的消耗、曝光、点击、微转化、订单、GMV
     及 CPA / CTR / CVR / CPM / CPC / ROAS 派生指标
"""
import csv
from collections import defaultdict

rows = []
with open(r"D:\20260716-GT\AD\docs\ref\客户历史数据_v3.csv", encoding="utf-8-sig") as f:
    for r in csv.DictReader(f):
        rows.append(r)

def num(s, default=None):
    s = (s or "").strip().replace(",", "")
    if s in ("", "#N/A", "N/A"):
        return default
    try:
        return float(s)
    except ValueError:
        return default

total = defaultdict(float)
by_channel = defaultdict(lambda: defaultdict(float))
by_plan = defaultdict(lambda: defaultdict(float))
micro_missing = 0
n = 0

for r in rows:
    n += 1
    cost = num(r["消耗(元)"], 0.0) or 0.0
    imp = num(r["曝光"], 0.0) or 0.0
    clk = num(r["点击"], 0.0) or 0.0
    micro = num(r["微转化数"])
    ords = num(r["首购订单数"], 0.0) or 0.0
    gmv = num(r["首购GMV(元)"], 0.0) or 0.0
    ch = r["媒体"]
    plan = r["计划名"]
    if micro is None:
        micro_missing += 1
    m = micro if micro is not None else 0.0
    for d, key in ((total, "total"), (by_channel[ch], ch), (by_plan[plan], plan)):
        d["cost"] += cost; d["imp"] += imp; d["clk"] += clk
        d["micro"] += m; d["orders"] += ords; d["gmv"] += gmv

def fmt(d):
    cpa = d["cost"] / d["orders"] if d["orders"] else float("nan")
    ctr = d["clk"] / d["imp"] if d["imp"] else float("nan")
    cvr = d["orders"] / d["clk"] if d["clk"] else float("nan")
    cpm = d["cost"] / d["imp"] * 1000 if d["imp"] else float("nan")
    cpc = d["cost"] / d["clk"] if d["clk"] else float("nan")
    roas = d["gmv"] / d["cost"] if d["cost"] else float("nan")
    return (f"消耗={d['cost']:,.0f} 曝光={d['imp']:,.0f} 点击={d['clk']:,.0f} "
            f"微转化={d['micro']:,.0f} 订单={d['orders']:,.0f} GMV={d['gmv']:,.0f} "
            f"CPA={cpa:.0f} CTR={ctr:.2%} CVR={cvr:.2%} CPM={cpm:.1f} CPC={cpc:.2f} ROAS={roas:.2f}")

print(f"=== 总览（{n} 行） ===")
print(fmt(total))
print(f"微转化缺失行数: {micro_missing}")
print()
print("=== 分渠道 ===")
for ch in sorted(by_channel, key=lambda c: -by_channel[c]["cost"]):
    print(f"{ch}: {fmt(by_channel[ch])}")
print()
print("=== 分计划（按消耗降序） ===")
for p in sorted(by_plan, key=lambda c: -by_plan[c]["cost"]):
    print(f"{p}: {fmt(by_plan[p])}")
