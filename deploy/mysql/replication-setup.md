# MySQL 主从复制初始化说明

`docker-compose.yml` 已经把主库和从库容器拉起，并挂载了初始化目录：
- `deploy/mysql/master/init`
- `deploy/mysql/slave/init`

当前已自动完成：
- 主库创建复制账号 `repl / repl123456`
- 从库启用只读相关配置

## 手动完成主从绑定

### 1. 查看主库 binlog 位点

```sql
SHOW MASTER STATUS;
```

记下：
- `File`
- `Position`

### 2. 在从库执行复制配置

```sql
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-master',
  SOURCE_PORT=3306,
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl123456',
  SOURCE_LOG_FILE='mysql-bin.000001',
  SOURCE_LOG_POS=157;
```

把 `SOURCE_LOG_FILE` 和 `SOURCE_LOG_POS` 替换成上一步查到的值。

### 3. 启动从库复制

```sql
START REPLICA;
SHOW REPLICA STATUS\G
```

重点确认：
- `Replica_IO_Running: Yes`
- `Replica_SQL_Running: Yes`

## 容器内执行示例

查看主库位点：

```bash
docker exec -it flashsale-mysql-master mysql -uroot -p123456 -e "SHOW MASTER STATUS\\G"
```

连接从库：

```bash
docker exec -it flashsale-mysql-slave mysql -uroot -p123456
```

## 说明

- 当前项目默认本地开发时主从都可指向同一个库，便于快速启动
- 只有在你需要演示真正的读写分离时，才需要按本说明完成主从绑定
