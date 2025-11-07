# Data Importer

### Основная информация

Сервис-импортер, обеспечивающий автоматическое наполнение базы данных актуальной информацией из таблиц Google.


## Стек

- Spring Boot 3
- Java 21
- [Google API Java Client](https://github.com/googleapis/google-api-java-client) для работы с Google Sheets API



### Ссылки на репозиторий документации
- [Бизнес аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/business-analytics/functionality/data-import.md)
- [Системная аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/functionality/data-import.md)

### Сборка и запуск Docker-образа

1. Сборка образа
    ```bash
    docker build -t data-importer:local-stack .
    ```
2. Запуск контейнера с активным профилем `local-stack`
    ```bash
    docker run -d -p 8080:8080 --name data-importer -e SPRING_PROFILES_ACTIVE=local-stack data-importer:local-stack
    ```
   
### Локальный запуск и тестирование
1. Предварительные требования

   - **Google Cloud SDK:** [Инструкция по установке](https://cloud.google.com/sdk/docs/install). 
   Это обязательно для локальной аутентификации.
   

2. Аутентификация

   - **Клонируйте репозиторий**.
   - **Выполните команды аутентификации в терминале Google Cloud SDK Shell `gcloud`:**
   ```bash
      gcloud auth application-default login --scopes=https://www.googleapis.com/auth/spreadsheets.readonly,https://www.googleapis.com/auth/cloud-platform
      gcloud auth application-default set-quota-project hover-344613
      gcloud config set project hover-344613
    ```
   - **Войдите в свой Google-аккаунт** и подтвердите разрешения.


3. Доступ к Google-таблице

   - Убедитесь, что Google-аккаунт, который вы использовали на Шаге 2, имеет доступ на чтение к целевой [Google-таблице](https://docs.google.com/spreadsheets/d/1Ya7J-nsc2m4D9MuiDbauypL7yWocHGoJj9P7tVimxp8/edit?gid=1043594367#gid=1043594367)
   - ID таблицы: `1Ya7J-nsc2m4D9MuiDbauypL7yWocHGoJj9P7tVimxp8`


4. Запуск
   - Через консоль
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=ide'
   ```

    - В IntelliJ IDEA
      * Run -> Edit Configurations....
      * В поле Active profiles введите имя профиля: `ide`
      

5. Тестирование
   
   - Запустить тесты из `user-import-test.http`