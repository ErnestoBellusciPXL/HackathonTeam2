#!/bin/bash
set -e

echo "Starting MySQL with custom staging initialization..."

# Start MySQL in the background
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
until mysqladmin ping -h localhost --silent; do
    sleep 2
done

echo "MySQL is ready. Dropping and recreating database..."

# Drop the database if it exists to ensure clean state
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "DROP DATABASE IF EXISTS ${MYSQL_DATABASE};"

echo "Loading FullData.sql with duplicate handling..."

# Import the SQL file with IGNORE to skip duplicate entries
sed -e 's/INSERT INTO/INSERT IGNORE INTO/g' \
    -e 's/`app_db`/`openinzichten`/g' \
    -e "s/'app_db'/'openinzichten'/g" \
    -e "s/ app_db/ openinzichten/g" \
    /docker-entrypoint-initdb.d/FullData.sql | mysql -u root -p"${MYSQL_ROOT_PASSWORD}"

echo "FullData.sql loaded successfully!"

# Keep MySQL running in the foreground
wait $MYSQL_PID
