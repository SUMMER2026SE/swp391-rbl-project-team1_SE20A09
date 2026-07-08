import sys
import json
import shutil
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
LATEST_SNAPSHOT = EVAL_DIR / "snapshots" / "latest.json"
BASELINE_SNAPSHOT = EVAL_DIR / "snapshots" / "baseline.json"


def promote_latest():
    if not LATEST_SNAPSHOT.exists():
        print(f"Error: {LATEST_SNAPSHOT} does not exist. Please run python run_eval.py first.")
        sys.exit(1)

    shutil.copy(LATEST_SNAPSHOT, BASELINE_SNAPSHOT)
    print(f"Success: Promoted {LATEST_SNAPSHOT} to {BASELINE_SNAPSHOT} as the new baseline.")


def compare_snapshots():
    if not LATEST_SNAPSHOT.exists():
        print(f"Error: {LATEST_SNAPSHOT} does not exist. Please run python run_eval.py first.")
        sys.exit(1)

    if not BASELINE_SNAPSHOT.exists():
        print("Warning: baseline.json does not exist yet.")
        print("Please review the latest results in snapshots/latest.json manually.")
        print("If the results are correct and satisfactory, run:")
        print("  python report_eval.py --promote")
        print("to establish the baseline snapshot.")
        sys.exit(0)

    with open(LATEST_SNAPSHOT, "r", encoding="utf-8") as f:
        latest = json.load(f)

    with open(BASELINE_SNAPSHOT, "r", encoding="utf-8") as f:
        baseline = json.load(f)

    latest_map = {item["id"]: item for item in latest}
    baseline_map = {item["id"]: item for item in baseline}

    regressions = []
    improvements = []
    total_passed = 0
    total_scenarios = len(latest)

    print("\n=== BÁO CÁO SO SÁNH EVALUATION ===")

    for sc_id, latest_sc in latest_map.items():
        if latest_sc["passed"]:
            total_passed += 1

        if sc_id not in baseline_map:
            print(f"[NEW] Scenario mới thêm vào: {sc_id} (Verdict: {'PASS' if latest_sc['passed'] else 'FAIL'})")
            continue

        baseline_sc = baseline_map[sc_id]

        # Kiểm tra thay đổi trạng thái
        if baseline_sc["passed"] and not latest_sc["passed"]:
            # Có sự giảm sút chất lượng (Regression)
            regressions.append(latest_sc)
            print(f"[REGRESSION] ❌ Scenario '{sc_id}' bị hỏng (Trước: PASS, Hiện tại: FAIL)")
            # In chi tiết lỗi của lượt hỏng
            for idx, (b_turn, l_turn) in enumerate(zip(baseline_sc["turns"], latest_sc["turns"])):
                if b_turn["verdict"] == "PASS" and l_turn["verdict"] != "PASS":
                    print(f"  - Lượt {idx + 1} hỏng. Lý do: {l_turn['reason']}")
                    if l_turn["response"]:
                        print(f"  - Raw Response: {json.dumps(l_turn['response'], ensure_ascii=False)}")
        elif not baseline_sc["passed"] and latest_sc["passed"]:
            # Cải thiện chất lượng
            improvements.append(latest_sc)
            print(f"[IMPROVEMENT]  Scenario '{sc_id}' đã hoạt động chính xác (Trước: FAIL, Hiện tại: PASS)")

    print("\n=== TỔNG KẾT ===")
    print(f"Đã pass: {total_passed} / {total_scenarios} scenarios.")
    print(f"Số lỗi hồi quy (Regressions): {len(regressions)}")
    print(f"Số kịch bản cải thiện (Improvements): {len(improvements)}")

    if regressions:
        print("\n❌ CẢNH BÁO: Phát hiện lỗi hồi quy! Vui lòng kiểm tra lại prompt hoặc code backend.")
        sys.exit(1)
    else:
        print("\n  HOÀN THÀNH: Không phát hiện lỗi hồi quy nào so với baseline.")
        sys.exit(0)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "--promote":
        promote_latest()
    else:
        compare_snapshots()


if __name__ == "__main__":
    main()
