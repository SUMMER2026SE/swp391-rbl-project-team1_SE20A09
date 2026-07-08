import json
import os
import uuid
import time
import requests
import sys
from pathlib import Path

# Force standard output and error to use UTF-8 encoding on Windows consoles
try:
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8')
except Exception:
    pass


EVAL_DIR = Path(__file__).parent
SCENARIOS_FILE = EVAL_DIR / "scenarios.json"
LATEST_SNAPSHOT = EVAL_DIR / "snapshots" / "latest.json"

API_URL = "http://localhost:8080/api/v1/ai/chat"
PING_URL = "http://localhost:8080"


def wait_for_server(url=PING_URL, timeout=60):
    start_time = time.time()
    print(f"Checking if backend server is up at {url}...")
    while True:
        try:
            # Any HTTP response shows the server is running and listening
            requests.get(url, timeout=2)
            print("Backend server is UP!")
            return True
        except requests.exceptions.RequestException:
            if time.time() - start_time > timeout:
                raise RuntimeError(
                    f"Backend server at {url} did not start within {timeout} seconds. Please run ./mvnw spring-boot:run first."
                )
            print("Waiting for backend server to start...")
            time.sleep(2)


def check_assertion(expect, response_result):
    """
    Kiểm tra xem kết quả từ API có khớp với mong đợi (expect) hay không.
    Trả về: (is_passed: bool, error_reason: str)
    """
    intent = response_result.get("intent")
    message = response_result.get("message", "")
    stadiums = response_result.get("stadiums")
    slots = response_result.get("slots")
    matches = response_result.get("matches")
    policy_text = response_result.get("policyText")

    if "intent" in expect:
        expected_intent = expect["intent"]
        if intent != expected_intent:
            return False, f"Expected intent '{expected_intent}', but got '{intent}'"

    if expect.get("stadiumsNotEmpty", False):
        if not stadiums or len(stadiums) == 0:
            return False, "Expected stadiums to be not empty, but was null or empty"

    if expect.get("slotsNotNull", False):
        if slots is None:
            return False, "Expected slots to be not null, but was null"

    if expect.get("matchesNotEmpty", False):
        if not matches or len(matches) == 0:
            return False, "Expected matches to be not empty, but was null or empty"

    if expect.get("policyTextNotNull", False):
        if policy_text is None:
            return False, "Expected policyText to be not null, but was null"

    if "messageContains" in expect:
        for val in expect["messageContains"]:
            if val.lower() not in message.lower():
                return False, f"Expected message to contain '{val}', but got '{message}'"

    if "messageNotContains" in expect:
        for val in expect["messageNotContains"]:
            if val.lower() in message.lower():
                return False, f"Expected message to NOT contain '{val}', but got '{message}'"

    return True, "PASS"


def main():
    wait_for_server()

    if not SCENARIOS_FILE.exists():
        print(f"Error: scenarios.json not found at {SCENARIOS_FILE}")
        return

    with open(SCENARIOS_FILE, "r", encoding="utf-8") as f:
        scenarios = json.load(f)

    os.makedirs(LATEST_SNAPSHOT.parent, exist_ok=True)

    results = []
    print(f"\n=== BẮT ĐẦU CHẠY KIỂM THỬ HÀNH VI LLM ({len(scenarios)} SCENARIOS) ===")

    for i, scenario in enumerate(scenarios):
        sc_id = scenario["id"]
        description = scenario["description"]
        print(f"\n[{i + 1}/{len(scenarios)}] Đang chạy scenario: {sc_id} ({description})")

        session_id = str(uuid.uuid4())
        history = []
        scenario_passed = True
        turns_results = []

        for turn_idx, turn in enumerate(scenario["turns"]):
            # Tránh rate limit của Groq và Controller (khoảng 12s mỗi request)
            if i > 0 or turn_idx > 0:
                print("Giãn cách 12s chống Rate Limit (Groq/Controller)...")
                time.sleep(12)

            payload = {
                "message": turn["message"],
                "history": history
            }
            headers = {
                "X-Session-ID": session_id,
                "Content-Type": "application/json"
            }

            max_retries = 3
            result_data = None
            for retry in range(max_retries):
                print(f"  -> Lượt {turn_idx + 1}: Gửi tin nhắn: '{turn['message']}'" + (f" (Lần thử {retry + 1}/{max_retries})" if retry > 0 else ""))
                try:
                    response = requests.post(API_URL, json=payload, headers=headers, timeout=30)
                except requests.exceptions.RequestException as e:
                    print(f"  [ERROR] Lỗi kết nối HTTP: {e}")
                    turns_results.append({
                        "turnIndex": turn_idx,
                        "verdict": "HTTP_ERROR",
                        "reason": str(e),
                        "response": None
                    })
                    scenario_passed = False
                    break

                if response.status_code == 429:
                    print(f"  [429] Bị rate limit từ Controller, tự động thử lại sau 15s...")
                    time.sleep(15)
                    continue

                if response.status_code != 200:
                    print(f"  [HTTP {response.status_code}] Lỗi hệ thống: {response.text}")
                    turns_results.append({
                        "turnIndex": turn_idx,
                        "verdict": "HTTP_ERROR",
                        "reason": f"Status code: {response.status_code}",
                        "response": response.text
                    })
                    scenario_passed = False
                    break

                response_json = response.json()
                result_data = response_json.get("result", {})

                # Kiểm tra xem có bị rate limit từ phía Groq (khiến backend trả về unknown kèm CSKH fallback) hay không
                if result_data.get("intent") == "unknown" and "Hotline: 1900" in result_data.get("message", ""):
                    print(f"  [Groq 429] Bị rate limit từ Groq API (fallback CSKH), tự động thử lại sau 15s...")
                    time.sleep(15)
                    continue

                # Nếu phản hồi hợp lệ không bị rate limit
                break
            else:
                # Vượt quá số lần thử lại
                print("  [FAIL] Vượt quá số lần retry do rate limit.")
                turns_results.append({
                    "turnIndex": turn_idx,
                    "verdict": "RATE_LIMITED",
                    "reason": "Vượt quá số lần retry do rate limit",
                    "response": result_data
                })
                scenario_passed = False
                break

            if not scenario_passed:
                break

            # Đánh giá Assertion
            is_passed, reason = check_assertion(turn["expect"], result_data)
            print(f"  <- Kết quả lượt {turn_idx + 1}: {reason}")

            turns_results.append({
                "turnIndex": turn_idx,
                "verdict": "PASS" if is_passed else "FAIL",
                "reason": reason,
                "response": result_data
            })

            if not is_passed:
                scenario_passed = False

            # Cập nhật lịch sử cho lượt tiếp theo
            history.append({
                "role": "user",
                "content": turn["message"]
            })
            history.append({
                "role": "assistant",
                "content": result_data.get("message", "")
            })

        results.append({
            "id": sc_id,
            "description": description,
            "passed": scenario_passed,
            "turns": turns_results
        })

    # Ghi file latest snapshot
    with open(LATEST_SNAPSHOT, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print("\n=== HOÀN TẤT KIỂM THỬ ===")
    print(f"Kết quả snapshot thô đã được ghi vào {LATEST_SNAPSHOT}")


if __name__ == "__main__":
    main()
