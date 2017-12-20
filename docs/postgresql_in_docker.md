# Работа с PostgreSql в Docker
## Подготовительные действия
- установка клиента, на хостовой машине: 
<pre><code>sudo apt install postgresql-client-9.6</code></pre>

- создать пользователя если его еще нет: 
<pre><code>sudo useradd -M postgres</code></pre>

- создадим папку, куда будет мапиться файлы с данными базы: 
<pre><code>sudo mkdir /var/postgresql</code></pre>

- установить права на созданную папку: 
<pre><code>sudo chown -R postgres:postgres /var/postgresql</code></pre>

## Запуск контейнера:
<pre><code>sudo docker run -d --name postgresql -v /var/postgresql:/var/lib/postgresql/data -e POSTGRES_PASSWORD=postgres postgres:9.6</code></pre>
или с пробросом порта:
<pre><code>sudo docker run -d -p 5432:5432 --name postgresql -v /var/postgresql:/var/lib/postgresql/data -e POSTGRES_PASSWORD=postgres postgres:9.6</code></pre>

## Подключение к PostgreSql:
<pre><code>psql -h localhost -U postgres</code></pre>

## Создание БД:
<pre><code>CREATE DATABASE chronodb ENCODING=UTF8;</code></pre>