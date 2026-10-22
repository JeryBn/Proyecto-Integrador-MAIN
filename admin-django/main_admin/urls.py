from django.urls import path
from administration import views
from administration import academic
from administration import identity
from administration import scheduling, grades

urlpatterns = [
    path('api/admin/v1/timetable', scheduling.timetable),
    path('api/admin/v1/timetable/<str:slot_id>', scheduling.timetable),
    path('api/admin/v1/courses/<int:item_id>/grades', grades.save),
    path('api/admin/v1/users', identity.users),
    path('api/admin/v1/users/<str:username>/reset-password', identity.change_account, {'action':'reset-password'}),
    path('api/admin/v1/users/<str:username>/disable', identity.change_account, {'action':'disable'}),
    path('api/admin/v1/guardians', identity.guardians),
    path('api/admin/v1/guardians/<str:guardian>/<int:student_id>', identity.guardians),
    path('api/admin/v1/contacts', identity.contacts),
    path('api/admin/v1/reports', identity.reports),
    path('api/admin/v1/courses', academic.courses),
    path('api/admin/v1/courses/<int:item_id>', academic.courses),
    path('api/admin/v1/courses/<int:item_id>/students', academic.students),
    path('api/admin/v1/audit', academic.audit),
    path('', views.page), path('admin.html', views.page), path('assets/<str:name>', views.asset),
    path('api/admin/csrf', views.csrf), path('api/admin/login', views.login),
    path('api/admin/logout', views.logout), path('api/admin/perfil', views.profile),
    path('api/admin/cursos', views.courses), path('api/admin/actividades', views.activities),
    path('api/admin/actividades/<int:item_id>', views.detail),
]
