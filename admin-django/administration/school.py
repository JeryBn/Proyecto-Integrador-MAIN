from datetime import date
from uuid import uuid4

from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from .academic import ValidationError, body, positive, record
from .identity import mutation, text
from .moderation import execute, rows
from .views import admin_only, error


def period_dates(value):
    try:
        start, end = (date.fromisoformat(value[key]) for key in ('start', 'end'))
    except (KeyError, TypeError, ValueError):
        raise ValidationError('Indica las fechas de inicio y fin.') from None
    if start > end:
        raise ValidationError('La fecha de fin debe ser igual o posterior al inicio.')
    return start, end


def catalog(years, selected):
    periods = rows('SELECT period_no AS number,name,starts_on AS startsOn,ends_on AS endsOn FROM academic_period WHERE year_number=%s ORDER BY starts_on,period_no', [selected])
    sections = rows('SELECT id,grade,label FROM school_section WHERE year_number=%s ORDER BY grade,label', [selected])
    assignments = rows("""
        SELECT a.course_id AS courseId,a.section_id AS sectionId,c.nombre AS course
        FROM course_section a JOIN school_section s ON s.id=a.section_id
        JOIN curso c ON c.id=a.course_id WHERE s.year_number=%s ORDER BY c.nombre,c.id
        """, [selected])
    return dict(years=years, selectedYear=selected, periods=periods, sections=sections, assignments=assignments)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def structure(request):
    if request.method == 'GET':
        years = rows('SELECT year_number AS year,starts_on AS startsOn,ends_on AS endsOn FROM academic_year ORDER BY year_number DESC LIMIT 100')
        selected = request.GET.get('year')
        if selected is None:
            return JsonResponse(catalog(years, years[0]['year'] if years else None))
        if not selected.isascii() or not selected.isdecimal() or len(selected) != 4:
            return error(400, 'VALIDATION_ERROR', 'Selecciona un año escolar válido.')
        year = int(selected)
        if not any(item['year'] == year for item in years):
            return error(404, 'NOT_FOUND', 'El año escolar no está registrado.')
        return JsonResponse(catalog(years, year))

    value = body(request, {'kind','year','start','end','number','name','grade','label','courseId','sectionId'})
    kind, year = value.get('kind'), value.get('year')
    if type(year) is not int or not 1970 <= year <= 2200:
        raise ValidationError('Indica un año escolar entre 1970 y 2200.')
    if kind == 'year':
        start, end = period_dates(value)
        if start.year != year or end <= start or (end - start).days > 366:
            raise ValidationError('El año debe empezar en el año indicado y durar hasta 366 días.')
        execute('INSERT INTO academic_year VALUES(%s,%s,%s)', [year,start,end])
        record(request,'SCHOOL_YEAR_CREATED',year)
        return JsonResponse({'saved':True},status=201)

    suffix = ' FOR UPDATE' if connection.vendor == 'mysql' else ''
    bounds = rows('SELECT starts_on,ends_on FROM academic_year WHERE year_number=%s' + suffix, [year])
    if not bounds:
        return error(404, 'NOT_FOUND', 'El año escolar no está registrado.')
    if kind == 'period':
        number = value.get('number')
        if type(number) is not int or not 1 <= number <= 12:
            raise ValidationError('Indica un número de período entre 1 y 12.')
        name = text(value.get('name'),60)
        start, end = period_dates(value)
        first, last = bounds[0]['starts_on'], bounds[0]['ends_on']
        if isinstance(first,str):
            first, last = date.fromisoformat(first), date.fromisoformat(last)
        if start < first or end > last:
            raise ValidationError('El período debe estar dentro del año escolar.')
        if rows('SELECT period_no FROM academic_period WHERE year_number=%s AND starts_on<=%s AND ends_on>=%s', [year,end,start]):
            return error(409,'SCHOOL_PERIOD_OVERLAP','El período se cruza con otro registrado.')
        execute('INSERT INTO academic_period VALUES(%s,%s,%s,%s,%s)', [year,number,name,start,end])
        record(request,'SCHOOL_PERIOD_CREATED',f'{year}:{number}')
    elif kind == 'section':
        section_id = str(uuid4())
        execute('INSERT INTO school_section VALUES(%s,%s,%s,%s)',
                [section_id,year,text(value.get('grade'),32),text(value.get('label'),16)])
        record(request,'SCHOOL_SECTION_CREATED',section_id)
    elif kind == 'assignment':
        course_id, section_id = value.get('courseId'), value.get('sectionId')
        if not positive(course_id) or not isinstance(section_id,str) or len(section_id) != 36:
            raise ValidationError('Selecciona el curso y su sección.')
        if not rows('SELECT id FROM school_section WHERE id=%s AND year_number=%s',[section_id,year]):
            return error(404,'NOT_FOUND','La sección no corresponde al año seleccionado.')
        if not rows('SELECT id FROM curso WHERE id=%s',[course_id]):
            return error(404,'NOT_FOUND','El curso no está disponible.')
        execute('INSERT INTO course_section VALUES(%s,%s)', [course_id,section_id])
        record(request,'SCHOOL_COURSE_ASSIGNED',f'{course_id}:{section_id}')
    else:
        raise ValidationError('Operación escolar inválida.')
    return JsonResponse({'saved':True},status=201)
