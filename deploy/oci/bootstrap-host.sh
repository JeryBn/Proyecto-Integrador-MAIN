#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${EUID:-$(id -u)} -eq 0 ]] || {
  printf 'Ejecuta este instalador con sudo.\n' >&2
  exit 2
}

deploy_user="${1:-ubuntu}"
id "$deploy_user" >/dev/null 2>&1 || { printf 'No existe el usuario %s.\n' "$deploy_user" >&2; exit 2; }
source /etc/os-release
[[ "${ID:-}" == ubuntu && "${VERSION_ID:-}" =~ ^(22\.04|24\.04|26\.04)$ ]] || {
  printf 'Este instalador exige Ubuntu LTS compatible con Docker.\n' >&2
  exit 2
}

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install --yes ca-certificates curl git gnupg openssl unattended-upgrades

install -m 0755 -d /etc/apt/keyrings
curl --fail --silent --show-error --location https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
cat >/etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
apt-get update
apt-get install --yes docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
usermod --append --groups docker "$deploy_user"

install -d -m 0750 -o "$deploy_user" -g "$deploy_user" /opt/main
chown -R "$deploy_user:$deploy_user" /opt/main
if [[ ! -d /opt/main/.git ]]; then
  sudo -u "$deploy_user" git clone --branch main --single-branch \
    https://github.com/JeryBn/Proyecto-Integrador-MAIN.git /opt/main
else
  remote="$(sudo -u "$deploy_user" git -C /opt/main remote get-url origin)"
  [[ "$remote" == 'https://github.com/JeryBn/Proyecto-Integrador-MAIN.git' ]] || {
    printf 'El repositorio existente en /opt/main no corresponde a MAIN.\n' >&2
    exit 1
  }
fi

install -m 0644 /opt/main/deploy/oci/systemd/main-backup.service /etc/systemd/system/main-backup.service
install -m 0644 /opt/main/deploy/oci/systemd/main-backup.timer /etc/systemd/system/main-backup.timer
systemctl daemon-reload
systemctl enable main-backup.timer

printf 'Servidor preparado. Cierra y vuelve a abrir la sesión SSH para usar Docker sin sudo.\n'
printf 'Siguiente paso: cd /opt/main/deploy/oci && ./prepare.sh --domain TU_DOMINIO\n'
printf 'Los puertos se controlan en la lista de seguridad de Oracle; no se modificó UFW.\n'
