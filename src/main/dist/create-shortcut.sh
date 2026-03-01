#!/bin/bash

APP_DIR=$(pwd)
DESKTOP_PATH="${APP_DIR}/Frost.desktop"
EXEC_PATH="${APP_DIR}/Frost"
ICON_FILE="jtc.png"
ICON_PATH="${HOME}/.local/share/icons/Frost/${ICON_FILE}"

install -D "${APP_DIR}/${ICON_FILE}" "${ICON_PATH}"

echo "[Desktop Entry]
Type=Application
Name=Frost
Exec=\"${EXEC_PATH}\"
Path=${APP_DIR}
Icon=${ICON_PATH}
Terminal=false" > "${DESKTOP_PATH}"

chmod +x "${DESKTOP_PATH}"
