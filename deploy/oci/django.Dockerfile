FROM python:3.12.16-slim-bookworm AS wheel-build
RUN apt-get update \
    && apt-get install --yes --no-install-recommends build-essential default-libmysqlclient-dev pkg-config \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY admin-django/requirements.txt ./requirements.txt
RUN --mount=type=cache,target=/root/.cache/pip pip wheel --wheel-dir /wheels -r requirements.txt

FROM python:3.12.16-slim-bookworm
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl ffmpeg libmariadb3 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /app main
COPY --from=wheel-build /wheels /wheels
COPY admin-django/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir --no-index --find-links=/wheels -r /tmp/requirements.txt \
    && rm -rf /wheels /tmp/requirements.txt
WORKDIR /app/admin-django
COPY --chown=10001:10001 admin-django /app/admin-django
COPY --chown=10001:10001 src/main/resources/static /app/src/main/resources/static
COPY --chown=10001:10001 deploy/oci/django-entrypoint.sh /app/entrypoint.sh
RUN chmod 0555 /app/entrypoint.sh
USER 10001:10001
EXPOSE 8090
ENTRYPOINT ["/app/entrypoint.sh"]
