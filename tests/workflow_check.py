"""Run against a fresh temporary demo server only. No third-party Python packages required."""
import json, os, urllib.request, urllib.error, urllib.parse, http.cookiejar, uuid
BASE=os.environ.get('FIXFIRST_TEST_URL','http://127.0.0.1:8080')
class Client:
    def __init__(self):
        self.opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(self,path,body=None,expected=200,form=False,csrf=True):
        headers={};data=None
        if body is not None:
            if csrf:
                token=self.request('/api/csrf');headers[token['headerName']]=token['token']
            headers['Content-Type']='application/x-www-form-urlencoded' if form else 'application/json'
            data=(urllib.parse.urlencode(body) if form else json.dumps(body)).encode()
        req=urllib.request.Request(BASE+path,data=data,headers=headers)
        try:
            with self.opener.open(req,timeout=15) as res: code=res.status;raw=res.read()
        except urllib.error.HTTPError as err: code=err.code;raw=err.read()
        assert code==expected,f'{path}: expected {expected}, got {code}: {raw[:300]}'
        try:return json.loads(raw) if raw else None
        except json.JSONDecodeError:return raw.decode()
    def login(self,email,password):return self.request('/api/login',{'username':email,'password':password},204,True)
    def upload(self,path,kind,content,expected=201):
        token=self.request('/api/csrf');boundary='----FixFirst'+uuid.uuid4().hex
        body=(f'--{boundary}\r\nContent-Disposition: form-data; name="kind"\r\n\r\n{kind}\r\n'
              f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="photo.png"\r\nContent-Type: image/png\r\n\r\n').encode()+content+f'\r\n--{boundary}--\r\n'.encode()
        headers={token['headerName']:token['token'],'Content-Type':f'multipart/form-data; boundary={boundary}'}
        req=urllib.request.Request(BASE+path,data=body,headers=headers)
        try:
            with self.opener.open(req,timeout=15) as res: code=res.status;raw=res.read()
        except urllib.error.HTTPError as err: code=err.code;raw=err.read()
        assert code==expected,f'{path}: expected {expected}, got {code}: {raw[:300]}'
        return json.loads(raw) if raw else None

def run():
    suffix=uuid.uuid4().hex[:8];password='Student-Test-47!'
    anon,student,peer,admin=[Client() for _ in range(4)]
    anon.request('/api/issues',expected=401)
    student.request('/api/register',{'name':'Maya','email':f'maya-{suffix}@example.test','password':password},403,csrf=False)
    for client,name in [(student,'maya'),(peer,'arun')]:
        client.request('/api/register',{'name':name.title(),'email':f'{name}-{suffix}@example.test','password':password},201)
        client.login(f'{name}-{suffix}@example.test',password)
    student.request('/api/register',{'name':'Duplicate','email':f'maya-{suffix}@example.test','password':password},409)
    admin.login(os.environ.get('ADMIN_EMAIL','admin@fixfirst.local'),os.environ['ADMIN_PASSWORD'])
    assert student.request('/api/me')['role']=='STUDENT'
    assert admin.request('/api/me')['role']=='ADMIN'
    body={'title':'Water purifier not working','description':'No drinking water from the ground floor purifier.','category':'WATER','location':'Block A','equipment':'Purifier A-01'}
    student.request('/api/issues',{**body,'title':' '},400)
    issue_id=student.request('/api/issues',body,201)['id']
    route=f'/api/issues/{issue_id}';adm=f'/api/admin/issues/{issue_id}'
    def current():return next(i for i in student.request('/api/issues') if i['id']==issue_id)
    assert current()['score']['effective']==42 and current()['supportCount']==1
    student.request(route+'/support',{},409)
    peer.request(route+'/support',{},204);peer.request(route+'/support',{},409)
    assert current()['supportCount']==2 and current()['score']['effective']==44
    student.request(adm+'/status',{'status':'IN_PROGRESS','note':'Unauthorized'},403)
    admin.request(adm+'/status',{'status':'RESOLVED','note':'Invalid skip'},409)
    admin.request(adm+'/priority',{'score':101,'reason':'Invalid'},400)
    admin.request(adm+'/priority',{'score':90,'reason':'Only available purifier in this block.'},204)
    assert current()['score']['effective']==90 and current()['score']['calculated']==44
    admin.request(adm+'/priority',{'score':None,'reason':'Backup water station installed; restore rule score.'},204)
    assert current()['score']['effective']==44
    admin.request(adm+'/status',{'status':'IN_PROGRESS','note':'Technician checking the inlet.'},204)
    admin.request(adm+'/status',{'status':'RESOLVED','note':'Filter changed and flow checked.'},204)
    peer.request(route+'/support',{},409)
    peer.request(route+'/feedback',{'fixed':True,'note':'Not my report'},403)
    student.request(route+'/feedback',{'fixed':False,'note':'Water flow stops after a minute.'},204)
    assert current()['status']=='OPEN'
    admin.request(adm+'/status',{'status':'IN_PROGRESS','note':'Inspecting pressure sensor.'},204)
    admin.request(adm+'/status',{'status':'RESOLVED','note':'Pressure sensor replaced.'},204)
    student.request(route+'/feedback',{'fixed':True,'note':'Tested; flow is now continuous.'},204)
    assert current()['status']=='CLOSED'
    repeat=student.request('/api/issues',{**body,'title':'Purifier leaking after repair','location':'  BLOCK   A  '},201)['id']
    repeated=next(i for i in student.request('/api/issues') if i['id']==repeat)
    assert repeated['previousReports']>=1
    tech_email=f'tech-{suffix}@example.test';tech_password='Technician-Test-47!'
    tech_info=admin.request('/api/admin/technicians',{'name':'Ravi Technician','email':tech_email,'password':tech_password},201)
    tech=Client();tech.login(tech_email,tech_password)
    assigned=student.request('/api/issues',{'title':'Broken lab fan','description':'Fan stops after five minutes.','category':'EQUIPMENT','location':'Lab 3','equipment':'Fan L3-02'},201)['id']
    assigned_route=f'/api/issues/{assigned}'
    assigned_view=next(i for i in student.request('/api/issues') if i['id']==assigned)
    assert assigned_view['targetResolutionAt'] and not assigned_view['overdue']
    student.upload(assigned_route+'/photos','BEFORE',b'\x89PNG\r\n\x1a\n')
    admin.request(f'/api/admin/issues/{assigned}/assignment',{'technicianId':tech_info['id']},204)
    assert tech.request('/api/notifications')['unread']>=1
    assert any('assigned to' in item['message'] for item in student.request('/api/notifications')['items'])
    assert len(tech.request('/api/issues'))==1
    student.request(f'/api/issues/{assigned}/status',{'status':'IN_PROGRESS','note':'Not allowed'},403)
    tech.request(f'/api/issues/{assigned}/status',{'status':'IN_PROGRESS','note':'Checked motor and belt.'},204)
    assert student.request('/api/notifications')['unread']>=1
    tech.upload(f'{assigned_route}/photos','AFTER',b'\x89PNG\r\n\x1a\n')
    tech.request(f'/api/issues/{assigned}/status',{'status':'RESOLVED','note':'Replaced belt and tested.','repairCost':125.50},204)
    student.request(f'{assigned_route}/feedback',{'fixed':False,'note':'Fan still stops under load.'},204)
    reopened=next(i for i in student.request('/api/issues') if i['id']==assigned)
    assert reopened['status']=='OPEN' and reopened['assignedTechnicianId']==tech_info['id'] and reopened['targetResolutionAt']==assigned_view['targetResolutionAt']
    admin.request('/api/admin/assets',{'assetIdentifier':f'FAN-{suffix}','location':'Lab 3','category':'EQUIPMENT','estimatedReplacementCost':2000},201)
    analytics=admin.request('/api/admin/analytics')
    assert 'byCategory' in analytics and 'overdueActive' in analytics and 'replacementSuggestions' in analytics
    tech.request('/api/notifications/999999/read',{},expected=404)
    for title,category,location,equipment in [('Projector display flickers','EQUIPMENT','Block B · Room 204','Projector B-204'),('Blocked accessible entrance','ACCESSIBILITY','Library','North ramp'),('Loose power socket','ELECTRICAL','Computer Lab 2','Desk C-12')]:
        student.request('/api/issues',{'title':title,'category':category,'location':location,'equipment':equipment,'description':'Reported for inspection by the campus maintenance team.'},201)
    data=student.request('/api/issues')
    assert all('passwordHash' not in i and 'reporterId' not in i for i in data)
    student.request('/api/logout',{},204);student.request('/api/issues',expected=401)
    print('PASS: legacy workflow plus technician creation/assignment, technician-only task visibility, status permissions, deadlines, notification ownership, before/after uploads, assets and analytics.')
if __name__=='__main__':run()
