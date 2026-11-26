FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends build-essential default-libmysqlclient-dev pkg-config ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app/backend/django-admin
COPY backend/django-admin/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY backend/django-admin .
COPY backend/springboot/src/main/resources/static /app/backend/springboot/src/main/resources/static
COPY database/seed.sql /app/database/seed.sql
EXPOSE 8000
CMD ["sh","-c","python manage.py migrate --noinput && waitress-serve --listen=0.0.0.0:8000 --threads=4 main_admin.wsgi:application"]
