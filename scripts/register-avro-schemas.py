import json
import os
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


REGISTRY_URL = os.getenv("SCHEMA_REGISTRY_URL", "http://localhost:8085").rstrip("/")
SCHEMA_DIR = Path(os.getenv("SCHEMA_DIR", "docs/avro"))
SCHEMAS = {
    "product-viewed-value": "product-viewed.avsc",
    "cart-added-value": "cart-added.avsc",
    "order-completed-value": "order-completed.avsc",
}


def wait_until_ready(attempts: int = 60, delay_seconds: int = 2) -> None:
    for attempt in range(1, attempts + 1):
        try:
            with urlopen(f"{REGISTRY_URL}/subjects", timeout=5) as response:
                if response.status == 200:
                    return
        except (HTTPError, URLError, TimeoutError):
            pass

        print(f"Schema Registry 기동 대기 중... ({attempt}/{attempts})", flush=True)
        time.sleep(delay_seconds)

    raise RuntimeError(f"Schema Registry에 연결할 수 없습니다: {REGISTRY_URL}")


def register(subject: str, schema_path: Path) -> int:
    schema = schema_path.read_text(encoding="utf-8")
    payload = json.dumps(
        {"schemaType": "AVRO", "schema": schema},
        ensure_ascii=False,
    ).encode("utf-8")
    endpoint = f"{REGISTRY_URL}/subjects/{quote(subject, safe='')}/versions"
    request = Request(
        endpoint,
        data=payload,
        headers={"Content-Type": "application/vnd.schemaregistry.v1+json"},
        method="POST",
    )

    try:
        with urlopen(request, timeout=10) as response:
            result = json.load(response)
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{subject} 등록 실패 ({error.code}): {detail}") from error

    return result["id"]


def main() -> None:
    missing = [
        str(SCHEMA_DIR / filename)
        for filename in SCHEMAS.values()
        if not (SCHEMA_DIR / filename).is_file()
    ]
    if missing:
        raise FileNotFoundError(
            f"Avro 스키마 파일을 찾을 수 없습니다: {', '.join(missing)}"
        )

    wait_until_ready()
    for subject, filename in SCHEMAS.items():
        schema_id = register(subject, SCHEMA_DIR / filename)
        print(f"등록 완료: {subject} (schema id: {schema_id})", flush=True)


if __name__ == "__main__":
    main()
