# Xml Parser

Тестовое задание на позицию Java Backend Developer в SoftMotion.

---

## Использованные технологии

- **Java 17**
- **Maven**
- **Groovy 3.0.19** (`groovy.xml.XmlSlurper`)
- **Docker Compose**
- **PostgreSQL, JDBC**

## Функционал
Сервис, обрабатывающий xml по предоставленной ссылке.

## Установка и запуск через Docker

### Сборка образа

```bash
docker compose build --no-cache
```

### Режимы запуска

#### Запустить контейнер в режиме батча
Контейнер удалится после выхода. Используйте этот режим, когда приложение должно само выполнить задачу и выйти.

```bash
docker compose run --rm xml-loader
```

#### Запустить контейнер в интерактивном режиме (Рекомендовано)

```bash
docker compose run --rm xml-loader --interactive
```

**Пример работы в интерактивном режиме:**

Мы переходим в интерактивный режим:

```text
[+] Creating 1/1
✔️ Container xml_loader_db  Running
=== XML to PostgreSQL Loader ===
Loading config from .env...
Connected to database successfully.
Commands: [list] [schema <table>] [update <table>] [update_all] [exit]
```

**Посмотреть список отношений:**

```text
> list
14:36:32.343 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Fetching table names from XML...
14:36:34.474 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Found tables: [currencies, categories, offers]
Tables in XML: [currencies, categories, offers]
```

**Обновление данных:**
Команда для обновления всех таблиц. Также можно обновить конкретную таблицу, указав ее имя (например, `update offers`).

```text
> update_all
14:36:42.425 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Fetching table names from XML...
14:36:43.244 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Found tables: [currencies, categories, offers]
14:36:43.244 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Parsing data for table: currencies
14:36:44.335 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Parsed 1 rows for table currencies
14:36:44.335 [main] INFO motion.soft.XmlLoader.Services.DbService - Starting update for table: currencies
14:36:44.377 [main] INFO motion.soft.XmlLoader.Services.DbService - Analyzing schema changes for table: currencies
14:36:44.395 [main] INFO motion.soft.XmlLoader.Services.DbService - No schema changes detected for table currencies
14:36:44.396 [main] DEBUG motion.soft.XmlLoader.Services.DbService - Prepared UPSERT statement: INSERT INTO currencies (rate, id) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET rate = EXCLUDED.rate
14:36:44.408 [main] INFO motion.soft.XmlLoader.Services.DbService - Loaded 1 rows into currencies
14:36:44.408 [main] INFO motion.soft.XmlLoader.Services.DbService - Update completed for table: currencies
14:36:44.408 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Parsing data for table: categories
14:36:45.428 [main] INFO motion.soft.XmlLoader.Services.XmlParserService - Parsed 492 rows for table categories
14:36:45.428 [main] INFO motion.soft.XmlLoader.Services.DbService - Starting update for table: categories
14:36:45.431 [main] INFO motion.soft.XmlLoader.Services.DbService - Analyzing schema changes for table: categories
14:36:45.437 [main] ERROR motion.soft.XmlLoader.Services.DbService - Structure mismatch detected for table 'categories'. Columns missing in XML but present in DB: parentid. Aborting to prevent data loss.
Error: Structure mismatch detected for table 'categories'. Columns missing in XML but present in DB: parentid. Aborting to prevent data loss.
```

**Генерация DDL:**
Команда для генерации DDL для конкретного отношения (например, для `offers`):

```text
> schema offers
14:37:05.796 [main] DEBUG motion.soft.XmlLoader.Services.XmlParserService - Generating DDL for table: offers
14:37:06.666 [main] DEBUG motion.soft.XmlLoader.Services.XmlParserService - Generated DDL: CREATE TABLE IF NOT EXISTS offers (available TEXT, id TEXT PRIMARY KEY, url TEXT, price TEXT, currencyId TEXT, categoryId TEXT, picture TEXT, name TEXT, vendor TEXT, vendorCode TEXT UNIQUE NOT NULL, description TEXT, param TEXT, count TEXT);
DDL:
CREATE TABLE IF NOT EXISTS offers (available TEXT, id TEXT PRIMARY KEY, url TEXT, price TEXT, currencyId TEXT, categoryId TEXT, picture TEXT, name TEXT, vendor TEXT, vendorCode TEXT UNIQUE NOT NULL, description TEXT, param TEXT, count TEXT);
```

**Выход:**
Команда для выхода из интерактивного режима:

```text
> exit
```

## Отладка

### Подключение к БД

```bash
docker exec -it xml_loader_db psql -U user -d testdb
```

### Работа в фоновом режиме

Для отладки запускаем `docker compose up -d` без флага `--rm`. Если нам нужен постоянно работающий сервис в фоне, то вводим команду:

```bash
docker compose up -d
```

Для просмотра логов:
```bash
docker compose logs -f xml-loader
```

