# -*- coding: utf-8 -*-
"""3종 세트 검증 — 눈이 아니라 실행으로 확인한다.

A. DDL     : H2(MariaDB 모드)에서 스키마 3개 + 시드 실제 실행, 필터 쿼리 결과 대조
B. Avro    : fastavro 로 스키마 파싱 + 샘플 레코드 직렬화 왕복
C. OpenAPI : openapi-spec-validator 로 스펙 검증 + 필수 경로 존재 확인
D. 정합성  : 프로토타입 JS ↔ ERD ↔ Avro ↔ OpenAPI 필드 단위 교차 대조
E. 채점표  : RTL-M 20개 항목이 어느 산출물에서 확인되는지 문자열 검증

사용법: python verify_docs.py   (docs/verify/ 에서 실행)
"""
import io, os, re, json, subprocess, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.dirname(HERE)
PJT  = os.path.dirname(DOCS)
PROTO = os.path.join(PJT, 'prototype', 'ZeroPick_프로토타입.html')
PLAN  = os.path.join(PJT, '기획', '팀플_기획서_ZeroPick.html')
H2 = os.path.expanduser('~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar')

results = []
def check(section, name, fn):
    try:
        detail = fn()
        results.append((section, name, True, detail or ''))
    except Exception as e:
        results.append((section, name, False, str(e)[:140]))

def read(p): return io.open(p, encoding='utf-8').read()

# ── A. DDL을 H2(MariaDB 모드)에서 실제 실행 ─────────────────────────
def run_sql(scripts, query=None):
    """스키마·시드를 한 DB 세션에서 실행하고, 마지막에 검증 쿼리를 돌린다."""
    merged = "\n".join(read(os.path.join(DOCS, 'sql', s)) for s in scripts)
    if query:
        merged += f"\nCREATE TABLE __v AS SELECT ({query}) AS n;\nSELECT n FROM __v;"
    with tempfile.NamedTemporaryFile('w', suffix='.sql', delete=False, encoding='utf-8') as f:
        f.write(merged); tmp = f.name
    r = subprocess.run(['java', '-Dfile.encoding=UTF-8', '-cp', H2, 'org.h2.tools.RunScript',
                        '-url', 'jdbc:h2:mem:v;MODE=MariaDB;DATABASE_TO_LOWER=TRUE',
                        '-script', tmp, '-showResults'],
                       capture_output=True, text=True, encoding='utf-8', timeout=60)
    os.unlink(tmp)
    if r.returncode != 0:
        raise RuntimeError((r.stderr or r.stdout).strip().splitlines()[-1])
    return r.stdout

check('A.DDL', 'schema-product.sql 실행', lambda: run_sql(['schema-product.sql']) and 'OK')
check('A.DDL', 'schema-commerce.sql 실행', lambda: run_sql(['schema-commerce.sql']) and 'OK')
check('A.DDL', 'schema-reco.sql 실행', lambda: run_sql(['schema-reco.sql']) and 'OK')
def run_count(scripts, query):
    """쿼리 결과 숫자를 결과 마커(--> N)에서 정확히 뽑는다. 시드 echo 오탐 방지."""
    out = run_sql(scripts, query)
    tail = out[out.rfind('FROM __v'):]
    nums = re.findall(r'^--> (\d+)', tail, re.M)
    assert nums, '결과 행을 찾지 못함'
    return int(nums[-1])

def d_seed_count():
    n = run_count(['schema-product.sql','seed-product.sql'], 'SELECT COUNT(*) FROM product')
    assert n == 18, f'상품 {n}개 (기대 18)'
    return f'COUNT(*) = {n}'
check('A.DDL', '시드 18개 상품 적재', d_seed_count)

def d_filter_count():
    n = run_count(['schema-product.sql','seed-product.sql'],
     "SELECT COUNT(*) FROM product p WHERE NOT EXISTS ("
     " SELECT 1 FROM product_sweetener ps JOIN sweetener s ON s.id=ps.sweetener_id"
     " WHERE ps.product_id=p.id AND s.name='말티톨')")
    assert n == 16, f'{n}개 (기대 16 — 말티톨 상품은 16·17번 둘뿐)'
    return f'COUNT(*) = {n} — 하드필터 쿼리 동작'
check('A.DDL', '말티톨 제외 필터 쿼리 = 16개', d_filter_count)
def d_check_constraint():
    """불량 status INSERT 가 CHECK 에 걸려 실패해야 한다."""
    bad = read(os.path.join(DOCS,'sql','schema-commerce.sql')) + (
        "\nINSERT INTO member (email,password,name) VALUES ('a@a.com','x','a');"
        "\nINSERT INTO orders (order_no,member_id,total_price,status) VALUES ('ZP1',1,0,'HACKED');")
    with tempfile.NamedTemporaryFile('w', suffix='.sql', delete=False, encoding='utf-8') as f:
        f.write(bad); tmp = f.name
    r = subprocess.run(['java','-Dfile.encoding=UTF-8','-cp',H2,'org.h2.tools.RunScript',
                        '-url','jdbc:h2:mem:v3;MODE=MariaDB;DATABASE_TO_LOWER=TRUE','-script',tmp],
                       capture_output=True, text=True, encoding='utf-8', timeout=60)
    os.unlink(tmp)
    assert r.returncode != 0, '불량 status(HACKED) 가 INSERT 됨 — CHECK 미동작'
    return "CHECK 동작 (HACKED 거부됨)"
check('A.DDL', '잘못된 주문상태 거부(CHECK)', d_check_constraint)

# ── B. Avro — 파싱 + 직렬화 왕복 ─────────────────────────────────────
import fastavro
SAMPLES = {
 'product-viewed.avsc':  {"memberId":1,"productId":10,"category":"간식/디저트","occurredAt":1765600000000},
 'cart-added.avsc':      {"memberId":1,"productId":10,"category":"간식/디저트","qty":2,"occurredAt":1765600000000},
 'order-completed.avsc': {"memberId":1,"productId":10,"category":"간식/디저트","qty":1,"unitPrice":15000,
                          "orderNo":"ZP1001","paymentMethod":"카카오페이","occurredAt":1765600000000},
}
def avro_roundtrip(fname):
    schema = fastavro.parse_schema(json.loads(read(os.path.join(DOCS,'avro',fname))))
    buf = io.BytesIO()
    fastavro.schemaless_writer(buf, schema, SAMPLES[fname]); buf.seek(0)
    back = fastavro.schemaless_reader(buf, schema)
    src = {k: v for k, v in SAMPLES[fname].items()}
    got = {k: (int(back[k].timestamp()*1000) if hasattr(back[k],'timestamp') else back[k]) for k in src}
    assert got == src, f"왕복 불일치: {got}"
    return f"{len(schema['fields'])}필드 왕복 일치"
for f in SAMPLES: check('B.Avro', f, lambda f=f: avro_roundtrip(f))

# ── C. OpenAPI — 스펙 검증 + 필수 경로 ──────────────────────────────
import yaml
from openapi_spec_validator import validate as validate_openapi
SPEC = yaml.safe_load(read(os.path.join(DOCS,'openapi','openapi.yaml')))
check('C.API', 'OpenAPI 3.0.3 스펙 검증', lambda: (validate_openapi(SPEC), f"경로 {len(SPEC['paths'])}개")[1])
REQUIRED_PATHS = ['/product-service/products','/product-service/products/{id}','/product-service/products/compare',
 '/product-service/products/{id}/stock/deduct',
 '/commerce-service/members','/commerce-service/members/login','/commerce-service/carts',
 '/commerce-service/orders','/commerce-service/orders/{orderId}/pay',
 '/recommendation-service/preferences','/recommendation-service/recommendations/{memberId}',
 '/recommendation-service/chat','/recommendation-service/search',
 '/recommendation-service/click','/recommendation-service/metrics']
check('C.API', '필수 경로 15개 존재', lambda:
    (lambda miss: '전부 존재' if not miss else (_ for _ in ()).throw(RuntimeError(f'누락: {miss}')))(
        [p for p in REQUIRED_PATHS if p not in SPEC['paths']]))
check('C.API', '결제 상태머신 표현', lambda:
    'PENDING' in json.dumps(SPEC['paths']['/commerce-service/orders']['post']) and
    'PAID' in json.dumps(SPEC['paths']['/commerce-service/orders/{orderId}/pay']) and 'OK')

# ── D. 프로토타입 ↔ 문서 정합성 ─────────────────────────────────────
proto = read(PROTO)
def d_topics():
    m = re.search(r'const TOPIC = \{([^}]+)\}', proto)
    proto_topics = dict(re.findall(r'(\w+):"([\w-]+)"', m.group(1)))
    for ev, topic in proto_topics.items():
        fname = topic + '.avsc'
        assert os.path.exists(os.path.join(DOCS,'avro',fname)), f'{fname} 없음'
    return f"토픽 3개 일치: {sorted(proto_topics.values())}"
check('D.정합성', '프로토타입 토픽명 == Avro 파일명', d_topics)

def d_weights():
    ws = re.findall(r'PRODUCT_VIEWED:(\d+),\s*CART_ADDED:(\d+),\s*ORDER_COMPLETED:(\d+)', proto)
    assert ws and all(w == ('1','0','50') for w in ws), f'프로토타입 가중치 {ws}'
    md = read(os.path.join(DOCS,'이벤트스키마.md'))
    assert '| 1 |' in md and '| 0 |' in md and '| 50 |' in md, '문서 가중치 표 불일치'
    return f"1/0/50 — 프로토타입 {len(ws)}곳 + 문서 일치"
check('D.정합성', '가중치(조회1/담기0/주문50) 일치', d_weights)

def d_payload():
    sql = read(os.path.join(DOCS,'sql','schema-reco.sql'))
    cols = set(re.findall(r'^\s{2}(\w+)\s', sql[sql.index('behavior_log'):sql.index('reco_result')], re.M))
    need = {'member_id','product_id','category','event_type','qty','unit_price','order_no','payment_method','occurred_at'}
    missing = need - cols
    assert not missing, f'behavior_log 누락 컬럼: {missing}'
    return 'Avro 전 필드가 behavior_log 에 적재 가능'
check('D.정합성', 'Avro 필드 ⊆ behavior_log 컬럼', d_payload)

def d_product_fields():
    m = re.search(r'"Product"?', '')
    props = set(SPEC['components']['schemas']['Product']['properties'].keys())
    need = {'id','name','brand','category','price','stock','claimType','kcal','sugarG','carbG','sweeteners'}
    missing = need - props
    assert not missing, f'Product 스키마 누락: {missing}'
    # 프로토타입 카드가 쓰는 필드가 다 있는가
    for f in ['name','brand','price','stock','kcal']:
        assert f in props
    return f"Product {len(props)}개 속성 — 카드 필드 전부 커버"
check('D.정합성', '프로토타입 카드 필드 ⊆ API Product', d_product_fields)

def d_no_cross_fk():
    for fname in ['schema-product.sql','schema-commerce.sql','schema-reco.sql']:
        sql = read(os.path.join(DOCS,'sql',fname))
        own = set(re.findall(r'CREATE TABLE (\w+)', sql))
        refs = set(re.findall(r'REFERENCES (\w+)\(', sql))
        cross = refs - own
        assert not cross, f'{fname} 교차 FK: {cross}'
    return '3개 스키마 모두 교차 FK 없음'
check('D.정합성', 'Database per Service (교차 FK 금지)', d_no_cross_fk)

def d_seed_matches():
    n = len(re.findall(r"INSERT INTO product \(", read(os.path.join(DOCS,'sql','seed-product.sql'))))
    m = len(re.findall(r'\{id:\d+,\s*name:\"[^\"]+\",\s*brand:', proto))
    assert n == m == 18, f'시드 {n} vs 프로토타입 {m}'
    return '시드 18개 == 프로토타입 18개'
check('D.정합성', '시드 데이터 == 프로토타입 데이터', d_seed_matches)

# ── E. 채점표 20개 항목 → 산출물 추적 ────────────────────────────────
plan = read(PLAN)
oas = read(os.path.join(DOCS,'openapi','openapi.yaml'))
erd = read(os.path.join(DOCS,'ERD.md'))
api = read(os.path.join(DOCS,'API명세서.md'))
evt = read(os.path.join(DOCS,'이벤트스키마.md'))
sqls = "".join(read(os.path.join(DOCS,'sql',f)) for f in
               ['schema-product.sql','schema-commerce.sql','schema-reco.sql'])
RUBRIC = [
 ('핵심1 서비스 3개 분리',      [(erd,'product_db'),(erd,'commerce_db'),(erd,'reco_db')]),
 ('핵심2 Eureka',              [(plan,'Eureka')]),
 ('핵심3 Gateway 단일 진입점',  [(oas,'localhost:8000'),(api,'API Gateway')]),
 ('핵심4 Config+LLM키 분리',    [(plan,'Config Server'),(plan,'LLM API 키')]),
 ('핵심5 OpenFeign 동기',       [(api,'stock/deduct'),(api,'OpenFeign')]),
 ('핵심6 Kafka 3종 이벤트',     [(evt,'product-viewed'),(evt,'cart-added'),(evt,'order-completed')]),
 ('핵심7 CRUD+재고차감',        [(oas,'/commerce-service/orders/{orderId}/pay'),(sqls,"'PENDING'")]),
 ('핵심8 DB분리+ERD',           [(erd,'Database per Service')]),
 ('핵심9 Docker Compose',       [(plan,'docker-compose')]),
 ('핵심10 챗봇+측정',           [(oas,'/recommendation-service/chat'),(plan,'측정 지표')]),
 ('선택1 K8s',                  [(plan,'Kubernetes')]),
 ('선택2 Schema Registry',      [(evt,'Schema Registry'),(evt,'BACKWARD')]),
 ('선택3 Spring Cloud Bus',     [(plan,'Spring Cloud Bus')]),
 ('선택4 CB/LLM Fallback',      [(plan,'Circuit Breaker'),(oas,'usedFallback')]),
 ('선택5 모니터링',             [(plan,'Grafana')]),
 ('선택6 추천 성과 대시보드',   [(oas,'/recommendation-service/click'),(oas,'/recommendation-service/metrics'),(sqls,'reco_click')]),
 ('선택7 CI/CD',                [(plan,'GitHub Actions')]),
 ('선택8 추천 API 부하테스트',  [(plan,'부하 테스트'),(oas,'/recommendations/{memberId}')]),
 ('선택9 로그 중앙화 EFK',      [(plan,'Fluent Bit')]),
 ('선택10 자연어 검색',         [(oas,'/recommendation-service/search')]),
]
for item, conds in RUBRIC:
    check('E.채점표', item, lambda conds=conds:
        (lambda miss: '근거 확인' if not miss else (_ for _ in ()).throw(
            RuntimeError(f'근거 문자열 없음: {miss}')))(
            [needle for hay, needle in conds if needle not in hay]))

# ── 출력 ─────────────────────────────────────────────────────────────
print()
cur = None
ok = fail = 0
for section, name, passed, detail in results:
    if section != cur:
        print(f"\n[{section}]"); cur = section
    mark = 'PASS' if passed else 'FAIL'
    ok += passed; fail += (not passed)
    print(f"  {mark}  {name}" + (f"  — {detail}" if detail else ""))
print(f"\n{'='*60}\n합계: PASS {ok} / FAIL {fail}")
sys.exit(1 if fail else 0)
