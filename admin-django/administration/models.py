from django.db import models

# Flyway es dueño del esquema de negocio. Django no crea ni modifica estas tablas.
class Teacher(models.Model):
    id = models.BigIntegerField(primary_key=True)
    nombre = models.CharField(max_length=100)
    class Meta:
        managed = False
        db_table = 'docente'

class Course(models.Model):
    id = models.BigIntegerField(primary_key=True)
    nombre = models.CharField(max_length=100)
    docente = models.ForeignKey(Teacher, on_delete=models.DO_NOTHING, db_column='docente_id')
    admin_version = models.IntegerField(default=0)
    class Meta:
        managed = False
        db_table = 'curso'

class Activity(models.Model):
    id = models.BigIntegerField(primary_key=True)
    curso = models.ForeignKey(Course, on_delete=models.DO_NOTHING, db_column='curso_id')
    titulo = models.CharField(max_length=150)
    descripcion = models.CharField(max_length=2000)
    fecha_limite = models.DateTimeField()
    class Meta:
        managed = False
        db_table = 'actividad'

class SharedUser(models.Model):
    username = models.CharField(primary_key=True, max_length=60)
    display_name = models.CharField(max_length=100)
    role = models.CharField(max_length=16)
    password_hash = models.CharField(max_length=100)
    enabled = models.BooleanField()
    password_change_required = models.BooleanField(default=False)
    alumno_id = models.BigIntegerField(null=True)
    docente_id = models.BigIntegerField(null=True)
    dark_theme = models.BooleanField(default=False)
    notifications = models.BooleanField(default=True)
    status_audience = models.CharField(max_length=16, default='CONTACTS')
    class Meta:
        managed = False
        db_table = 'main_user'

class Audit(models.Model):
    id = models.CharField(primary_key=True, max_length=36)
    actor = models.CharField(max_length=60)
    action = models.CharField(max_length=40)
    subject = models.CharField(max_length=200)
    occurred_at = models.DateTimeField()
    class Meta:
        managed = False
        db_table = 'administrative_audit'
