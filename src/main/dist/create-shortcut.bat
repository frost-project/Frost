@echo off

set APP_DIR=%~dp0
set SHORTCUT_PATH=%APP_DIR%Frost.lnk
set EXEC_PATH=%APP_DIR%Frost-no-console.vbs
set ICON_PATH=%APP_DIR%jtc.ico

powershell ^
  "$Shell = New-Object -ComObject WScript.Shell; " ^
  "$Shortcut = $Shell.CreateShortcut('%SHORTCUT_PATH%'); " ^
  "$Shortcut.TargetPath = '%EXEC_PATH%'; " ^
  "$Shortcut.WorkingDirectory = '%APP_DIR%'; " ^
  "$Shortcut.IconLocation = '%ICON_PATH%'; " ^
  "$Shortcut.Save()"
