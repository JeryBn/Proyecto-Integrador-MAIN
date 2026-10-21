from django.urls import path
from administration import views

urlpatterns = [
    path('', views.page), path('admin.html', views.page), path('assets/<str:name>', views.asset),
    path('api/admin/csrf', views.csrf), path('api/admin/login', views.login),
    path('api/admin/logout', views.logout), path('api/admin/perfil', views.profile),
    path('api/admin/cursos', views.courses), path('api/admin/actividades', views.activities),
    path('api/admin/actividades/<int:item_id>', views.detail),
]
