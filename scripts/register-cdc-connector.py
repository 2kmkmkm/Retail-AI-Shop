import json
import os
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


CONNECT_URL = os.getenv("KAFKA_CONNECT_URL", "http://localhost:8083").rstrip("/")
CONNECTOR_FILE = Path(os.getenv("CONNECTOR_FILE", "docs/cdc/pos-inventory-connector.json"))


def wait_until_ready(attempts: int = 60, delay_seconds: int = 2) -> None:
    for attempt in range(1, attempts + 1):
        try:
            with urlopen(f"{CONNECT_URL}/connectors", timeout=5) as response:
                if response.status == 200:
                    return
        except (HTTPError, URLError, TimeoutError):
            pass

        print(f"Kafka Connect 기동 대기 중... ({attempt}/{attempts})", flush=True)
        time.sleep(delay_seconds)

    raise RuntimeError(f"Kafka Connect에 연결할 수 없습니다: {CONNECT_URL}")


def register(name: str, config: dict) -> str:
    # PUT /connectors/{name}/config 는 없으면 생성하고 있으면 갱신하므로
    # 컨테이너를 다시 띄워도 같은 명령이 안전하게 반복된다.
    payload = json.dumps(config, ensure_ascii=False).encode("utf-8")
    request = Request(
        f"{CONNECT_URL}/connectors/{name}/config",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="PUT",
    )

    try:
        with urlopen(request, timeout=15) as response:
            result = json.load(response)
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{name} 등록 실패 ({error.code}): {detail}") from error

    return result.get("name", name)


def wait_until_running(name: str, attempts: int = 30, delay_seconds: int = 2) -> str:
    for attempt in range(1, attempts + 1):
        try:
            with urlopen(f"{CONNECT_URL}/connectors/{name}/status", timeout=5) as response:
                status = json.load(response)
                state = status.get("connector", {}).get("state")
                if state == "RUNNING":
                    return state
                if state == "FAILED":
                    trace = status.get("connector", {}).get("trace", "")
                    raise RuntimeError(f"{name} 커넥터가 FAILED 상태입니다: {trace}")
        except (HTTPError, URLError, TimeoutError):
            pass

        print(f"커넥터 RUNNING 대기 중... ({attempt}/{attempts})", flush=True)
        time.sleep(delay_seconds)

    raise RuntimeError(f"{name} 커넥터가 RUNNING 상태가 되지 않았습니다")


def main() -> None:
    if not CONNECTOR_FILE.is_file():
        raise FileNotFoundError(f"커넥터 설정 파일을 찾을 수 없습니다: {CONNECTOR_FILE}")

    definition = json.loads(CONNECTOR_FILE.read_text(encoding="utf-8"))
    name = definition["name"]

    wait_until_ready()
    register(name, definition["config"])
    state = wait_until_running(name)
    print(f"등록 완료: {name} ({state})", flush=True)


if __name__ == "__main__":
    main()
