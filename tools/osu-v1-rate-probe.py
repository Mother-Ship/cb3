#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
osu! API v1（/api/**）限流探针。

用途
----
以固定速率持续请求 osu! API v1，测出「从某个出口 IP 出发，发多久 / 多少请求后会触发
Cloudflare 1015（HTTP 429），以及 Cloudflare 给的 Retry-After 是多少」。

背景
----
/api/** 不返回任何限流响应头（不像 /api/v2/** 有 x-ratelimit-limit），超限时 Cloudflare
直接在 edge 上返回 429 + "error code: 1015"。所以“安全速率是多少”只能靠主动探测。

实现要点
--------
* 用 http.client 复用长连接（urllib 每次重建 TLS，本机实测单请求 ~350ms，会被延迟卡死而
  不是被限速卡死）。
* 全局匀速放行：所有 worker 共享一个 next_slot，请求之间至少间隔 60/rate 秒，不允许突发
  —— 和线上 OsuRateLimiter.Bucket 的策略一致。
* 单请求 RTT 通常 100~250ms，所以 rate 高时需要 --concurrency 提供足够的在途请求数
  （capacity ≈ concurrency / RTT）。

用法
----
    # 默认从 src/main/resources/cabbage.properties 读 apikey
    python3 tools/osu-v1-rate-probe.py --rate 300 --duration 600 --concurrency 4

    python3 tools/osu-v1-rate-probe.py --rate 600 --duration 300 --concurrency 8 \
        --csv /tmp/probe.csv

判读
----
* 跑满 duration 一次 429 都没有  -> 该速率对该出口 IP 至少在这段时间内是安全的
* 在 ~N 次请求后固定触发 429     -> 限制更像“每个窗口的总请求数配额”，而不是瞬时 QPS
* 一上来就 429                   -> 该 IP 还在封禁窗口里，先等 Retry-After 结束再测

注意
----
* 一旦触发 429，该出口 IP 会被封 Retry-After 秒（实测约 1800s），期间所有 v1 请求都会被拒。
  脚本在第一次非 200 时立即停下，不再继续加重封禁。
* 请在“有意义的出口 IP”上跑：Cloudflare 1015 按客户端 IP 判定，换台机器测的结果不能直接
  套到生产机上。
"""

import argparse
import csv
import http.client
import itertools
import random
import ssl
import sys
import threading
import time

HOST = "osu.ppy.sh"
PATH = "/api/get_user"
USER_AGENT = "cb3/0.0.1 (+https://github.com/Mother-Ship/cb3)"


def read_key(path, literal):
    if literal:
        return literal.strip()
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if line.startswith("apikey="):
                value = line.split("=", 1)[1].strip()
                if value:
                    return value
    raise SystemExit("没有在 %s 里找到非空的 apikey=" % path)


def main():
    ap = argparse.ArgumentParser(description="osu! API v1 限流探针")
    ap.add_argument("--rate", type=float, default=600, help="目标速率（次/分钟），默认 600")
    ap.add_argument("--duration", type=float, default=300, help="最长运行秒数，默认 300")
    ap.add_argument("--concurrency", type=int, default=4, help="并发连接数，默认 4")
    ap.add_argument("--key-file", default="src/main/resources/cabbage.properties")
    ap.add_argument("--key", default=None, help="直接指定 apikey（会出现在命令行历史里，慎用）")
    ap.add_argument("--csv", default=None, help="把每条请求记录写到 CSV")
    ap.add_argument("--timeout", type=float, default=20, help="单请求超时秒数")
    args = ap.parse_args()

    key = read_key(args.key_file, args.key)
    interval = 60.0 / args.rate
    ctx = ssl.create_default_context()

    print("目标速率 %.0f 次/分钟（间隔 %.1f ms），并发 %d，最长 %.0f 秒，预计最多 %d 次请求"
          % (args.rate, interval * 1000, args.concurrency, args.duration,
             int(args.duration / interval)))
    sys.stdout.flush()

    start = time.monotonic()
    stop = threading.Event()
    lock = threading.Lock()
    state = {"next_slot": start, "sent": 0, "ok": 0, "failure": None}
    counter = itertools.count(1)

    csv_file = open(args.csv, "w", newline="", encoding="utf-8") if args.csv else None
    csv_writer = csv.writer(csv_file) if csv_file else None
    if csv_writer:
        csv_writer.writerow(["index", "elapsed_s", "http_code", "retry_after", "body"])

    def acquire_slot():
        with lock:
            slot = max(state["next_slot"], time.monotonic())
            state["next_slot"] = slot + interval
            return slot

    def record(index, code, retry_after, body):
        elapsed = time.monotonic() - start
        with lock:
            state["sent"] += 1
            if code == 200:
                state["ok"] += 1
            elif state["failure"] is None:
                state["failure"] = (index, elapsed, code, retry_after, body)
                stop.set()
        if csv_writer:
            with lock:
                csv_writer.writerow([index, "%.3f" % elapsed, code, retry_after,
                                     body.replace("\n", " ")[:200]])

    def worker():
        conn = None
        while not stop.is_set():
            slot = acquire_slot()
            now = time.monotonic()
            if now < slot:
                time.sleep(slot - now)
            if stop.is_set() or time.monotonic() - start > args.duration:
                break

            index = next(counter)
            # 随机 uid，避免任何缓存干扰；不存在的 uid 也是 200 + []，不影响测速
            path = "%s?k=%s&u=%d&type=id&m=0" % (PATH, key, random.randint(1, 50_000_000))
            code, retry_after, body = -1, "", ""
            for attempt in (1, 2):
                try:
                    if conn is None:
                        conn = http.client.HTTPSConnection(HOST, timeout=args.timeout, context=ctx)
                    conn.request("GET", path, headers={"User-Agent": USER_AGENT})
                    resp = conn.getresponse()
                    body = resp.read(200).decode("utf-8", "replace")
                    code = resp.status
                    retry_after = resp.getheader("Retry-After") or ""
                    break
                except Exception as e:  # 连接被 edge 掐断等，重连一次
                    try:
                        if conn:
                            conn.close()
                    except Exception:
                        pass
                    conn = None
                    code, body = -1, repr(e)
            record(index, code, retry_after, body)

            if index % 200 == 0:
                with lock:
                    sent, ok = state["sent"], state["ok"]
                el = time.monotonic() - start
                print("  已发 %d 次，成功 %d 次，用时 %.1fs，实际 %.0f 次/分钟"
                      % (sent, ok, el, sent / el * 60 if el > 0 else 0))
                sys.stdout.flush()
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(args.concurrency)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    if csv_file:
        csv_file.close()

    elapsed = time.monotonic() - start
    with lock:
        sent, ok, failure = state["sent"], state["ok"], state["failure"]

    print()
    print("=== 汇总 ===")
    print("发送 %d 次，成功 %d 次，用时 %.1fs，平均 %.0f 次/分钟"
          % (sent, ok, elapsed, sent / elapsed * 60 if elapsed > 0 else 0))
    if failure:
        i, t, code, ra, body = failure
        print("!! 第 %d 次请求（第 %.1f 秒）返回 HTTP %s，已停止" % (i, t, code))
        if ra:
            print("   Retry-After = %s 秒" % ra)
        print("   响应体: %s" % body.strip()[:200])
        print("   结论：该出口 IP 连续成功 %d 次（约 %.1f 秒）后触发限流；"
              "限制更像“窗口内总请求数配额”而不是瞬时 QPS。" % (ok, t))
    else:
        print("跑满 %.0f 秒未出现任何 429 —— 速率 %.0f 次/分钟在该出口 IP 上暂时安全。"
              % (args.duration, args.rate))


if __name__ == "__main__":
    main()
