Set Shell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

appDir = FSO.GetParentFolderName(WScript.ScriptFullName)
Shell.Run """" & appDir & "\Frost.bat" & """", 0
