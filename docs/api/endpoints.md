# Contrato de API vigente

## Autenticación y formatos

Sesiones y CSRF, sin JWT. GET /login-token devuelve parameterName/headerName/token; POST /login usa formulario username/password y token. Redirige a / en éxito; error a /login.html?error. GET /api/perfil identifica usuario y rol. GET /api/csrf obtiene el token para escrituras; POST /logout cierra sesión. El PDF devuelve application/pdf y Content-Disposition attachment. Otros endpoints escolares devuelven JSON, no HTML. No registrar cookies, hashes ni contraseñas en los logs.

## Permisos escolares

| Rutas | Permiso |
|---|---|
| GET /api/actividades/pendientes, /api/actividades/{id} | ALUMNO, solo matrícula propia; ajeno/inexistente 404 |
| GET /api/v1/agenda | Cuenta activa, alcance según rol |
| GET /api/v1/family/students y /{student}/catalog, report, attendance, report.pdf | FAMILIAR con guardian_link vigente; año, periodo y curso autorizados |
| GET /api/v1/school/catalog, report | ALUMNO, identidad derivada del servidor |
| GET /api/v1/teaching/school/scopes y /{assignment}/marks; POST marks | DOCENTE asignado al aula/curso |
| GET /api/v1/teaching/publication-scopes; GET/POST /api/v1/teaching/publications | DOCENTE y alcance autorizado |
| GET /api/v1/school/publications | ALUMNO de los cursos autorizados |
| GET /api/v1/family/students/{student}/publications; POST /{id}/read | Apoderado vinculado; lectura explícita |
| POST /api/v1/assistant | Cuenta activa y contexto académico autorizado |
| /api/admin/... Django | ADMIN; rutas de revisión permiten REVIEWER según decoradores |

Reportes requieren yearId y periodId; courseId es opcional. Asistencia requiere yearId, from y to ISO. Publicación recibe requestId UUID, courseId, classroomId opcional, kind NOTICE/TASK/EXAM, title, instructions y dueAt obligatorio para TASK/EXAM. Las correcciones de notas requieren expectedVersion para evitar sobrescrituras.

Lecturas de publicaciones corresponden a apoderados, no a todos los alumnos. TASK/EXAM no se integraron con los pendientes anteriores.

## Inventario Spring extraído de los controladores

Los @RequestMapping de clase se anteponen a las rutas de método. El inventario conserva las firmas de entrada y los servicios que aplican autorización.

### ActividadController

```java
@RestController @RequestMapping("/api")
@GetMapping("/perfil") public Map<String,String> perfil(Principal p){return identities.profile(p.getName());}
@GetMapping("/actividades/pendientes") public List<ActividadService.Actividad> pendientes(Principal p){return service.pendientes(p.getName());}
@GetMapping("/actividades/{id}") public ActividadService.Actividad detalle(Principal p,@PathVariable long id){return service.detalle(p.getName(),id);}
@GetMapping("/csrf") public Map<String,String> csrf(CsrfToken t){return Map.of("parameterName",t.getParameterName(),"headerName",t.getHeaderName(),"token",t.getToken());}
```

### LoginController

```java
@GetMapping("/login-token") public Map<String,String> token(CsrfToken token){return Map.of("parameterName",token.getParameterName(),"token",token.getToken());}
@GetMapping(value="/favicon.ico",produces="image/svg+xml") public Resource favicon(){return new ClassPathResource("static/favicon.svg");}
```

### AdminController

```java
@RequestMapping("/api/admin")
@GetMapping("/csrf")
@GetMapping("/perfil")
@GetMapping("/cursos")
@GetMapping("/actividades")
@GetMapping("/actividades/{id}")
@PostMapping(value="/actividades", consumes="application/json")
```

### AcademicAdminController

```java
@RestController @RequestMapping("/api/admin/v1")
@GetMapping("/courses") public List<AcademicAdministration.Course> courses(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.courses(p.getName(),offset,limit);}
@PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
@PutMapping("/courses/{id}") public AcademicAdministration.Course edit(Principal p,@PathVariable long id,@RequestBody AcademicAdministration.Input input){return service.save(p.getName(),id,input,false);}
@GetMapping("/courses/{id}/students") public List<AcademicAdministration.Student> students(Principal p,@PathVariable long id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.students(p.getName(),id,offset,limit);}
@PutMapping("/courses/{id}/students") public void student(Principal p,@PathVariable long id,@RequestBody Enrollment input){service.enroll(p.getName(),id,input.username(),input.enrolled());}
@GetMapping("/audit") public List<AdministrativeAudit.Event> events(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.events(p.getName(),offset,limit);}
```

### AppealController

```java
@RestController @RequestMapping("/api/v1/appeals")
@GetMapping public List<AppealService.Appeal> list(Principal actor,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.list(actor.getName(),offset,limit);}
@PostMapping public AppealService.Appeal create(Principal actor,@RequestBody AppealService.Input input){return service.create(actor.getName(),input);}
```

### AssistantController

```java
@RestController @RequestMapping("/api/v1/assistant")
@PostMapping public AssistantService.Reply ask(Principal actor,@RequestBody AssistantService.Input input){return service.ask(actor.getName(),input);}
```

### CommunicationController

```java
@RestController @RequestMapping("/api/v1")
@GetMapping("/conversations") public List<CommunicationService.Conversation> list(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.conversations(p.getName(),offset,limit);}
@GetMapping("/conversations/{id}") public CommunicationService.Conversation conversation(Principal p,@PathVariable String id){return service.conversation(p.getName(),id);}
@PostMapping("/conversations/direct") @ResponseStatus(HttpStatus.CREATED)
@PostMapping("/conversations/groups") @ResponseStatus(HttpStatus.CREATED)
@PostMapping("/conversations/{id}/invitations") public Map<String,Boolean> invite(Principal p,@PathVariable String id,@RequestBody Invite input){service.invite(p.getName(),id,input.username());return Map.of("invited",true);}
@PostMapping("/conversations/{id}/invitation") public Map<String,Boolean> invitation(Principal p,@PathVariable String id,@RequestBody Invitation input){service.invitation(p.getName(),id,input.accept());return Map.of("saved",true);}
@GetMapping("/conversations/{id}/members") public List<CommunicationService.Member> members(Principal p,@PathVariable String id){return service.members(p.getName(),id);}
@PutMapping("/conversations/{id}/members/{username}") public Map<String,Boolean> member(Principal p,@PathVariable String id,@PathVariable String username,@RequestBody Membership input){
@GetMapping("/conversations/{id}/messages") public List<CommunicationService.Message> messages(Principal p,@PathVariable String id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.messages(p.getName(),id,offset,limit);}
@PostMapping(value="/conversations/{id}/messages",consumes="application/json") @ResponseStatus(HttpStatus.ACCEPTED)
@PutMapping("/messages/{id}") public CommunicationService.Message edit(Principal p,@PathVariable String id,@RequestBody CommunicationService.Edit input){return service.edit(p.getName(),id,input);}
@PostMapping("/messages/{id}/cancel") public Map<String,Boolean> cancel(Principal p,@PathVariable String id,@RequestBody Cancel input){service.cancel(p.getName(),id,input.expectedRevision());return Map.of("cancelled",true);}
```

### FamilySchoolController

```java
@RequestMapping("/api/v1/family")
@GetMapping("/students") public ResponseEntity<?> students(Principal p){return privateData(service.students(p.getName()));}
@GetMapping("/students/{student}/catalog") public ResponseEntity<?> catalog(Principal p,@PathVariable long student){return privateData(service.catalog(p.getName(),student));}
@GetMapping("/students/{student}/report") public ResponseEntity<?> report(Principal p,@PathVariable long student,@RequestParam int yearId,@RequestParam String periodId,@RequestParam(required=false) Long courseId){return privateData(service.report(p.getName(),student,yearId,periodId,courseId));}
@GetMapping("/students/{student}/attendance") public ResponseEntity<?> attendance(Principal p,@PathVariable long student,@RequestParam int yearId,@RequestParam LocalDate from,@RequestParam LocalDate to){return privateData(service.attendance(p.getName(),student,yearId,from,to));}
@GetMapping(value="/students/{student}/report.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
```

### GradeController

```java
@GetMapping("/api/v1/grades") public List<GradeService.CourseGrades> own(Principal p){return grades.own(p.getName());}
@GetMapping("/api/v1/teaching/courses/{courseId}/grades") public List<GradeService.StudentGrades> roster(Principal p,@PathVariable long courseId){return grades.roster(p.getName(),courseId);}
@PutMapping({"/api/v1/teaching/courses/{courseId}/grades","/api/admin/v1/courses/{courseId}/grades"})
```

### IdentityAdminController

```java
@RestController @RequestMapping("/api/admin/v1")
@GetMapping("/users") public List<IdentityService.Account> users(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return identities.users(p.getName(),offset,limit);}
@PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
@PostMapping("/guardians") public Map<String,Boolean> guardian(Principal p,@RequestBody Guardian input){identities.verifyGuardian(p.getName(),input.guardian(),input.studentId());return Map.of("verified",true);}
@DeleteMapping("/guardians/{guardian}/{studentId}") public void guardian(Principal p,@PathVariable String guardian,@PathVariable long studentId){identities.revokeGuardian(p.getName(),guardian,studentId);}
@PostMapping("/contacts") public Map<String,Boolean> contact(Principal p,@RequestBody Contact input){identities.approveContact(p.getName(),input.first(),input.second());return Map.of("approved",true);}
@PostMapping("/users/{username}/reset-password") public void reset(Principal p,@PathVariable String username,@RequestBody Password input){identities.resetPassword(p.getName(),username,input.password());}
@PostMapping("/users/{username}/disable") public void disable(Principal p,@PathVariable String username){identities.disable(p.getName(),username);}
@GetMapping("/reports") public List<Map<String,Object>> reports(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return identities.reports(p.getName(),offset,limit);}
@PostMapping("/timetable") @ResponseStatus(HttpStatus.CREATED)
@DeleteMapping("/timetable/{id}") public void slot(Principal p,@PathVariable String id){academic.removeSlot(p.getName(),id);}
```

### IdentityController

```java
@RestController @RequestMapping("/api/v1")
@GetMapping("/profile") public Map<String,String> profile(Principal p){return identities.profile(p.getName());}
@GetMapping("/preferences") public IdentityService.Preferences preferences(Principal p){return identities.preferences(p.getName());}
@PutMapping("/preferences") public IdentityService.Preferences preferences(Principal p,@RequestBody IdentityService.Preferences input){return identities.preferences(p.getName(),input);}
@GetMapping("/contacts") public List<IdentityService.Contact> contacts(Principal p){return identities.contacts(p.getName());}
@PutMapping("/contacts/{username}/preferences") public Map<String,Boolean> contact(Principal p,@PathVariable String username,@RequestBody ContactSettings input){
@PostMapping("/contacts/{username}/reports") public Map<String,String> report(Principal p,@PathVariable String username,@RequestBody ReportInput input){
@GetMapping("/agenda") public AcademicService.Agenda agenda(Principal p){return academic.agenda(p.getName());}
```

### MessageAttachmentController

```java
@PostMapping(value="/api/v1/conversations/{id}/messages",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
@GetMapping("/api/v1/messages/{id}/attachment")
@GetMapping("/api/admin/v1/moderation/messages/{id}/attachment")
```

### ModerationController

```java
@RestController @RequestMapping("/api/admin/v1/moderation")
@GetMapping("/messages") public List<CommunicationService.Message> queue(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="30")int limit){return service.queue(p.getName(),offset,limit);}
@PostMapping("/messages/{id}/review") public Map<String,Boolean> review(Principal p,@PathVariable String id,@RequestBody CommunicationService.Review input){
```

### NotificationController

```java
@RestController @RequestMapping("/api/v1/notifications")
@GetMapping public List<NotificationService.Notice> list(Principal p,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="30") int limit){return service.list(p.getName(),offset,limit);}
@PostMapping("/{id}/read") public Map<String,Boolean> read(Principal p,@PathVariable String id){service.read(p.getName(),id);return Map.of("read",true);}
```

### PushController

```java
@RestController @RequestMapping("/api/v1/push")
@GetMapping("/configuration") public Map<String,Boolean> configuration(){return Map.of("configured",service.configured());}
@PostMapping("/devices") public Map<String,String> register(Principal p,HttpServletRequest request,@RequestBody PushService.Registration body){var session=request.getSession(false);if(session==null)throw DomainException.denied();return service.register(p.getName(),session.getId(),session.getMaxInactiveInterval(),body);}
@DeleteMapping("/devices/{id}") public Map<String,Boolean> remove(Principal p,@PathVariable String id){service.remove(p.getName(),id);return Map.of("removed",true);}
```

### ReminderController

```java
@RestController @RequestMapping("/api/v1/reminders/preferences")
@GetMapping public ReminderService.Settings get(Principal p){return service.settings(p.getName());}
@PutMapping public ReminderService.Settings put(Principal p,@RequestBody ReminderService.Settings value){return service.settings(p.getName(),value);}
```

### SchoolPublicationController

```java
@RequestMapping("/api/v1")
@GetMapping("/teaching/publication-scopes") public ResponseEntity<?> scopes(Principal p){return data(service.scopes(p.getName()));}
@GetMapping("/teaching/publications") public ResponseEntity<?> teaching(Principal p){return data(service.teaching(p.getName()));}
@PostMapping("/teaching/publications") public ResponseEntity<?> publish(Principal p,@RequestBody SchoolPublicationService.Input input){return data(service.publish(p.getName(),input));}
@GetMapping("/family/students/{student}/publications") public ResponseEntity<?> family(Principal p,@PathVariable long student){return data(service.family(p.getName(),student));}
@PostMapping("/family/students/{student}/publications/{id}/read") public ResponseEntity<?> read(Principal p,@PathVariable long student,@PathVariable String id){service.read(p.getName(),student,id);return data(java.util.Map.of());}
@GetMapping("/school/publications") public ResponseEntity<?> own(Principal p){return data(service.own(p.getName()));}
```

### StatusController

```java
@GetMapping("/api/v1/statuses") public List<StatusService.Update> feed(Principal p){return service.feed(p.getName());}
@PostMapping(value="/api/v1/statuses",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
@GetMapping("/api/v1/statuses/{id}/media") public ResponseEntity<byte[]> media(Principal p,@PathVariable String id){var status=service.get(p.getName(),id);return response(service.media(p.getName(),id,false),status.kind());}
@PostMapping("/api/v1/statuses/{id}/seen") public Map<String,Boolean> seen(Principal p,@PathVariable String id){service.seen(p.getName(),id);return Map.of("seen",true);}
@PostMapping("/api/v1/statuses/{id}/delete") public Map<String,Boolean> delete(Principal p,@PathVariable String id){service.delete(p.getName(),id);return Map.of("deleted",true);}
@GetMapping("/api/admin/v1/moderation/statuses") public List<StatusService.Update> queue(Principal p,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.queue(p.getName(),offset,limit);}
@GetMapping("/api/admin/v1/moderation/statuses/{id}/media") public ResponseEntity<byte[]> reviewMedia(Principal p,@PathVariable String id){String kind=service.reviewKind(p.getName(),id);return response(service.media(p.getName(),id,true),kind);}
@PostMapping("/api/admin/v1/moderation/statuses/{id}/review") public Map<String,Boolean> review(Principal p,@PathVariable String id,@RequestBody StatusService.Review review){service.review(p.getName(),id,review);return Map.of("reviewed",true);}
```

### StudentSchoolController

```java
@RequestMapping("/api/v1/school")
@GetMapping("/catalog") public ResponseEntity<?> catalog(Principal p){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownCatalog(p.getName()));}
@GetMapping("/report") public ResponseEntity<?> report(Principal p,@RequestParam int yearId,@RequestParam String periodId,@RequestParam(required=false) Long courseId){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownReport(p.getName(),yearId,periodId,courseId));}
```

### TeachingSchoolController

```java
@RequestMapping("/api/v1/teaching/school")
@GetMapping("/scopes") public ResponseEntity<?> scopes(Principal p){return data(service.scopes(p.getName()));}
@GetMapping("/{assignment}/marks") public ResponseEntity<?> sheet(Principal p,@PathVariable String assignment,@RequestParam(required=false) String periodId){return data(service.sheet(p.getName(),assignment,periodId));}
@PostMapping("/{assignment}/marks") public ResponseEntity<?> save(Principal p,@PathVariable String assignment,@RequestBody TeachingSchoolService.Input input){return data(service.save(p.getName(),assignment,input));}
```

## Rutas Django

```python
path('api/admin/school/catalog', school.catalog),
path('api/admin/school/years', school.years),
path('api/admin/school/active-period', school.active_period),
path('api/admin/school/classrooms', school.classrooms),
path('api/admin/school/assignments', school.assignments),
path('api/admin/school/enrollments', school.enrollments),
path('api/admin/school/marks', school.marks),
path('api/admin/school/attendance', school.attendance),
path('api/admin/school/recovery', school.recovery),
path('api/admin/school/overview', school.overview),
path('api/admin/health', views.health),
path('api/admin/reviewer/password', review_accounts.change_password),
path('api/admin/v1/appeals', appeals.queue),
path('api/admin/v1/appeals/<str:appeal_id>', appeals.detail),
path('api/admin/v1/appeals/<str:appeal_id>/media', appeals.download),
path('api/admin/v1/appeals/<str:appeal_id>/resolve', appeals.resolve),
path('api/admin/v1/retention-holds', retention.holds),
path('api/admin/v1/moderation/<str:kind>/<str:item_id>/retention-hold', retention.hold),
path('moderation.html', moderation.page),
path('moderation.js', moderation.page, {'name':'moderation.js'}),
path('moderation.css', moderation.page, {'name':'moderation.css'}),
path('api/admin/v1/moderation/<str:kind>', moderation.queue),
path('api/admin/v1/moderation/messages/<str:item_id>/review', moderation.review, {'kind':'messages'}),
path('api/admin/v1/moderation/statuses/<str:item_id>/review', moderation.review, {'kind':'statuses'}),
path('api/admin/v1/moderation/messages/<str:item_id>/attachment', moderation.download, {'kind':'messages'}),
path('api/admin/v1/moderation/statuses/<str:item_id>/media', moderation.download, {'kind':'statuses'}),
path('api/admin/v1/timetable', scheduling.timetable),
path('api/admin/v1/timetable/<str:slot_id>', scheduling.timetable),
path('api/admin/v1/courses/<int:item_id>/grades', grades.save),
path('api/admin/v1/users', identity.users),
path('api/admin/v1/users/<str:username>/reset-password', identity.change_account, {'action':'reset-password'}),
path('api/admin/v1/users/<str:username>/enable', identity.change_account, {'action':'enable'}),
path('api/admin/v1/users/<str:username>/disable', identity.change_account, {'action':'disable'}),
path('api/admin/v1/users/<str:username>/archive', identity.change_account, {'action':'archive'}),
path('api/admin/v1/users/<str:username>/restore', identity.change_account, {'action':'restore'}),
path('api/admin/v1/guardians', identity.guardians),
path('api/admin/v1/guardians/<str:guardian>/<int:student_id>', identity.guardians),
path('api/admin/v1/contacts', identity.contacts),
path('api/admin/v1/reports', identity.reports),
path('api/admin/v1/courses', academic.courses),
path('api/admin/v1/courses/<int:item_id>', academic.courses),
path('api/admin/v1/courses/<int:item_id>/students', academic.students),
path('api/admin/v1/audit', academic.audit),
path('', views.page), path('admin.html', views.page), path('favicon.ico', views.favicon), path('favicon.svg', views.favicon), path('assets/<str:name>', views.asset),
path('api/admin/csrf', views.csrf), path('api/admin/login', views.login),
path('api/admin/logout', views.logout), path('api/admin/perfil', views.profile),
path('api/admin/cursos', views.courses), path('api/admin/actividades', views.activities),
path('api/admin/actividades/<int:item_id>', views.detail),
```
