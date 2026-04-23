# Germes — Интернет-магазин

> Полнофункциональный интернет-магазин с интеграцией платёжной системы ЮKassa (СБП).

---

## Содержание

- [Стек технологий](#стек-технологий)
- [Архитектура проекта](#архитектура-проекта)
- [Быстрый старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [Модель данных](#модель-данных)
- [Фронтенд](#фронтенд)
- [API](#api)
- [Ключевые модули](#ключевые-модули)
- [Известные проблемы](#известные-проблемы)

---

## Стек технологий

| Категория | Технологии |
|-----------|-----------|
| Язык | Java 17+ |
| Фреймворк | Spring Boot, Spring MVC, Spring Security, Spring Data JPA |
| База данных | PostgreSQL |
| Платежи | ЮKassa (СБП) |
| Email | Spring Mail (SMTP) |
| Сборка | Maven |
| Фронтенд | HTML + Vanilla JS (статика, раздаётся Spring Boot) |
| Маппинг | MapStruct |

---

## Архитектура проекта

Монорепо — фронтенд живёт внутри backend-проекта в папке `resources/`.

```
germes/
├── backend/                        ← Spring Boot приложение
│   └── src/main/
│       ├── java/org/example/
│       │   ├── config/             ← SecurityConfig, DataLoader
│       │   ├── controller/         ← REST-контроллеры
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── mapper/             ← MapStruct маперы
│       │   ├── model/              ← JPA-сущности
│       │   ├── repository/
│       │   ├── service/            ← бизнес-логика
│       │   └── App.java
│       └── resources/              ← статический фронтенд
│           ├── css/
│           ├── img/
│           ├── js/
│           │   ├── admin.js
│           │   ├── core.js
│           │   ├── data.js
│           │   └── pages.js
│           ├── index.html
│           ├── catalog.html
│           ├── product.html
│           ├── cart.html
│           ├── basket.html
│           ├── admin.html
│           ├── shop_admin_panel.html
│           ├── about.html
│           ├── contacts.html
│           ├── delivery.html
│           └── application.properties
└── front/                          ← (см. Известные проблемы)
```

---

## Быстрый старт

### Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Установка и запуск

1. Клонировать репозиторий:
   ```bash
   git clone <repo-url>
   cd germes
   ```

2. Создать базу данных PostgreSQL:
   ```sql
   CREATE DATABASE germes_db;
   ```

3. Задать переменные окружения:
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=your_password
   export YOOKASSA_RETURN_URL=https://yourdomain.com/order/success
   export MAIL_HOST=smtp.gmail.com
   export MAIL_USERNAME=your@email.com
   export MAIL_PASSWORD=your_mail_password
   export ADMIN_PASSWORD=your_admin_password
   ```

4. Собрать и запустить приложение:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

5. Открыть в браузере: [http://localhost:8080](http://localhost:8080)

---

## Конфигурация

Все чувствительные данные задаются через переменные окружения:

| Переменная | Описание | Дефолт |
|-----------|----------|--------|
| `SPRING_DATASOURCE_URL` | URL базы данных | `jdbc:postgresql://localhost:5432/germes_db` |
| `DB_USERNAME` | Пользователь БД | `postgres` |
| `DB_PASSWORD` | Пароль БД | — |
| `PORT` | Порт сервера | `8080` |
| `YOOKASSA_RETURN_URL` | URL возврата после оплаты | — |
| `MAIL_HOST` | SMTP-хост | — |
| `MAIL_PORT` | SMTP-порт | `587` |
| `MAIL_USERNAME` | Email отправителя | — |
| `MAIL_PASSWORD` | Пароль от почты | — |
| `ADMIN_PASSWORD` | Пароль администратора | — |


---

## Модель данных

### Сущности

#### `Product` — товар
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Long | Идентификатор |
| `name` | String | Название товара |
| `price` | BigDecimal | Цена |
| `category` | Category | Категория |

#### `Category` — категория товаров
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Long | Идентификатор |
| `name` | String | Название (уникальное) |
| `products` | List\<Product\> | Товары в категории |

#### `Cart` — корзина (привязана к сессии браузера)
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Long | Идентификатор |
| `sessionId` | String | ID сессии пользователя |
| `items` | List\<CartItem\> | Позиции корзины |
| `totalPrice` | BigDecimal | Итоговая сумма (вычисляется) |

#### `Order` — заказ
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Long | Идентификатор |
| `createdAt` | LocalDateTime | Дата создания |
| `totalPrice` | BigDecimal | Сумма заказа |
| `customerEmail` | String | Email покупателя |
| `customerPhone` | String | Телефон покупателя |
| `deliveryAddress` | String | Адрес доставки |
| `deliveryType` | DeliveryType | Способ доставки |
| `paymentMethod` | PaymentMethod | Способ оплаты |
| `paymentStatus` | PaymentStatus | Статус оплаты |
| `status` | OrderStatus | Статус заказа |
| `trackNumber` | String | Номер отслеживания |
| `receiptNumber` | String | Номер чека |
| `paidAt` | LocalDateTime | Дата оплаты |
| `yookassaPaymentId` | String | ID платежа в ЮKassa |

### Перечисления (Enum)

**`OrderStatus`** — статус заказа:
- `NEW` — новый
- `PAID` — оплачен
- `SHIPPED` — отправлен
- `DELIVERED` — доставлен

**`PaymentStatus`** — статус платежа:
- `PENDING` — ожидает оплаты
- `PAID` — оплачен
- `FAILED` — ошибка
- `CANCELED` — отменён
- `PAYMENT_FAILED` — платёж не прошёл

**`DeliveryType`** — способ доставки:
- `CDEK` — СДЭК
- `DELOVYE_LINII` — Деловые Линии

**`PaymentMethod`** — способ оплаты:
- `SBP` — Система быстрых платежей

### Схема связей

```
Category ──< Product ──< CartItem >── Cart
                   └──< OrderItem >── Order
```

---

## Фронтенд

Фронтенд реализован на чистом HTML + Vanilla JS. Страницы раздаются Spring Boot как статические ресурсы и общаются с backend через REST API.

| Страница | Описание |
|----------|----------|
| `index.html` | Главная страница |
| `catalog.html` | Каталог товаров |
| `product.html` | Карточка товара |
| `cart.html` / `basket.html` | Корзина |
| `admin.html` / `shop_admin_panel.html` | Панель администратора |
| `about.html` | О магазине |
| `contacts.html` | Контакты |
| `delivery.html` | Условия доставки |

---

## API

### Товары и категории

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/products` | Список всех товаров |
| `GET` | `/api/products/{id}` | Товар по ID |
| `GET` | `/api/categories` | Список категорий |

### Корзина

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/cart` | Получить корзину текущей сессии |
| `POST` | `/api/cart/add` | Добавить товар |
| `PUT` | `/api/cart/update` | Обновить количество |
| `DELETE` | `/api/cart/remove/{id}` | Удалить позицию |

### Заказы

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/orders` | Создать заказ из корзины |
| `GET` | `/api/orders/{id}` | Получить заказ |
| `POST` | `/api/orders/pay` | Инициировать оплату через ЮKassa (СБП) |

---

## Ключевые модули

| Файл | Описание |
|------|----------|
| `config/SecurityConfig.java` | Настройка Spring Security, правила доступа к эндпоинтам |
| `config/DataLoader.java` | Начальные данные: категории и товары |
| `controller/OrderController.java` | Заказы, включая эндпоинт `/pay` |
| `service/OrderService.java` | Логика заказов, создание платежа в ЮKassa |
| `service/CartService.java` | Корзина: add / update / remove |
| `service/YookassaService.java` | Интеграция с ЮKassa (СБП) |

---



---

## Лицензия

Проект разработан на заказ. Все права защищены.