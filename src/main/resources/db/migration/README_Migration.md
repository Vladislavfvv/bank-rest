# Database Migrations

Этот проект использует Liquibase для управления миграциями базы данных.

## Структура миграций

```
src/main/resources/db/
├── changelog/
│   ├── db.changelog-master.yaml          # Главный файл миграций
│   ├── contexts.yaml                     # Информация о контекстах
│   ├── v1.0/                            # Версия 1.0 - основные таблицы
│   │   ├── db.changelog-v1.0.yaml       # Главный файл версии 1.0
│   │   ├── 001-create-users-table.yaml  # Создание таблицы пользователей
│   │   ├── 002-create-cards-table.yaml  # Создание таблицы карт
│   │   └── 003-insert-initial-data.yaml # Начальные данные
│   └── v1.1/                            # Версия 1.1 - расширения
│       ├── db.changelog-v1.1.yaml       # Главный файл версии 1.1
│       ├── 001-add-card-type-column.yaml # Добавление типа карты
│       └── 002-create-transactions-table.yaml # Таблица транзакций
└── scripts/
    └── create-database.sql               # Скрипт создания БД
```

## Контексты выполнения

- `dev` - для разработки (включает тестовые данные)
- `test` - для тестирования (включает тестовые данные)  
- `prod` - для продакшена (без тестовых данных)
- `!prod` - для всех окружений кроме продакшена

## Команды Liquibase

### Применение миграций
```bash
# Все миграции
mvn liquibase:update

# С определенным контекстом
mvn liquibase:update -Dliquibase.contexts=dev

# Только продакшен (без тестовых данных)
mvn liquibase:update -Dliquibase.contexts=prod
```

### Откат миграций
```bash
# Откат последней миграции
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Откат до определенной даты
mvn liquibase:rollback -Dliquibase.rollbackDate=2024-01-01
```

### Информация о миграциях
```bash
# Статус миграций
mvn liquibase:status

# История изменений
mvn liquibase:history

# Предварительный просмотр SQL
mvn liquibase:updateSQL
```

## Создание новых миграций

1. Создайте новую версию (например, v1.2):
   ```
   src/main/resources/db/changelog/v1.2/
   ```

2. Добавьте файлы миграций с префиксом номера:
   ```
   001-your-migration-name.yaml
   002-another-migration.yaml
   ```

3. Создайте главный файл версии:
   ```yaml
   # db.changelog-v1.2.yaml
   databaseChangeLog:
     - include:
         file: db/changelog/v1.2/001-your-migration-name.yaml
   ```

4. Добавьте версию в master файл:
   ```yaml
   # db.changelog-master.yaml
   databaseChangeLog:
     - include:
         file: db/changelog/v1.2/db.changelog-v1.2.yaml
   ```

## Настройка базы данных

1. Создайте базу данных PostgreSQL:
   ```bash
   psql -U postgres -f src/main/resources/db/scripts/create-database.sql
   ```

2. Настройте переменные окружения:
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```

3. Запустите приложение - миграции применятся автоматически.

## Тестовые данные

В версии 1.0 создаются тестовые пользователи:

- **Администратор:**
  - Email: `admin@bankcards.com`
  - Password: `password`
  - Role: `ROLE_ADMIN`

- **Пользователь:**
  - Email: `john.doe@example.com`
  - Password: `password`
  - Role: `ROLE_USER`
  - Карты: 2 активные карты с балансом

**Примечание:** Тестовые данные создаются только в контекстах `dev` и `test`, не в `prod`.
