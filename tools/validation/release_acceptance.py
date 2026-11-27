"""ELS-47 requirement-scoped HTTP/SQL checks extending the unchanged lending oracles."""
import contextlib
import json
import os
import urllib.request
from lending_workflow import Workflow, STUDENT, EQUIPMENT, LOAN, ERROR, MAX_ID


class ReleaseAcceptance(Workflow):
    def __init__(self, args):
        self.events = []
        super().__init__(args)

    def sql(self, query):
        self.psql = ['docker', 'exec', '-i', self.args.container, 'psql', '-X', '-q', '-A', '-t',
                     '-v', 'ON_ERROR_STOP=1', '-U', os.environ['ELS_DB_USERNAME'], '-d', self.db.path[1:]]
        return super().sql(query)

    def call(self, *args, **kwargs):
        body, headers, status = super().call(*args, **kwargs)
        self.check(headers.get('Set-Cookie') is None, 'no authentication session cookie')
        if status >= 400:
            text = json.dumps(body)
            self.shape(body, ERROR)
            for private in (os.environ['ELS_STAFF_PASSWORD'], os.environ['ELS_DB_PASSWORD'],
                            self.authorization, 'stackTrace', 'SQLException', 'org.hibernate', 'Key (email)='):
                self.check(private not in text, 'sanitized error response')
        if status == 401:
            self.check(body['message'] == 'Staff credentials required' and body['fieldErrors'] == {},
                       'exact sanitized authentication message')
            self.check(headers.get('WWW-Authenticate') == 'Basic realm="equipment-lending"', 'Basic challenge')
        return body, headers, status

    @contextlib.contextmanager
    def scenario(self, *ids):
        first = len(self.requests)
        sql_first = len(self.database_checks)
        before = self.assertions
        event = {'scenarios': list(ids), 'status': 'failed'}
        try:
            yield
            event['status'] = 'passed'
        finally:
            event.update(requestRange=[first + 1, len(self.requests)],
                         sqlEvidenceRange=[sql_first + 1, len(self.database_checks)],
                         assertions=self.assertions - before)
            self.events.append(event)

    def reject_auth(self, method, path, payload, mode):
        before = self.snapshot()
        self.call(method, path, payload, expected=401, code='UNAUTHORIZED', authentication=mode)
        self.check(self.snapshot() == before, 'rejected authentication preserves every committed row')
        self.database_checks.append({'oracle': 'authentication full snapshot', 'method': method,
                                     'path': path, 'mode': mode or 'missing', 'unchanged': True})

    def run(self):
        super().run()
        self.baseline = {'requests': len(self.requests), 'assertions': self.assertions,
                         'sqlChecks': len(self.database_checks), 'races': list(self.races)}
        with self.scenario('TC11'):
            race = next(r for r in self.races if r['race'] == 'borrow-borrow')
            self.check(sorted(race['statuses']) == [201, 409] and race['observedEquipmentWaiters'] >= 2,
                       'independent coordinated HTTP contention retained')
        with self.scenario('TC01'):
            student, headers, _ = self.call('POST', '/api/students',
                    {'name': ' ELS47 Borrower ', 'email': ' ONE@ELS47.EXAMPLE.TEST '}, 201)
            sid = student['id']
            self.check(student == {'id': sid, 'name': 'ELS47 Borrower', 'email': 'one@els47.example.test'}, 'exact normalized student')
            self.shape(student, STUDENT)
            self.check(headers['Location'].endswith('/api/students/' + str(sid)), 'generated student Location')
            self.check(self.sql(f'SELECT row_to_json(s) FROM student s WHERE id={sid}') == student, 'committed creation DTO')
            self.check(self.call('GET', f'/api/students/{sid}')[0] == student, 'retrieved creation DTO')
        with self.scenario('TC02', 'TC24'):
            updated = self.call('PUT', f'/api/students/{sid}', {'name': 'Updated ELS47', 'email': ' ONE@ELS47.EXAMPLE.TEST '})[0]
            self.check(updated == dict(student, name='Updated ELS47'), 'normalized same-record email update')
            self.check(self.sql(f'SELECT row_to_json(s) FROM student s WHERE id={sid}') == updated, 'committed self update')
            for method, payload in [('GET', None), ('PUT', {'name': 'Missing', 'email': 'missing@els47.example.test'})]:
                self.reject(method, f'/api/students/{MAX_ID}', payload, 404, 'STUDENT_NOT_FOUND')
        with self.scenario('TC03', 'TC24'):
            good = {'name': 'Boundary', 'email': 'boundary@els47.example.test'}
            for field, value in [('name', ''), ('name', ' '), ('name', None), ('name', 'x'*121),
                                 ('email', 'bad-email'), ('email', ' '), ('email', None), ('email', 'x'*255),
                                 ('name', True), ('email', ['invalid']), ('extra', 'unknown')]:
                self.reject('POST', '/api/students', dict(good, **{field: value}), 400, 'VALIDATION_FAILED')
            self.reject('POST', '/api/students', dict(good, email=' ONE@ELS47.EXAMPLE.TEST '), 409, 'DUPLICATE_EMAIL')
            self.reject('PUT', f'/api/students/{sid}', dict(good, email=' SECOND@ELS36.EXAMPLE.TEST '), 409, 'DUPLICATE_EMAIL')
            edge = self.call('POST', '/api/students', {'name': 'N'*120, 'email': 'edge@els47.example.test'}, 201)[0]
            self.check(edge['name'] == 'N'*120, 'maximum student name accepted')
            maximum_email = 'a'*64 + '@' + 'b'*63 + '.' + 'c'*63 + '.' + 'd'*61
            edge = self.call('POST', '/api/students', {'name': 'Maximum Email', 'email': maximum_email}, 201)[0]
            self.check(len(edge['email']) == 254 and edge['email'] == maximum_email, 'maximum valid email accepted')
        with self.scenario('TC04', 'TC24'):
            item = self.equipment('ELS47-CAMERA')
            changed = self.call('PUT', f'/api/equipment/{item}',
                    {'inventoryCode': ' els47-camera ', 'name': ' Updated Camera ', 'category': ' Camera '})[0]
            self.check(changed == {'id': item, 'inventoryCode': 'ELS47-CAMERA', 'name': 'Updated Camera',
                                  'category': 'Camera', 'description': None, 'available': True}, 'exact replacement DTO')
            self.check(self.sql(f'SELECT row_to_json(e) FROM equipment e WHERE id={item}') ==
                       {'id': item, 'inventory_code': 'ELS47-CAMERA', 'name': 'Updated Camera', 'category': 'Camera', 'description': None}, 'committed normalized inventory')
            other = self.equipment('ELS47-OTHER')
        with self.scenario('TC05', 'TC24'):
            good = {'inventoryCode': 'ELS47-BOUNDARY', 'name': 'Boundary', 'category': 'Camera'}
            for field, value in [('inventoryCode', ' '), ('name', ''), ('category', None),
                    ('inventoryCode', 'X'*51), ('name', 'N'*121), ('category', 'C'*61), ('description', 'D'*1001),
                    ('inventoryCode', 123), ('category', True), ('description', ['invalid']), ('available', True)]:
                self.reject('POST', '/api/equipment', dict(good, **{field: value}), 400, 'VALIDATION_FAILED')
            self.reject('PUT', f'/api/equipment/{item}', dict(good, inventoryCode=' els47-other '), 409, 'DUPLICATE_INVENTORY_CODE')
            self.reject('POST', '/api/equipment', dict(good, inventoryCode=' els47-camera '), 409, 'DUPLICATE_INVENTORY_CODE')
            for method, payload in [('PUT', good), ('DELETE', None)]:
                self.reject(method, f'/api/equipment/{MAX_ID}', payload, 404, 'EQUIPMENT_NOT_FOUND')
            edge = self.call('POST', '/api/equipment', {'inventoryCode': 'X'*50, 'name': 'N'*120,
                             'category': 'C'*60, 'description': 'D'*1000}, 201)[0]
            self.check([len(edge[k]) for k in ['inventoryCode', 'name', 'category', 'description']] == [50, 120, 60, 1000], 'maximum equipment field lengths accepted')
        with self.scenario('TC06'):
            unused = self.equipment('ELS47-DELETE')
            self.call('DELETE', f'/api/equipment/{unused}', expected=204)
            self.check(self.sql(f'SELECT to_json(count(*)) FROM equipment WHERE id={unused}') == 0, 'deleted row absent')
        with self.scenario('TC07'):
            loan = self.borrow(sid, item)[0]
        with self.scenario('TC08'):
            base = {'studentId': sid, 'equipmentId': other, 'borrowingDate': '2030-06-10', 'dueDate': '2030-06-20'}
            for field, code in [('studentId', 'STUDENT_NOT_FOUND'), ('equipmentId', 'EQUIPMENT_NOT_FOUND')]:
                self.reject('POST', '/api/loans', dict(base, **{field: MAX_ID}), 404, code)
        with self.scenario('TC09'):
            for due in ['2030-06-10', '2030-06-09']:
                self.reject('POST', '/api/loans', dict(base, dueDate=due), 400, 'INVALID_LOAN_DATES')
            for field, value in [('borrowingDate', '2030-02-30'), ('borrowingDate', '-0001-01-01'),
                    ('dueDate', '+02030-06-20'), ('dueDate', 20300620), ('dueDate', [2030, 6, 20]),
                    ('studentId', 0), ('studentId', 1.5), ('equipmentId', True), ('extra', 1)]:
                self.reject('POST', '/api/loans', dict(base, **{field: value}), 400, 'VALIDATION_FAILED')
        with self.scenario('TC10'):
            self.reject('POST', '/api/loans', dict(base, equipmentId=item), 409, 'EQUIPMENT_UNAVAILABLE')
        with self.scenario('TC12'):
            before = self.loan_row(loan['id'])
            returned = self.call('POST', f"/api/loans/{loan['id']}/return", {'returnDate': '2030-06-15'})[0]
            self.check(returned == dict(loan, returnDate='2030-06-15'), 'exact returned DTO')
            self.check(self.loan_row(loan['id']) == dict(before, return_date='2030-06-15'), 'only committed return date changes')
        with self.scenario('TC13'):
            self.reject('POST', f"/api/loans/{loan['id']}/return", {'returnDate': '2030-06-16'}, 409, 'LOAN_ALREADY_RETURNED')
            self.reject('POST', f'/api/loans/{MAX_ID}/return', {'returnDate': '2030-06-15'}, 404, 'LOAN_NOT_FOUND')
        with self.scenario('TC14'):
            same_day_item = self.equipment('ELS47-SAME-DAY')
            same = self.borrow(sid, same_day_item)[0]
            for value, code in [('2030-06-09', 'INVALID_RETURN_DATE'), ('2030-02-30', 'VALIDATION_FAILED'),
                                ('+02030-06-15', 'VALIDATION_FAILED'), (20300615, 'VALIDATION_FAILED')]:
                self.reject('POST', f"/api/loans/{same['id']}/return", {'returnDate': value}, 400, code)
            result = self.call('POST', f"/api/loans/{same['id']}/return", {'returnDate': '2030-06-10'})[0]
            self.check(result == dict(same, returnDate='2030-06-10') and self.loan_row(same['id'])['return_date'] == '2030-06-10', 'same-day return persisted')
        with self.scenario('TC16'):
            before = self.snapshot()
            for suffix, condition in [('', 'TRUE'), ('?available=true', 'NOT EXISTS'), ('?available=false', 'EXISTS')]:
                where = condition if condition == 'TRUE' else condition + ' (SELECT 1 FROM loan l WHERE l.equipment_id=e.id AND l.return_date IS NULL)'
                expected = self.sql('SELECT COALESCE(json_agg(id ORDER BY id),\'[]\'::json) FROM equipment e WHERE ' + where)
                self.array('/api/equipment' + suffix, expected, EQUIPMENT)
            self.check(self.snapshot() == before, 'availability reads preserve all rows')
        with self.scenario('TC15'):
            history = self.loan_row(loan['id'])
            again = self.borrow(sid, item, borrowed='2030-06-15')[0]
            self.check(again['id'] != loan['id'] and self.loan_row(loan['id']) == history, 'reborrow preserves exact history')
            self.array(f'/api/loans?studentId={sid}&equipmentId={item}', [loan['id'], again['id']])
        with self.scenario('TC17'):
            dated = []
            for code, due in [('YESTERDAY', '2030-06-14'), ('TODAY', '2030-06-15'), ('TOMORROW', '2030-06-16')]:
                dated.append(self.borrow(sid, self.equipment('ELS47-' + code), due=due)[0]['id'])
            expected = self.sql("SELECT COALESCE(json_agg(id ORDER BY id),'[]'::json) FROM loan WHERE return_date IS NULL AND due_date<DATE '2030-06-15'")
            self.check(dated[0] in expected and dated[1] not in expected and dated[2] not in expected and loan['id'] not in expected, 'yesterday/today/tomorrow and returned boundary')
            before = self.snapshot(); self.array('/api/loans/overdue', expected)
            self.check(self.snapshot() == before, 'overdue read preserves rows')
        with self.scenario('TC18', 'TC25'):
            for query, where in [(f'studentId={sid}', f'student_id={sid}'), (f'equipmentId={item}', f'equipment_id={item}'),
                    (f'studentId={sid}&equipmentId={item}', f'student_id={sid} AND equipment_id={item}'),
                    (f'studentId={MAX_ID}', f'student_id={MAX_ID}')]:
                expected = self.sql("SELECT COALESCE(json_agg(id ORDER BY id),'[]'::json) FROM loan WHERE " + where)
                before = self.snapshot(); self.array('/api/loans?' + query, expected)
                self.check(self.snapshot() == before, 'history filters preserve rows')
            for query in ['studentId=-1', 'equipmentId=1.2', 'studentId=9223372036854775808', 'equipmentId=abc']:
                self.reject('GET', '/api/loans?' + query, None, 400, 'VALIDATION_FAILED')
        with self.scenario('TC19'):
            for history_item in [item, same_day_item]:
                self.reject('DELETE', f'/api/equipment/{history_item}', None, 409, 'EQUIPMENT_HAS_HISTORY')
        with self.scenario('TC20'):
            self.rollback_probes(sid, other, again['id'])
        with self.scenario('TC21'):
            operations = [('GET', f'/api/students/{sid}', None), ('GET', '/api/equipment', None),
                ('GET', '/api/loans', None), ('GET', '/api/loans/overdue', None),
                ('POST', '/api/students', {'name': 'Denied', 'email': 'denied@els47.example.test'}),
                ('PUT', f'/api/students/{sid}', {'name': 'Denied', 'email': 'denied@els47.example.test'}),
                ('POST', '/api/equipment', {'inventoryCode': 'DENIED', 'name': 'Denied', 'category': 'Denied'}),
                ('PUT', f'/api/equipment/{other}', {'inventoryCode': 'DENIED', 'name': 'Denied', 'category': 'Denied'}),
                ('DELETE', f'/api/equipment/{other}', None), ('POST', '/api/loans', base),
                ('POST', f"/api/loans/{again['id']}/return", {'returnDate': '2030-06-15'})]
            for mode in [None, 'invalid']:
                for method, path, payload in operations:
                    self.reject_auth(method, path, payload, mode)
            with urllib.request.urlopen(self.args.base_url + '/actuator/health', timeout=5) as response:
                self.check(response.status == 200 and json.load(response) == {'status': 'UP'}, 'public aggregate health only')
                self.check(response.headers.get('Set-Cookie') is None, 'public health creates no session')
        self.check(self.sql("SELECT to_json(count(*)) FROM loan l LEFT JOIN student s ON s.id=l.student_id LEFT JOIN equipment e ON e.id=l.equipment_id WHERE s.id IS NULL OR e.id IS NULL") == 0, 'final zero orphans')
        self.check(self.sql('SELECT to_json(count(*)) FROM (SELECT equipment_id FROM loan WHERE return_date IS NULL GROUP BY equipment_id HAVING count(*)>1) x') == 0, 'final active uniqueness')

    def rollback_probes(self, sid, available_item, active_loan):
        for kind, event, condition, method, path, payload in [
            ('borrow', 'INSERT', f'NEW.equipment_id={available_item}', 'POST', '/api/loans',
             {'studentId': sid, 'equipmentId': available_item, 'borrowingDate': '2030-06-15', 'dueDate': '2030-06-20'}),
            ('return', 'UPDATE', f'NEW.id={active_loan}', 'POST', f'/api/loans/{active_loan}/return', {'returnDate': '2030-06-15'})]:
            function = 'els47_fail_' + kind
            self.sql(f"CREATE FUNCTION {function}() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF {condition} THEN RAISE EXCEPTION 'synthetic acceptance failure' USING ERRCODE='XX000'; END IF; RETURN NEW; END $$; CREATE TRIGGER {function} AFTER {event} ON loan FOR EACH ROW EXECUTE FUNCTION {function}(); SELECT to_json(true)")
            try:
                self.reject(method, path, payload, 500, 'INTERNAL_ERROR')
                self.database_checks.append({'oracle': 'forced database failure full rollback', 'operation': kind, 'unchanged': True})
            finally:
                self.sql(f'DROP TRIGGER IF EXISTS {function} ON loan; DROP FUNCTION IF EXISTS {function}(); SELECT to_json(true)')
        self.check(self.sql("SELECT to_json(count(*)) FROM pg_proc WHERE proname IN ('els47_fail_borrow','els47_fail_return')") == 0, 'test fault functions removed')
