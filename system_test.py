import json
import subprocess
import time
import urllib.parse

test_data = [
    {"userId": 1, "contactName": "张三", "bankName": "工商银行", "accountNo": "123456", "phone": "13800000001"},
    {"userId": 1, "contactName": "张伟", "bankName": "建设银行", "accountNo": "123457", "phone": "13800000002"},
    {"userId": 1, "contactName": "李四", "bankName": "中国银行", "accountNo": "123458", "phone": "13800000003"},
    {"userId": 1, "contactName": "王五", "bankName": "农业银行", "accountNo": "123459", "phone": "13800000004"},
    {"userId": 1, "contactName": "刘德华", "bankName": "招商银行", "accountNo": "123450", "phone": "13800000005"},
    {"userId": 1, "contactName": "周杰伦", "bankName": "交通银行", "accountNo": "123451", "phone": "13800000006"},
    {"userId": 1, "contactName": "张学友", "bankName": "平安银行", "accountNo": "123452", "phone": "13800000007"},
    {"userId": 2, "contactName": "张三", "bankName": "工商银行", "accountNo": "123456", "phone": "13800000001"}
]

print("1. Generating test data...")
for item in test_data:
    cmd = [
        "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
        "-X", "POST", "http://localhost:8080/contacts",
        "-H", "Content-Type: application/json",
        "-d", json.dumps(item)
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    print(f"Inserted: {item['contactName']} -> Status: {res.stdout.strip()}")
    time.sleep(0.5)

print("Waiting 5 seconds for Kafka events to hit Elasticsearch...")
time.sleep(5)

def test_api(name, path, params):
    query_string = urllib.parse.urlencode(params)
    api_url = f"http://localhost:8080/contacts/{path}?{query_string}"
    cmd = ["curl", "-s", "-H", "Content-Type: application/json", api_url]
    res = subprocess.run(cmd, capture_output=True, text=True)
    try:
        data = json.loads(res.stdout)
        return {"name": name, "url": api_url, "status": "SUCCESS", "results": data}
    except Exception as e:
        return {"name": name, "url": api_url, "status": "FAILED", "error": str(e), "stdout": res.stdout}

test_cases = [
    ("Chinese Search (张)", "search", {"userId": 1, "keyword": "张"}),
    ("Full Pinyin (zhangsan)", "search", {"userId": 1, "keyword": "zhangsan"}),
    ("Pinyin Initials (zs)", "search", {"userId": 1, "keyword": "zs"}),
    ("Pinyin Prefix (zha)", "search", {"userId": 1, "keyword": "zha"}),
    ("Mixed Input (zh张)", "search", {"userId": 1, "keyword": "zh张"}),
    ("Fuzziness (zagn)", "search", {"userId": 1, "keyword": "zagn"}),
    ("Suggest Prefix (zh)", "suggest", {"userId": 1, "prefix": "zh"}),
]

print("\n2. Running API tests...")
report = []
markdown_report = "# System Test Report - Pinyin Search Feature\n\n"
markdown_report += "## Test Environment\n- **Microservices Built**: MySQL, Redis, Kafka, Elasticsearch 8.10, Spring Boot app\n\n"
markdown_report += "## Test Cases Executed\n"

for name, path, params in test_cases:
    res = test_api(name, path, params)
    report.append(res)
    
    if res["status"] == "SUCCESS":
        result_len = len(res['results'])
        print(f"[{name}] -> Success, returned {result_len} items.")
        markdown_report += f"### ✅ {name}\n"
        markdown_report += f"- **Endpoint**: `{res['url']}`\n"
        markdown_report += f"- **Matched Elements**: {result_len}\n\n"
        markdown_report += "```json\n" + json.dumps(res['results'], ensure_ascii=False, indent=2) + "\n```\n\n"
    else:
        print(f"[{name}] -> Failed: {res.get('error')}")
        markdown_report += f"### ❌ {name}\n"
        markdown_report += f"- **Error**: {res.get('error')}\n"
        markdown_report += f"- **Output**: {res.get('stdout')}\n\n"

with open("/Users/xiaolulu/.gemini/antigravity/brain/4cc3f4ae-c2ba-4301-a21f-a90ce9ebe59c/test_report.md", "w", encoding='utf-8') as f:
    f.write(markdown_report)

print("Saved test_report.md successfully.")
