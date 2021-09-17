rc /r icon.rc   || exit /b
clang -I "C:\Program Files\Java\jdk-14.0.2\include" -I "C:\Program Files\Java\jdk-14.0.2\include\win32" launcher.cpp icon.res -L"C:\Program Files\Java\jdk-14.0.2\lib" -ljvm -luser32 -lshell32 -o neoeedit.exe
copy neoeedit.exe ..\..\..\dist\
..\..\..\dist\neoeedit.exe ÄãºÃ

