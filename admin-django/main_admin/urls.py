from django.urls import path
from administration import views
from administration import academic, school, enrollment_import
from administration import identity, account_activation
from administration import scheduling, grades
from administration import moderation
from administration import retention
from administration import appeals
from administration import review_accounts
from administration import school_assistant

handler404 = 'administration.views.api_not_found'
handler500 = 'administration.views.api_server_error'

urlpatterns = [
    path('api/admin/v1/assistant', school_assistant.ask),
    path('api/admin/v1/users/<str:username>/invitation', account_activation.invitation),
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
    path('api/admin/v1/moderation/campus', moderation.campus_queue),
    path('api/admin/v1/moderation/campus/<str:item_id>/review', moderation.campus_review),
    path('api/admin/v1/moderation/<str:kind>', moderation.queue),
    path('api/admin/v1/moderation/statuses/<str:item_id>/review', moderation.review, {'kind':'statuses'}),
    path('api/admin/v1/moderation/statuses/<str:item_id>/media', moderation.download, {'kind':'statuses'}),
    path('api/admin/v1/timetable', scheduling.timetable),
    path('api/admin/v1/timetable/<str:slot_id>', scheduling.timetable),
    path('api/admin/v1/courses/<int:item_id>/grades', grades.save),
    path('api/admin/v1/users', identity.users),
    path('api/admin/v1/users/<str:username>/reset-password', identity.change_account, {'action':'reset-password'}),
    path('api/admin/v1/users/<str:username>/enable', identity.change_account, {'action':'enable'}),
    path('api/admin/v1/users/<str:username>/disable', identity.change_account, {'action':'disable'}),
    path('api/admin/v1/guardians', identity.guardians),
    path('api/admin/v1/guardians/<str:guardian>/<int:student_id>', identity.guardians),
    path('api/admin/v1/contacts', identity.contacts),
    path('api/admin/v1/reports', identity.reports),
    path('api/admin/v1/school-structure', school.structure),
    path('api/admin/v1/courses', academic.courses),
    path('api/admin/v1/courses/<int:item_id>', academic.courses),
    path('api/admin/v1/courses/<int:item_id>/students', academic.students),
    path('api/admin/v1/courses/<int:item_id>/students/import', enrollment_import.import_students),
    path('api/admin/v1/audit', academic.audit),
    path('', views.page), path('admin.html', views.page), path('favicon.ico', views.favicon), path('favicon.svg', views.favicon), path('assets/<str:name>', views.asset),
    path('api/admin/csrf', views.csrf), path('api/admin/login', views.login),
    path('api/admin/logout', views.logout), path('api/admin/perfil', views.profile),
    path('api/admin/cursos', views.courses), path('api/admin/actividades', views.activities),
    path('api/admin/actividades/<int:item_id>', views.detail),
]
