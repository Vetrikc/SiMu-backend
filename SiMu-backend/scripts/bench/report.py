#!/usr/bin/env python3
"""Builds a human-readable report from a benchmark run directory."""
import csv
import os
import re
import sys
from collections import defaultdict


def parse_wrk(path):
    text = open(path, encoding='utf-8', errors='replace').read()
    out = {'file': os.path.basename(path)}
    m = re.search(r'Requests/sec:\s+([\d.]+)', text)
    out['rps'] = float(m.group(1)) if m else 0.0
    m = re.search(r'Latency\s+([\d.]+)(ms|s|us)', text)
    out['avg_ms'] = _ms(m.group(1), m.group(2)) if m else 0.0
    m = re.search(r'Non-2xx or 3xx responses:\s+(\d+)', text)
    out['errors'] = int(m.group(1)) if m else 0
    m = re.search(r'Socket errors:\s+(\d+)', text)
    out['socket_errors'] = int(m.group(1)) if m else 0
    for pct in ('50', '75', '90', '99'):
        m = re.search(r'^\s*' + pct + r'%\s+([\d.]+)(ms|s|us)', text, re.M)
        out['p' + pct] = _ms(m.group(1), m.group(2)) if m else 0.0
    return out


def _ms(value, unit):
    v = float(value)
    return v if unit == 'ms' else (v * 1000 if unit == 's' else v / 1000)


def parse_docker_csv(path):
    rows = []
    if not os.path.exists(path):
        return rows
    with open(path, newline='', encoding='utf-8', errors='replace') as f:
        for row in csv.DictReader(f):
            try:
                row['epoch'] = int(row['epoch'])
                row['cpu_percent'] = float(row['cpu_percent'] or 0)
            except (ValueError, KeyError):
                continue
            rows.append(row)
    return rows


def percentile(values, p):
    if not values:
        return 0.0
    values = sorted(values)
    return values[min(len(values) - 1, int(len(values) * p / 100))]


def read_text(path):
    try:
        return open(path, encoding='utf-8', errors='replace').read()
    except OSError:
        return ''


def prom_value(text, metric):
    total = 0.0
    found = False
    for line in text.splitlines():
        if line.startswith('#'):
            continue
        if line.startswith(metric):
            parts = line.rsplit(' ', 1)
            if len(parts) == 2:
                try:
                    total += float(parts[1])
                    found = True
                except ValueError:
                    pass
    return total if found else None


def main():
    results = sys.argv[1] if len(sys.argv) > 1 else '.'
    nproc = int(read_text(os.path.join(results, 'nproc.txt')).strip() or 0) or os.cpu_count() or 1
    total_cpu = nproc * 100.0

    print('# SiMu benchmark report')
    print()
    print('## Environment')
    print()
    print(f"- cores: **{nproc}**")
    for name in ('docker-version.txt', 'uname.txt'):
        value = read_text(os.path.join(results, name)).strip()
        if value:
            print(f"- {name[:-4]}: `{value}`")
    mem = read_text(os.path.join(results, 'free.txt')).splitlines()
    if len(mem) > 1:
        print(f"- memory: `{mem[1].strip()}`")
    print()

    wrk_dir = os.path.join(results, 'wrk')
    runs = []
    if os.path.isdir(wrk_dir):
        for name in sorted(os.listdir(wrk_dir)):
            if name.endswith('.txt'):
                runs.append(parse_wrk(os.path.join(wrk_dir, name)))
    print('## Load results')
    print()
    print('| scenario | RPS | p50 ms | p99 ms | avg ms | errors |')
    print('|---|---|---|---|---|---|')
    for r in sorted(runs, key=lambda x: x['file']):
        print(f"| {r['file'][:-4]} | {r['rps']:.0f} | {r['p50']:.1f} | {r['p99']:.1f} | {r['avg_ms']:.1f} | {r['errors']} |")
    print()

    rows = parse_docker_csv(os.path.join(results, 'stats', 'docker-stats.csv'))

    print('## Peak CPU per scenario')
    print()
    print('| scenario | top containers (peak CPU %) | total % of host |')
    print('|---|---|---|')
    if os.path.isdir(wrk_dir):
        for name in sorted(os.listdir(wrk_dir)):
            if not name.endswith('.txt'):
                continue
            base = name[:-4]
            start_file = os.path.join(wrk_dir, base + '.start')
            end_file = os.path.join(wrk_dir, base + '.end')
            if not (os.path.exists(start_file) and os.path.exists(end_file)):
                continue
            start = int(read_text(start_file).strip() or 0)
            end = int(read_text(end_file).strip() or 0)
            window = [r for r in rows if start - 2 <= r['epoch'] <= end + 2]
            if not window:
                continue
            peaks = defaultdict(float)
            for r in window:
                peaks[r['container']] = max(peaks[r['container']], r['cpu_percent'])
            top = sorted(peaks.items(), key=lambda kv: -kv[1])[:3]
            label = ', '.join(f'{c} {v:.0f}%' for c, v in top)
            total = sum(peaks.values())
            print(f"| {base} | {label} | {total:.0f}% ({total / (nproc * 100) * 100:.0f}%) |")
    print()
    per_container = defaultdict(lambda: {'max': 0.0, 'sum': 0.0, 'n': 0, 'mem': 0.0})
    for row in rows:
        c = per_container[row['container']]
        c['max'] = max(c['max'], row['cpu_percent'])
        c['sum'] += row['cpu_percent']
        c['n'] += 1
        c['mem'] = max(c['mem'], _to_mb(row.get('mem_usage', '')))
    print('## CPU per container (whole run)')
    print()
    print('| container | peak CPU % | avg CPU % | peak memory |')
    print('|---|---|---|---|')
    for name, c in sorted(per_container.items(), key=lambda kv: -kv[1]['max']):
        avg = c['sum'] / c['n'] if c['n'] else 0
        print(f"| {name} | {c['max']:.0f} | {avg:.0f} | {c['mem']:.0f} MB |")
    print()

    peak_total = sum(c['max'] for c in per_container.values())
    print(f"Total peak CPU across containers: **{peak_total:.0f}%** of **{total_cpu:.0f}%** "
          f"({peak_total / total_cpu * 100:.0f}% of the host)")
    top = max(per_container.items(), key=lambda kv: kv[1]['max'], default=(None, None))
    if top[0]:
        print(f"Top consumer: **{top[0]}** ({top[1]['max']:.0f}% ≈ {top[1]['max'] / 100:.1f} cores)")
    print()

    snap_dir = os.path.join(results, 'snapshots')
    if os.path.isdir(snap_dir):
        labels = sorted(os.listdir(snap_dir))
        last = labels[-1] if labels else None
        if last:
            base = os.path.join(snap_dir, last)
            print(f'## Dependency snapshot: `{last}`')
            print()
            for port, label in (('8081', 'gateway'), ('8084', 'music'), ('8085', 'playlist'), ('8083', 'user'), ('8082', 'auth')):
                text = read_text(os.path.join(base, f'prometheus-{port}.txt'))
                if not text:
                    continue
                pending = prom_value(text, 'hikaricp_connections_pending')
                busy = prom_value(text, 'tomcat_threads_busy_threads')
                gc = prom_value(text, 'jvm_gc_pause_seconds_count')
                parts = []
                if pending is not None:
                    parts.append(f'hikari pending {pending:.0f}')
                if busy is not None:
                    parts.append(f'tomcat busy {busy:.0f}')
                if gc is not None:
                    parts.append(f'gc pauses {gc:.0f}')
                if parts:
                    print(f"- {label}: " + ', '.join(parts))
            redis = read_text(os.path.join(base, 'redis-info.txt'))
            m = re.search(r'instantaneous_ops_per_sec:(\d+)', redis)
            if m:
                print(f"- redis: {m.group(1)} ops/s")
            print()

    mpstat = read_text(os.path.join(results, 'stats', 'mpstat.log'))
    if mpstat:
        idle = []
        for line in mpstat.splitlines():
            parts = line.split()
            if len(parts) >= 12 and parts[-1].replace('.', '', 1).isdigit():
                idle.append(float(parts[-1]))
        if idle:
            print(f'## CPU idle during sampling: min **{min(idle):.0f}%**, avg **{sum(idle) / len(idle):.0f}%**')
            print()

    print('## How to read this')
    print()
    print('- CPU % is per core: 100 means one fully busy core.')
    print('- If the top consumer is a gateway and the host is below ~80%, the remaining limit is the client or the network.')
    print('- Hikari `pending > 0` means the pool is too small; `tomcat busy` close to the thread limit means the thread pool is exhausted.')
    print('- Compare RPS of the same scenario across concurrency levels: the point where RPS stops growing is saturation.')


def _to_mb(value):
    m = re.match(r'([\d.]+)\s*([KMGi]*B)', value or '')
    if not m:
        return 0.0
    number = float(m.group(1))
    unit = m.group(2).upper()
    if unit.startswith('G'):
        return number * 1024
    if unit.startswith('K'):
        return number / 1024
    return number


if __name__ == '__main__':
    main()
