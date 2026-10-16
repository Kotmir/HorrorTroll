@echo off
setlocal EnableDelayedExpansion
chcp 65001 > nul
cd /d "%~dp0"

echo ==========================================
echo    [HorrorTroll] Автоматический Git-Пуш
echo ==========================================
echo.

REM ---------- Проверки ----------
git --version > nul 2>&1
if errorlevel 1 (
    echo [Ошибка] Git не найден. Установи Git и перезапусти консоль.
    goto :end
)

git rev-parse --is-inside-work-tree > nul 2>&1
if errorlevel 1 (
    echo [Ошибка] Эта папка не является git-репозиторием.
    goto :end
)

set "branch="
for /f "delims=" %%b in ('git branch --show-current') do set "branch=%%b"
if not defined branch (
    echo [Ошибка] Не удалось определить ветку, возможно, detached HEAD.
    goto :end
)
echo Текущая ветка: !branch!
echo.

REM ---------- Защита секретов и мусора: .gitignore для Java/Maven ----------
set "ignore_added="
call :ensure_ignore "target/"
call :ensure_ignore ".vscode/"
call :ensure_ignore "*.log"
call :ensure_ignore ".idea/"
call :ensure_ignore "*.iml"
call :ensure_ignore ".DS_Store"
if defined ignore_added echo [Защита] .gitignore обновлён для Java/Maven.

REM ---------- Защита: убираем из git то, что уже случайно попало (например, target) ----------
set "untracked="
for /f "delims=" %%f in ('git ls-files "target/" "*.log" ".idea/"') do (
    git rm -r --cached -q -- "%%f"
    echo [Защита] Убран из git: %%f
    set "untracked=1"
)
if defined untracked (
    echo [Защита] Мусорные файлы убраны из отслеживания Git.
)
echo.

REM ---------- Добавление изменений ----------
echo [1/3] Добавление изменений...
git add -A
git diff --cached --quiet
if not errorlevel 1 (
    echo Нет изменений для коммита.
    goto :end
)
echo.
git status -s

REM ---------- Сообщение коммита ----------
echo.
echo Выберите вариант сообщения для коммита:
echo  1. Сгенерировать случайное ламповое
echo  2. Ввести своё вручную
set "choice=1"
set /p "choice=Ваш выбор (1 или 2, по умолчанию 1): "

set "comment="
if "!choice!"=="2" (
    set /p "comment=Введите сообщение для коммита: "
)
if defined comment set "comment=!comment:"=!"

if not defined comment (
    set "msg0=✨ Обновление хоррор-функций и фиксы багов"
    set "msg1=👻 Новые пранки уже в строю, сервер готов пугать"
    set "msg2=🛠️ Очередной апдейт для стабильности плагина"
    set "msg3=🌙 Ночные правки и оптимизация GUI-меню"
    set "msg4=💡 Наводим красоту и порядок в кодовой базе"
    set "msg5=⚡ Прокачка функционала HorrorTroll прошла успешно"
    set /a rand=!random! %% 6
    for %%i in (!rand!) do set "comment=!msg%%i!"
)

REM ---------- Коммит ----------
echo.
echo [2/3] Создание коммита: !comment!
git commit -m "!comment!"
if errorlevel 1 (
    echo [Ошибка] Коммит не удался.
    goto :end
)

REM ---------- Push ----------
echo.
echo [3/3] Отправка на GitHub, ветка !branch!...
git push origin "!branch!"
if errorlevel 1 (
    echo.
    echo Push не прошёл. Возможно, на GitHub есть новые коммиты.
    set /p "retry=Подтянуть изменения через pull --rebase и повторить? (y/N): "
    if /i not "!retry!"=="y" goto :end

    git pull --rebase origin "!branch!"
    if errorlevel 1 (
        echo [Ошибка] Rebase не удался. Реши конфликты вручную.
        goto :end
    )
    git push origin "!branch!"
    if errorlevel 1 (
        echo [Ошибка] Повторный push не удался.
        goto :end
    )
)

echo.
echo ==========================================
echo   [Готово] Код улетел в репозиторий, ветка !branch!
echo ==========================================

:end
echo.
pause
endlocal
exit /b

REM ---------- Подпрограмма: добавить строку в .gitignore, если её там нет ----------
:ensure_ignore
findstr /x /c:"%~1" ".gitignore" > nul 2>&1
if errorlevel 1 (
    if not defined ignore_added (
        if exist ".gitignore" echo.>>".gitignore"
        set "ignore_added=1"
    )
    echo %~1>>".gitignore"
)
exit /b