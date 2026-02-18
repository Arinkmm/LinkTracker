# LinkTracker

LinkTracker – Telegram-бот, который отслеживает изменения на веб-страницах и оперативно информирует пользователя о них.

## Как запустить

### 1. Получение токена
Если у вас нет бота, создайте его в Telegram через [@BotFather](https://t.me/BotFather) и скопируйте полученный API Token.

### 2. Настройка токена
Бот ожидает токен в переменной окружения `TELEGRAM_TOKEN`.

**Через файл .env:**
1. Создайте в корне файл `.env`
2. Впишите туда токен:
```env
TELEGRAM_TOKEN=123456789:ABCDEF...
```

**Через Run Configuration:**
1. Перейдите в **Run** -> **Edit Configurations**. 
2. В поле **Environment variables** добавьте: `TELEGRAM_TOKEN=ваш_токен`.

Приложение готово к работе!
