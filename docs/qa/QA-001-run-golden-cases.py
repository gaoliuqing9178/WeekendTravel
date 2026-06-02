import json
import math
import time
import urllib.error
import urllib.request
from pathlib import Path


BASE_URL = "http://127.0.0.1:8000"
RESULT_PATH = Path("docs/qa/QA-001-golden-cases-results.json")

FALSE_FLAGS = {
    "restaurantFull": False,
    "routeTooFar": False,
    "bookingFail": False,
    "ageMismatch": False,
}


CASES = [
    {
        "id": "FAM-001",
        "category": "family",
        "scenario": "family",
        "text": "今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。",
        "expect": "normal",
    },
    {
        "id": "FAM-002",
        "category": "family",
        "scenario": "family",
        "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右。",
        "expect": "normal",
    },
    {
        "id": "FAM-003",
        "category": "family",
        "scenario": "family",
        "text": "下午想带5岁孩子去室内玩，天气不好也不影响，晚上找个家庭友好的餐厅，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FAM-004",
        "category": "family",
        "scenario": "family",
        "text": "今天下午一家三口轻松活动，不想离家太远，餐厅排队不要太久，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FAM-005",
        "category": "family",
        "scenario": "family",
        "text": "孩子5岁，老婆想吃清淡一点，下午想玩亲子项目再吃饭，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FAM-006",
        "category": "family",
        "scenario": "family",
        "text": "下午半天带孩子出去玩，希望餐厅能订位，别太累，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FAM-007",
        "category": "family",
        "scenario": "family",
        "text": "下午空着，想带孩子随便逛逛，顺便吃个饭。",
        "expect": "normal",
    },
    {
        "id": "FRI-001",
        "category": "friends",
        "scenario": "friends",
        "text": "今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。",
        "expect": "normal",
    },
    {
        "id": "FRI-002",
        "category": "friends",
        "scenario": "friends",
        "text": "今天下午4个人出去玩，想找个能拍照也能吃饭的地方，安排4小时左右。",
        "expect": "normal",
    },
    {
        "id": "FRI-003",
        "category": "friends",
        "scenario": "friends",
        "text": "朋友下午想拍照、喝点咖啡或甜品，晚上一起聚餐，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FRI-004",
        "category": "friends",
        "scenario": "friends",
        "text": "4个朋友想轻松聊天，不想走太远，下午玩一会儿再吃饭，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FRI-005",
        "category": "friends",
        "scenario": "friends",
        "text": "2男2女下午出去，希望有拍照氛围，也能吃得舒服，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FRI-006",
        "category": "friends",
        "scenario": "friends",
        "text": "朋友半天下午活动，想先玩再找个能订位的餐厅，安排4小时。",
        "expect": "normal",
    },
    {
        "id": "FRI-007",
        "category": "friends",
        "scenario": "friends",
        "text": "下午空着，4个朋友想出去玩、拍照、吃饭。",
        "expect": "normal",
    },
    {
        "id": "BND-001",
        "category": "boundary",
        "scenario": "family",
        "text": "今天下午是空的，想和老婆孩子出去玩几个小时。",
        "expect": "normal",
    },
    {
        "id": "BND-002",
        "category": "boundary",
        "scenario": "family",
        "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右。",
        "expect": "degrade",
        "flags": {"restaurantFull": True},
    },
    {
        "id": "BND-003",
        "category": "boundary",
        "scenario": "family",
        "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右。",
        "expect": "degrade",
        "flags": {"routeTooFar": True},
    },
    {
        "id": "BND-004",
        "category": "boundary",
        "scenario": "family",
        "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右。",
        "expect": "bookingFail",
        "flags": {"bookingFail": True},
    },
    {
        "id": "BND-005",
        "category": "boundary",
        "scenario": "family",
        "text": "孩子5岁，下午想安排亲子活动和家庭餐厅，安排4小时。",
        "expect": "degrade",
        "flags": {"ageMismatch": True},
    },
    {
        "id": "BND-006",
        "category": "boundary",
        "scenario": "family",
        "text": "今天下午想和老婆孩子找个轻松的亲子活动，再吃个不用排太久的晚饭，安排4小时左右。",
        "expect": "adjust",
        "adjustInstruction": "换一家餐厅，要能订位的",
    },
    {
        "id": "BND-007",
        "category": "boundary",
        "scenario": "solo",
        "text": "今天下午自己出去玩。",
        "expect": "invalid",
    },
]


def request_json(method, path, payload=None, timeout=10):
    body = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(BASE_URL + path, data=body, method=method, headers=headers)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            text = response.read().decode("utf-8")
            elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
            return response.status, parse_json(text), text, elapsed_ms
    except urllib.error.HTTPError as error:
        text = error.read().decode("utf-8")
        elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
        return error.code, parse_json(text), text, elapsed_ms


def request_text(method, path, payload=None, timeout=10, accept="text/plain"):
    body = None
    headers = {"Accept": accept}
    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(BASE_URL + path, data=body, method=method, headers=headers)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            text = response.read().decode("utf-8")
            elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
            return response.status, text, elapsed_ms
    except urllib.error.HTTPError as error:
        text = error.read().decode("utf-8")
        elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
        return error.code, text, elapsed_ms


def parse_json(text):
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


def parse_sse(text):
    events = []
    event_name = None
    data_lines = []

    def flush():
        nonlocal event_name, data_lines
        if event_name is None and not data_lines:
            return
        data_text = "\n".join(data_lines)
        data = parse_json(data_text) if data_text else None
        events.append({
            "event": event_name,
            "type": data.get("type") if isinstance(data, dict) else event_name,
            "data": data,
        })
        event_name = None
        data_lines = []

    for raw_line in text.splitlines():
        line = raw_line.strip("\r")
        if line == "":
            flush()
        elif line.startswith("event:"):
            event_name = line[len("event:"):].strip()
        elif line.startswith("data:"):
            data_lines.append(line[len("data:"):].strip())
    flush()
    return events


def merged_flags(case):
    flags = dict(FALSE_FLAGS)
    flags.update(case.get("flags", {}))
    return flags


def reset_flags():
    status, payload, text, elapsed_ms = request_json("POST", "/api/debug/scenario", FALSE_FLAGS)
    if status != 200:
        raise RuntimeError(f"failed to reset scenario flags: {status} {text}")
    return elapsed_ms


def set_flags(flags):
    status, payload, text, elapsed_ms = request_json("POST", "/api/debug/scenario", flags)
    if status != 200:
        raise RuntimeError(f"failed to set scenario flags: {status} {text}")
    return elapsed_ms


def stream_plan(plan_id):
    status, text, elapsed_ms = request_text(
        "GET",
        f"/api/plan/{plan_id}/stream",
        accept="text/event-stream",
        timeout=10,
    )
    return status, parse_sse(text), text, elapsed_ms


def last_plan(events):
    for event in reversed(events):
        data = event.get("data")
        if not isinstance(data, dict):
            continue
        if event.get("type") == "plan_ready":
            return data.get("plan")
        if event.get("type") == "adjust_result":
            return data.get("plan")
    return None


def final_state(events, plan=None):
    for event in reversed(events):
        data = event.get("data")
        if not isinstance(data, dict):
            continue
        if event.get("type") == "done":
            return "DONE"
        if event.get("type") == "error":
            return data.get("code") or "ERROR"
        if event.get("type") == "state_change":
            return data.get("to")
    if isinstance(plan, dict):
        return plan.get("status")
    return None


def has_friends_timeline(plan):
    if not isinstance(plan, dict):
        return False
    types = [slot.get("type") for slot in plan.get("timeline", []) if isinstance(slot, dict)]
    return "activity" in types and "restaurant" in types and ("cafe" in types or "dessert" in types)


def has_family_timeline(plan):
    if not isinstance(plan, dict):
        return False
    types = [slot.get("type") for slot in plan.get("timeline", []) if isinstance(slot, dict)]
    return "activity" in types and "restaurant" in types


def execute_case(case):
    case_started = time.perf_counter()
    reset_flags()
    flags = merged_flags(case)
    set_flags(flags)

    record = {
        "caseId": case["id"],
        "category": case["category"],
        "scenario": case["scenario"],
        "expect": case["expect"],
        "flags": flags,
        "planId": None,
        "createStatus": None,
        "clarified": False,
        "adjusted": False,
        "executed": False,
        "planningMs": 0.0,
        "adjustMs": 0.0,
        "executionMs": 0.0,
        "totalMs": 0.0,
        "finalState": None,
        "eventTypes": [],
        "stateTransitions": [],
        "planSummary": None,
        "planIsPlanB": None,
        "planReplanCount": None,
        "executeStatuses": [],
        "errorCode": None,
        "pass": False,
        "failures": [],
    }

    def fail(message):
        record["failures"].append(message)

    if case["expect"] == "invalid":
        status, payload, raw_text, create_ms = request_json("POST", "/api/plan", {
            "text": case["text"],
            "scenario": case["scenario"],
            "origin": "当前位置",
        })
        record["createStatus"] = status
        record["planningMs"] = create_ms
        record["totalMs"] = round((time.perf_counter() - case_started) * 1000, 2)
        record["errorCode"] = payload.get("error") if isinstance(payload, dict) else None
        record["finalState"] = record["errorCode"]
        record["pass"] = status == 400 and record["errorCode"] == "INVALID_INPUT"
        if not record["pass"]:
            fail(f"expected 400 INVALID_INPUT, got {status} {raw_text}")
        reset_flags()
        return record

    status, payload, raw_text, create_ms = request_json("POST", "/api/plan", {
        "text": case["text"],
        "scenario": case["scenario"],
        "origin": "当前位置",
    })
    record["createStatus"] = status
    record["planningMs"] += create_ms
    if status != 202 or not isinstance(payload, dict) or not payload.get("planId"):
        fail(f"create failed: {status} {raw_text}")
        record["totalMs"] = round((time.perf_counter() - case_started) * 1000, 2)
        reset_flags()
        return record

    plan_id = payload["planId"]
    record["planId"] = plan_id

    status, planning_events, raw_stream, stream_ms = stream_plan(plan_id)
    record["planningMs"] += stream_ms
    all_events = list(planning_events)
    if status != 200:
        fail(f"planning stream failed: {status}")

    if any(event["type"] == "clarification_request" for event in planning_events):
        record["clarified"] = True
        status, payload, raw_text, clarify_ms = request_json("POST", f"/api/plan/{plan_id}/clarify", {"reply": "4-6小时"})
        record["planningMs"] += clarify_ms
        if status != 200:
            fail(f"clarify failed: {status} {raw_text}")
        status, resumed_events, raw_stream, resumed_ms = stream_plan(plan_id)
        record["planningMs"] += resumed_ms
        all_events.extend(resumed_events)
        if status != 200:
            fail(f"resumed planning stream failed: {status}")

    plan = last_plan(all_events)

    if case["expect"] == "degrade":
        record_events(record, all_events, plan)
        has_error_degrade = any(event["type"] == "error" and event.get("data", {}).get("code") == "DEGRADE" for event in all_events)
        has_replan = any(event["type"] == "replan" for event in all_events)
        if not has_error_degrade:
            fail("expected error(code=DEGRADE)")
        if case["id"] in {"BND-002", "BND-003"} and not has_replan:
            fail("expected replan event for injected Plan B case")
        record["pass"] = not record["failures"]
        record["totalMs"] = round((time.perf_counter() - case_started) * 1000, 2)
        reset_flags()
        return record

    if not plan:
        fail("missing plan_ready plan")
    else:
        record["planSummary"] = plan.get("summary")
        record["planIsPlanB"] = plan.get("isPlanB")
        record["planReplanCount"] = plan.get("replanCount")
        if plan.get("scenario") != case["scenario"]:
            fail(f"plan scenario mismatch: {plan.get('scenario')}")
        if case["scenario"] == "friends" and not has_friends_timeline(plan):
            fail("friends timeline missing activity + cafe/dessert + restaurant")
        if case["scenario"] == "family" and not has_family_timeline(plan):
            fail("family timeline missing activity + restaurant")

    if case["expect"] == "adjust":
        status, payload, raw_text, adjust_request_ms = request_json(
            "PATCH",
            f"/api/plan/{plan_id}/adjust",
            {"instruction": case["adjustInstruction"]},
        )
        record["adjustMs"] += adjust_request_ms
        if status != 202:
            fail(f"adjust request failed: {status} {raw_text}")
        status, adjust_events, raw_stream, adjust_stream_ms = stream_plan(plan_id)
        record["adjustMs"] += adjust_stream_ms
        all_events.extend(adjust_events)
        record["adjusted"] = any(event["type"] == "adjust_result" for event in adjust_events)
        if not record["adjusted"]:
            fail("expected adjust_result")
        adjusted_plan = last_plan(adjust_events)
        if adjusted_plan:
            plan = adjusted_plan

    status, payload, raw_text, execute_request_ms = request_json("POST", f"/api/plan/{plan_id}/execute", {"confirmed": True})
    record["executionMs"] += execute_request_ms
    record["executed"] = status == 200
    if status != 200:
        fail(f"execute request failed: {status} {raw_text}")
    status, execution_events, raw_stream, execution_stream_ms = stream_plan(plan_id)
    record["executionMs"] += execution_stream_ms
    all_events.extend(execution_events)
    if status != 200:
        fail(f"execution stream failed: {status}")

    record_events(record, all_events, plan)
    execute_results = [event.get("data", {}) for event in all_events if event["type"] == "execute_result"]
    record["executeStatuses"] = [result.get("status") for result in execute_results]

    if not any(event["type"] == "execute_result" for event in all_events):
        fail("missing execute_result")
    if not any(event["type"] == "done" for event in all_events):
        fail("missing done")
    if case["expect"] == "bookingFail" and "failed" not in record["executeStatuses"]:
        fail("expected failed execute_result under bookingFail")
    if case["expect"] in {"normal", "adjust"} and any(status != "success" for status in record["executeStatuses"]):
        fail(f"expected all success execute statuses, got {record['executeStatuses']}")

    record["pass"] = not record["failures"]
    record["totalMs"] = round((time.perf_counter() - case_started) * 1000, 2)
    reset_flags()
    return record


def record_events(record, events, plan):
    record["eventTypes"] = [event["type"] for event in events]
    record["stateTransitions"] = [
        {
            "from": event["data"].get("from"),
            "to": event["data"].get("to"),
        }
        for event in events
        if event["type"] == "state_change" and isinstance(event.get("data"), dict)
    ]
    record["finalState"] = final_state(events, plan)
    for event in events:
        if event["type"] == "error" and isinstance(event.get("data"), dict):
            record["errorCode"] = event["data"].get("code")


def percentile(values, p):
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (len(ordered) - 1) * p
    lower = math.floor(rank)
    upper = math.ceil(rank)
    if lower == upper:
        return ordered[int(rank)]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (rank - lower)


def compute_metrics(results):
    non_invalid = [item for item in results if item["expect"] != "invalid" and item["createStatus"] == 202]
    normal_non_injected = [
        item for item in non_invalid
        if item["flags"] == FALSE_FLAGS and item["category"] in {"family", "friends"} and item["expect"] == "normal"
    ]
    scenario_applicable = [
        item for item in non_invalid
        if item["scenario"] in {"family", "friends"} and item["expect"] in {"normal", "adjust", "bookingFail"}
    ]
    injected_plan_b = [
        item for item in non_invalid
        if item["flags"].get("restaurantFull") or item["flags"].get("routeTooFar") or item["flags"].get("ageMismatch")
    ]
    normal_plan_b = [
        item for item in normal_non_injected
        if item.get("planIsPlanB") or "replan" in item.get("eventTypes", [])
    ]
    feasible = [
        item for item in normal_non_injected
        if "plan_ready" in item.get("eventTypes", []) and item["pass"]
    ]
    scenario_correct = [
        item for item in scenario_applicable
        if "plan scenario mismatch" not in " ".join(item.get("failures", []))
    ]
    injected_triggered = [
        item for item in injected_plan_b
        if "replan" in item.get("eventTypes", []) and item.get("errorCode") == "DEGRADE"
    ]
    return {
        "caseCount": len(results),
        "passCount": sum(1 for item in results if item["pass"]),
        "failCount": sum(1 for item in results if not item["pass"]),
        "planningMsP95AllCreated": round(percentile([item["planningMs"] for item in non_invalid], 0.95), 2),
        "planningMsP95NormalNonInjected": round(percentile([item["planningMs"] for item in normal_non_injected], 0.95), 2),
        "normalNonInjectedFeasibilityRate": round(len(feasible) / len(normal_non_injected), 4) if normal_non_injected else None,
        "normalNonInjectedPlanBRate": round(len(normal_plan_b) / len(normal_non_injected), 4) if normal_non_injected else None,
        "injectedPlanBTriggerAccuracy": round(len(injected_triggered) / len(injected_plan_b), 4) if injected_plan_b else None,
        "intentScenarioAccuracy": round(len(scenario_correct) / len(scenario_applicable), 4) if scenario_applicable else None,
        "injectedPlanBCaseIds": [item["caseId"] for item in injected_plan_b],
        "normalNonInjectedCaseIds": [item["caseId"] for item in normal_non_injected],
    }


def main():
    health_status, health_payload, health_text, health_ms = request_json("GET", "/health")
    if health_status != 200:
        raise RuntimeError(f"backend health failed: {health_status} {health_text}")

    results = []
    for case in CASES:
        result = execute_case(case)
        results.append(result)
        verdict = "PASS" if result["pass"] else "FAIL"
        print(f"{case['id']} {verdict} planId={result['planId']} final={result['finalState']} totalMs={result['totalMs']}")

    payload = {
        "runId": "QA-001-EVAL-CODEX-20260602T1647+0800",
        "executedAt": "2026-06-02T16:47:00+08:00",
        "baseUrl": BASE_URL,
        "health": {
            "status": health_status,
            "payload": health_payload,
            "elapsedMs": health_ms,
        },
        "cases": results,
        "metrics": compute_metrics(results),
    }
    RESULT_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(payload["metrics"], ensure_ascii=False, indent=2))

    if payload["metrics"]["failCount"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
