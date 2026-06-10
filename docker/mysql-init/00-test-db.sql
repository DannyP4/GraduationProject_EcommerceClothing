-- integration tests use a separate uniform_store_test schema
CREATE DATABASE IF NOT EXISTS uniform_store_test;
GRANT ALL PRIVILEGES ON uniform_store_test.* TO 'uniform'@'%';
GRANT CREATE ON *.* TO 'uniform'@'%';
FLUSH PRIVILEGES;
