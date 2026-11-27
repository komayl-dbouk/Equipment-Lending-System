"""ELS-36: real HTTP and independent PostgreSQL oracles; Python standard library only."""
import argparse
import base64
import concurrent.futures
import contextlib
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
STUDENT = {"id", "name", "email"}
EQUIPMENT = {"id", "inventoryCode", "name", "category", "description", "available"}
LOAN = {"id", "studentId", "equipmentId", "borrowingDate", "dueDate", "returnDate"}
ERROR = {"timestamp", "status", "code", "message", "path", "fieldErrors"}
MAX_ID = 9223372036854775807
FIXED_INSTANT = "2030-06-15T09:00:00Z"


class Workflow:
    def __init__(self, args):
        self.args = args
        self.requests = []
        self.assertions = 0
        self.races = []
        self.database_checks = []
        self.db = self.parse_target(os.environ["ELS_DB_URL"])
        test_db = self.parse_target(os.environ["ELS_TEST_DB_URL"])
        self.check(self.db.path != test_db.path, "live and integration databases differ")
        self.check(os.environ["ELS_DB_USERNAME"] != os.environ["ELS_TEST_DB_USERNAME"],
                   "live and integration credentials use separate users")
        for name in ("ELS_STAFF_USERNAME", "ELS_STAFF_PASSWORD"):
            if not os.environ.get(name):
                raise ValueError(f"Required private environment variable missing: {name}")
        staff = f'{os.environ["ELS_STAFF_USERNAME"]}:{os.environ["ELS_STAFF_PASSWORD"]}'.encode("utf-8")
        self.authorization = "Basic " + base64.b64encode(staff).decode("ascii")
        invalid = b"invalid-els46-staff:invalid-els46-credential"
        self.invalid_authorization = "Basic " + base64.b64encode(invalid).decode("ascii")
        self.pg_env = dict(os.environ, PGPASSWORD=os.environ["ELS_DB_PASSWORD"], PGCONNECT_TIMEOUT="5")
        self.psql = [args.psql, "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
                     "-h", self.db.hostname, "-p", str(self.db.port or 5432),
                     "-U", os.environ["ELS_DB_USERNAME"], "-d", self.db.path[1:]]
        self.identity = self.sql("SELECT json_build_object('database',current_database(),'user',current_user,"
                                 "'postgresql',current_setting('server_version'))")
        self.check(self.identity["database"] == args.expected_database == self.db.path[1:], "effective live target matches")
        self.check(self.identity["user"] == os.environ["ELS_DB_USERNAME"], "effective live role matches")
        self.check(self.identity["postgresql"] == "17.5", "pinned PostgreSQL 17.5")

    @staticmethod
    def parse_target(value):
        if not value.startswith("jdbc:postgresql://"):
            raise ValueError("Expected an explicit PostgreSQL JDBC URL")
        url = urllib.parse.urlsplit(value[5:])
        if (url.hostname not in ("localhost", "127.0.0.1", "::1") or url.query or url.fragment
                or url.username or not re.fullmatch(r"/[A-Za-z0-9_]+_test", url.path)):
            raise ValueError("Validation requires a loopback disposable _test database without URL overrides")
        return url

    def check(self, condition, message):
        self.assertions += 1
        if not condition:
            raise AssertionError(message)

    def sql(self, query):
        result = subprocess.run(self.psql + ["-c", query], env=self.pg_env, capture_output=True,
                                text=True, check=False, timeout=15)
        if result.returncode:
            raise RuntimeError("PostgreSQL oracle failed; inspect the local database log")
        return json.loads(result.stdout.strip())

    def snapshot(self):
        return self.sql("SELECT json_build_object("
                        "'students',COALESCE((SELECT json_agg(s ORDER BY id) FROM student s),'[]'::json),"
                        "'equipment',COALESCE((SELECT json_agg(e ORDER BY id) FROM equipment e),'[]'::json),"
                        "'loans',COALESCE((SELECT json_agg(l ORDER BY id) FROM loan l),'[]'::json))")

    def empty_preflight(self):
        tables = self.sql("SELECT COALESCE(json_agg(table_name),'[]'::json) FROM information_schema.tables "
                          "WHERE table_schema='public' AND table_name IN ('student','equipment','loan')")
        if tables:
            self.check(len(tables) == 3, "existing schema is complete")
            self.check(all(not rows for rows in self.snapshot().values()), "workflow database starts empty")

    def call(self, method, path, payload=None, expected=200, code=None, authentication="valid"):
        data = None if payload is None else json.dumps(payload).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        if authentication == "valid":
            headers["Authorization"] = self.authorization
        elif authentication == "invalid":
            headers["Authorization"] = self.invalid_authorization
        request = urllib.request.Request(self.args.base_url + path, data=data, method=method,
                                         headers=headers)
        try:
            response = urllib.request.urlopen(request, timeout=25)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read()
        body = json.loads(raw) if raw else None
        entry = {"method": method, "path": path, "status": response.status, "expected": expected}
        if isinstance(body, dict) and "code" in body:
            entry["code"] = body["code"]
        self.requests.append(entry)
        self.check(response.status in expected if isinstance(expected, tuple) else response.status == expected,
                   f"{method} {path}: unexpected HTTP {response.status}")
        if response.status == 204:
            self.check(not raw, "204 response has no body")
        else:
            self.check(response.headers.get_content_type() == "application/json", "JSON response content type")
        if code:
            self.shape(body, ERROR)
            self.check(body["code"] == code and body["status"] == expected, "exact error status/code")
            self.check(body["path"] == path.split("?")[0], "error path")
            self.check(body["timestamp"] == FIXED_INSTANT, "separately loaded fixed Clock is active")
            self.check(isinstance(body["fieldErrors"], dict), "error fieldErrors object")
            self.check(not any(token in raw.decode() for token in ("SQLException", "stackTrace", "org.hibernate")), "sanitized error")
        return body, response.headers, response.status

    def shape(self, body, keys):
        self.check(isinstance(body, dict) and set(body) == keys, "exact DTO fields")
        if "id" in keys:
            self.check(type(body["id"]) is int and body["id"] > 0, "positive integral DTO id")

    def array(self, path, expected_ids=None, keys=LOAN):
        body = self.call("GET", path)[0]
        self.check(isinstance(body, list), "list response is an array")
        for item in body:
            self.shape(item, keys)
            if keys == LOAN:
                row = self.loan_row(item["id"])
                expected = {"id": row["id"], "studentId": row["student_id"], "equipmentId": row["equipment_id"],
                            "borrowingDate": row["borrowing_date"], "dueDate": row["due_date"], "returnDate": row["return_date"]}
            else:
                expected = self.sql("SELECT json_build_object('id',e.id,'inventoryCode',e.inventory_code,'name',e.name,"
                                    "'category',e.category,'description',e.description,'available',NOT EXISTS "
                                    f"(SELECT 1 FROM loan l WHERE l.equipment_id=e.id AND l.return_date IS NULL)) FROM equipment e WHERE e.id={int(item['id'])}")
            self.check(item == expected, "query DTO values match independent committed SQL state")
        ids = [item["id"] for item in body]
        self.check(ids == sorted(ids) and len(ids) == len(set(ids)), "unique ascending ID order")
        if expected_ids is not None:
            self.check(ids == sorted(expected_ids), "exact filtered result IDs")
        return body

    def reject(self, method, path, payload, status, code):
        before = self.snapshot()
        self.call(method, path, payload, expected=status, code=code)
        self.check(self.snapshot() == before, "rejected request leaves all committed rows unchanged")
        self.database_checks.append({"oracle": "rejected-write snapshot", "method": method, "path": path, "unchanged": True})

    def equipment(self, code):
        body = self.call("POST", "/api/equipment", {"inventoryCode": " " + code.lower() + " ",
                          "name": " Camera ", "category": " Camera ", "description": " Synthetic item "}, 201)[0]
        self.shape(body, EQUIPMENT)
        self.check(body == {"id": body["id"], "inventoryCode": code, "name": "Camera", "category": "Camera",
                            "description": "Synthetic item", "available": True}, "normalized equipment DTO")
        return body["id"]

    def borrow(self, student, item, due="2030-06-20", borrowed="2030-06-10", expected=201):
        body, _, status = self.call("POST", "/api/loans", {"studentId": student, "equipmentId": item,
                          "borrowingDate": borrowed, "dueDate": due}, expected)
        if status == 201:
            self.shape(body, LOAN)
            self.check(body == {"id": body["id"], "studentId": student, "equipmentId": item,
                                "borrowingDate": borrowed, "dueDate": due, "returnDate": None}, "exact borrowing DTO")
            self.check(self.loan_row(body["id"]) == {"id": body["id"], "student_id": student, "equipment_id": item,
                       "borrowing_date": borrowed, "due_date": due, "return_date": None}, "committed borrowing row")
        return body, status

    def loan_row(self, loan_id):
        return self.sql(f"SELECT row_to_json(l) FROM loan l WHERE id={int(loan_id)}")

    def loans_for(self, item):
        return self.sql(f"SELECT COALESCE(json_agg(l ORDER BY id),'[]'::json) FROM loan l WHERE equipment_id={int(item)}")

    @contextlib.contextmanager
    def equipment_lock(self, item):
        process = subprocess.Popen(self.psql, env=self.pg_env, stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        try:
            process.stdin.write(f"BEGIN; SELECT id FROM equipment WHERE id={int(item)} FOR UPDATE;\n\\echo ELS36_LOCKED\n")
            process.stdin.flush()
            # psql is bounded by its connection timeout; this handshake precedes request submission.
            ready = await_marker(process, "ELS36_LOCKED", timeout=10)
            self.check(ready, "independent equipment lock acquired")
            yield process
        finally:
            if process.poll() is None:
                try:
                    process.stdin.write("ROLLBACK;\n\\q\n")
                    process.stdin.flush()
                    process.wait(timeout=5)
                except (BrokenPipeError, subprocess.TimeoutExpired):
                    process.kill()
                    process.wait(timeout=5)
            for stream in (process.stdin, process.stdout, process.stderr):
                stream.close()

    def contend(self, label, item, one, two):
        gate = threading.Barrier(3, timeout=10)
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool, self.equipment_lock(item) as lock:
            def request(operation):
                gate.wait()
                return operation()
            first = pool.submit(request, one)
            second = pool.submit(request, two)
            gate.wait()
            deadline = time.monotonic() + 15
            while True:
                waiting = self.sql("SELECT to_json(count(*)) FROM pg_stat_activity WHERE datname=current_database() "
                                   "AND pid<>pg_backend_pid() AND wait_event_type='Lock' AND query ILIKE '%equipment%'")
                if waiting >= 2:
                    break
                if first.done() or second.done() or time.monotonic() >= deadline:
                    raise AssertionError("Both independent HTTP requests must wait for the equipment lock")
            lock.stdin.write("COMMIT;\n\\q\n")
            lock.stdin.flush()
            lock.wait(timeout=5)
            result = first.result(timeout=25), second.result(timeout=25)
        self.races.append({"race": label, "statuses": [r[2] for r in result], "observedEquipmentWaiters": waiting})
        return result

    def run(self):
        self.empty_preflight()
        before_authentication = self.snapshot()
        self.call("GET", "/api/equipment", expected=401, code="UNAUTHORIZED", authentication=None)
        self.call("GET", "/api/equipment", expected=401, code="UNAUTHORIZED", authentication="invalid")
        self.call("POST", "/api/students",
                  {"name": "ELS-46 Rejected Student", "email": "els46.rejected@example.test"},
                  expected=401, code="UNAUTHORIZED", authentication=None)
        self.check(self.snapshot() == before_authentication,
                   "unauthenticated mutation leaves all committed rows unchanged")
        self.database_checks.append({"oracle": "unauthenticated-write snapshot", "unchanged": True})
        for path in ("/api/equipment", "/api/equipment?available=true", "/api/equipment?available=false"):
            self.array(path, [], EQUIPMENT)
        self.array("/api/loans", [])
        self.array("/api/loans/overdue", [])
        student, headers, _ = self.call("POST", "/api/students", {"name": " ELS36 Student ", "email": " ONE@ELS36.EXAMPLE.TEST "}, 201)
        self.shape(student, STUDENT)
        sid = student["id"]
        self.check(student == {"id": sid, "name": "ELS36 Student", "email": "one@els36.example.test"}, "student normalization")
        self.check(urllib.parse.urlsplit(headers["Location"]).path == f"/api/students/{sid}", "student Location")
        self.check(self.call("GET", f"/api/students/{sid}")[0] == student, "retrieved student DTO")
        updated = self.call("PUT", f"/api/students/{sid}", {"name": " Updated Student ", "email": " UPDATED@ELS36.EXAMPLE.TEST "})[0]
        self.shape(updated, STUDENT)
        self.check(updated == {"id": sid, "name": "Updated Student", "email": "updated@els36.example.test"}, "replacement normalization")
        stored = self.sql(f"SELECT row_to_json(s) FROM student s WHERE id={sid}")
        self.check(stored == updated, "committed student update")
        second = self.call("POST", "/api/students", {"name": "Second Student", "email": "second@els36.example.test"}, 201)[0]
        self.shape(second, STUDENT)
        camera = self.equipment("ELS36-Z-CAMERA")
        unused = self.equipment("ELS36-A-UNUSED")
        changed = self.call("PUT", f"/api/equipment/{camera}", {"inventoryCode": " els36-z-camera ", "name": " Updated Camera ", "category": " Camera "})[0]
        self.shape(changed, EQUIPMENT)
        self.check(changed == {"id": camera, "inventoryCode": "ELS36-Z-CAMERA", "name": "Updated Camera", "category": "Camera", "description": None, "available": True}, "equipment replacement clears omitted description")
        self.check(self.sql(f"SELECT row_to_json(e) FROM equipment e WHERE id={camera}") == {
            "id": camera, "inventory_code": "ELS36-Z-CAMERA", "name": "Updated Camera", "category": "Camera", "description": None}, "committed equipment update")
        self.array("/api/equipment", [camera, unused], EQUIPMENT)
        self.call("DELETE", f"/api/equipment/{unused}", expected=204)
        self.check(self.sql(f"SELECT to_json(count(*)) FROM equipment WHERE id={unused}") == 0, "unreferenced equipment deleted")
        first, _ = self.borrow(sid, camera, "2030-06-14")
        first_id = first["id"]
        self.reject("POST", "/api/loans", {"studentId": sid, "equipmentId": camera, "borrowingDate": "2030-06-10", "dueDate": "2030-06-20"}, 409, "EQUIPMENT_UNAVAILABLE")
        today_item = self.equipment("ELS36-TODAY")
        tomorrow_item = self.equipment("ELS36-TOMORROW")
        self.borrow(second["id"], today_item, "2030-06-15")
        tomorrow, _ = self.borrow(sid, tomorrow_item, "2030-06-16")
        before_reads = self.snapshot()
        self.array("/api/equipment?available=true", [], EQUIPMENT)
        self.array("/api/equipment?available=false", [camera, today_item, tomorrow_item], EQUIPMENT)
        self.array("/api/loans/overdue", [first_id])
        self.array(f"/api/loans?studentId={sid}", [first_id, tomorrow["id"]])
        self.array(f"/api/loans?equipmentId={camera}", [first_id])
        self.array(f"/api/loans?studentId={sid}&equipmentId={camera}", [first_id])
        self.array(f"/api/loans?studentId={second['id']}&equipmentId={camera}", [])
        self.array(f"/api/loans?studentId={MAX_ID}", [])
        self.array(f"/api/loans?equipmentId={MAX_ID}", [])
        self.check(self.snapshot() == before_reads, "valid availability/history/overdue queries preserve state")
        self.database_checks.append({"oracle": "valid-query snapshot", "unchanged": True})
        self.invalid_requests(sid, camera, first_id)
        returned = self.call("POST", f"/api/loans/{first_id}/return", {"returnDate": "2030-06-10"})[0]
        self.shape(returned, LOAN)
        self.check(returned == dict(first, returnDate="2030-06-10"), "same-day return exact DTO")
        history = self.loan_row(first_id)
        self.check(history["return_date"] == "2030-06-10", "committed return date")
        self.reject("POST", f"/api/loans/{first_id}/return", {"returnDate": "2030-06-15"}, 409, "LOAN_ALREADY_RETURNED")
        self.array("/api/loans/overdue", [])
        self.array("/api/equipment?available=true", [camera], EQUIPMENT)
        self.reject("DELETE", f"/api/equipment/{camera}", None, 409, "EQUIPMENT_HAS_HISTORY")
        new, _ = self.borrow(sid, camera, "2030-06-20", "2030-06-15")
        self.check(new["id"] != first_id and self.loan_row(first_id) == history, "reborrow preserves previous loan")
        self.array(f"/api/loans?studentId={sid}&equipmentId={camera}", [first_id, new["id"]])
        self.reject("DELETE", f"/api/equipment/{camera}", None, 409, "EQUIPMENT_HAS_HISTORY")
        self.http_races(sid)
        self.check(self.sql("SELECT to_json(count(*)) FROM loan l LEFT JOIN student s ON s.id=l.student_id "
                            "LEFT JOIN equipment e ON e.id=l.equipment_id WHERE s.id IS NULL OR e.id IS NULL") == 0, "no orphan loans")
        self.check(self.sql("SELECT to_json(count(*)) FROM (SELECT equipment_id FROM loan WHERE return_date IS NULL "
                            "GROUP BY equipment_id HAVING count(*)>1) duplicates") == 0, "at most one active loan per equipment")
        self.database_checks.append({"oracle": "final committed workflow", "rows": self.snapshot(), "orphans": 0, "duplicateActiveEquipment": 0})

    def invalid_requests(self, sid, camera, loan_id):
        self.reject("POST", "/api/students", {"name": "Bad", "email": "new@els36.example.test", "id": 1}, 400, "VALIDATION_FAILED")
        self.reject("POST", "/api/students", {"name": 123, "email": "new@els36.example.test"}, 400, "VALIDATION_FAILED")
        self.reject("POST", "/api/students", {"name": "Duplicate", "email": " UPDATED@ELS36.EXAMPLE.TEST "}, 409, "DUPLICATE_EMAIL")
        self.reject("PUT", f"/api/students/{sid}", {"name": "Do not save", "email": "SECOND@ELS36.EXAMPLE.TEST"}, 409, "DUPLICATE_EMAIL")
        self.reject("POST", "/api/equipment", {"inventoryCode": " els36-z-camera ", "name": "Dup", "category": "Camera"}, 409, "DUPLICATE_INVENTORY_CODE")
        self.reject("PUT", f"/api/equipment/{camera}", {"inventoryCode": "CHANGED", "name": "Changed", "category": True}, 400, "VALIDATION_FAILED")
        base = {"studentId": sid, "equipmentId": camera, "borrowingDate": "2030-06-10", "dueDate": "2030-06-20"}
        for field, value, status, code in [("studentId", MAX_ID, 404, "STUDENT_NOT_FOUND"), ("equipmentId", MAX_ID, 404, "EQUIPMENT_NOT_FOUND"),
                  ("studentId", str(sid), 400, "VALIDATION_FAILED"), ("studentId", MAX_ID + 1, 400, "VALIDATION_FAILED"),
                  ("dueDate", "2030-06-10", 400, "INVALID_LOAN_DATES"), ("dueDate", "2030-02-30", 400, "VALIDATION_FAILED"),
                  ("borrowingDate", "+10000-06-10", 400, "VALIDATION_FAILED"), ("dueDate", "+10000000-06-20", 400, "VALIDATION_FAILED")]:
            self.reject("POST", "/api/loans", dict(base, **{field: value}), status, code)
        for value, code in [("2030-06-09", "INVALID_RETURN_DATE"), ("2030-02-29", "VALIDATION_FAILED"),
                            ("+10000000-01-01", "VALIDATION_FAILED"), ([2030, 6, 15], "VALIDATION_FAILED"), (None, "VALIDATION_FAILED")]:
            self.reject("POST", f"/api/loans/{loan_id}/return", {"returnDate": value}, 400, code)
        self.reject("POST", f"/api/loans/{loan_id}/return", {}, 400, "VALIDATION_FAILED")
        self.reject("POST", f"/api/loans/{loan_id}/return", {"returnDate": "2030-06-15", "dueDate": "2030-06-20"}, 400, "VALIDATION_FAILED")
        for invalid in ("0", "abc", str(MAX_ID + 1)):
            self.reject("POST", f"/api/loans/{invalid}/return", {"returnDate": "2030-06-15"}, 400, "VALIDATION_FAILED")
        self.reject("POST", f"/api/loans/{MAX_ID}/return", {"returnDate": "2030-06-15"}, 404, "LOAN_NOT_FOUND")
        for value in ("", "TRUE", "1"):
            self.reject("GET", "/api/equipment?available=" + value, None, 400, "VALIDATION_FAILED")
        for value in ("0", "abc", str(MAX_ID + 1), "%2B1"):
            for field in ("studentId", "equipmentId"):
                self.reject("GET", f"/api/loans?{field}={value}", None, 400, "VALIDATION_FAILED")

    def http_races(self, sid):
        item = self.equipment("ELS36-RACE-BORROW")
        body = {"studentId": sid, "equipmentId": item, "borrowingDate": "2030-06-15", "dueDate": "2030-06-20"}
        one, two = self.contend("borrow-borrow", item,
            lambda: self.call("POST", "/api/loans", body, (201, 409)), lambda: self.call("POST", "/api/loans", body, (201, 409)))
        self.check(sorted([one[2], two[2]]) == [201, 409], "one HTTP borrowing creation and one conflict")
        winner = next(result[0] for result in (one, two) if result[2] == 201)
        loser = next(result[0] for result in (one, two) if result[2] == 409)
        self.shape(winner, LOAN)
        self.shape(loser, ERROR)
        self.check(loser["code"] == "EQUIPMENT_UNAVAILABLE", "HTTP borrowing loser business code")
        self.check(self.loans_for(item) == [{"id": winner["id"], "student_id": sid, "equipment_id": item,
            "borrowing_date": "2030-06-15", "due_date": "2030-06-20", "return_date": None}], "exactly one committed active row")
        item = self.equipment("ELS36-RACE-RETURN")
        original, _ = self.borrow(sid, item)
        before = self.loan_row(original["id"])
        body["equipmentId"] = item
        borrowed, returned = self.contend("borrow-return", item,
            lambda: self.call("POST", "/api/loans", body, (201, 409)),
            lambda: self.call("POST", f"/api/loans/{original['id']}/return", {"returnDate": "2030-06-15"}))
        before["return_date"] = "2030-06-15"
        self.check(self.loan_row(original["id"]) == before, "cross-feature race preserves original history")
        rows = self.loans_for(item)
        self.check(len(rows) == (2 if borrowed[2] == 201 else 1), "borrow-return row count follows serial outcome")
        self.check(sum(row["return_date"] is None for row in rows) == (1 if borrowed[2] == 201 else 0), "borrow-return active count")
        if borrowed[2] == 409:
            self.check(borrowed[0]["code"] == "EQUIPMENT_UNAVAILABLE", "borrow-return conflict code")
        item = self.equipment("ELS36-RACE-DELETE")
        body["equipmentId"] = item
        borrowed, deleted = self.contend("borrow-delete", item,
            lambda: self.call("POST", "/api/loans", body, (201, 404)),
            lambda: self.call("DELETE", f"/api/equipment/{item}", expected=(204, 409)))
        if borrowed[2] == 201:
            self.check(deleted[2] == 409 and deleted[0]["code"] == "EQUIPMENT_HAS_HISTORY", "borrow-first serial outcome")
            self.check(len(self.loans_for(item)) == 1 and self.loan_row(borrowed[0]["id"])["return_date"] is None, "borrow-first committed state")
        else:
            self.check(deleted[2] == 204 and borrowed[0]["code"] == "EQUIPMENT_NOT_FOUND", "delete-first serial outcome")
            self.check(not self.loans_for(item) and self.sql(f"SELECT to_json(count(*)) FROM equipment WHERE id={item}") == 0, "delete-first committed state")

    def cleanup_owned_fixtures(self):
        counts = {}
        for table, predicate in (
            ("loan", "equipment_id IN (SELECT id FROM equipment WHERE inventory_code LIKE 'ELS36-%') "
                     "OR student_id IN (SELECT id FROM student WHERE email LIKE '%@els36.example.test')"),
            ("equipment", "inventory_code LIKE 'ELS36-%'"),
            ("student", "email LIKE '%@els36.example.test'"),
        ):
            counts[table] = self.sql(f"WITH removed AS (DELETE FROM {table} WHERE {predicate} RETURNING id) SELECT to_json(count(*)) FROM removed")
        return counts


def await_marker(process, marker, timeout):
    # A daemon reader avoids a platform-specific blocking-pipe select implementation.
    import queue
    lines = queue.Queue()
    def read():
        for line in process.stdout:
            lines.put(line.strip())
            if line.strip() == marker:
                return
        lines.put(None)
    reader = threading.Thread(target=read, daemon=True)
    reader.start()
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=max(0.01, deadline - time.monotonic()))
        except queue.Empty:
            break
        if line == marker:
            reader.join(timeout=1)
            return True
        if line is None:
            break
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:18036")
    parser.add_argument("--psql", default=os.environ.get("ELS_PSQL", "psql"))
    parser.add_argument("--expected-database", required=True)
    parser.add_argument("--output", default="target/els36-http-results.json")
    parser.add_argument("--preflight-only", action="store_true")
    parser.add_argument("--cleanup-owned-fixtures", action="store_true",
                        help="Remove only ELS36 inventory/email fixtures from the checked disposable workflow target")
    args = parser.parse_args()
    parsed = urllib.parse.urlsplit(args.base_url)
    if parsed.scheme != "http" or parsed.hostname not in ("localhost", "127.0.0.1", "::1") or parsed.path:
        parser.error("Only a loopback HTTP base URL is permitted")
    workflow = Workflow(args)
    if args.cleanup_owned_fixtures:
        print(json.dumps({"removedOwnedFixtures": workflow.cleanup_owned_fixtures()}))
        return
    if args.preflight_only:
        workflow.empty_preflight()
        print(json.dumps({"preflight": "passed", "effectiveTarget": workflow.identity}))
        return
    report = {"card": "ELS-36", "fixedClock": FIXED_INSTANT, "database": workflow.identity,
              "staffAccess": {"scheme": "HTTP Basic", "protected": "/api/**",
                              "credentialSource": "private process environment",
                              "transport": "loopback HTTP only; remote HTTPS not verified"},
              "status": "failed"}
    git = ["git", "-c", "safe.directory=" + ROOT.as_posix()]
    report["testedCommit"] = subprocess.check_output(git + ["rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    report["jarSha256"] = hashlib.sha256((ROOT / "target/equipment-lending-system-0.0.1-SNAPSHOT.jar").read_bytes()).hexdigest()
    try:
        workflow.run()
        report["status"] = "passed"
    except Exception as error:
        report["failure"] = {"type": type(error).__name__, "message": str(error)}
        raise
    finally:
        report.update(requestCount=len(workflow.requests), assertions=workflow.assertions,
                      requests=workflow.requests, databaseChecks=workflow.database_checks, races=workflow.races)
        output = ROOT / args.output
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({key: report[key] for key in ("status", "requestCount", "assertions", "races")}))


if __name__ == "__main__":
    main()
