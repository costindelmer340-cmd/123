# 数据库目录

本目录用于存放数据库脚本和 ER 说明。

当前文件：

- `init.sql`：数据库初始化脚本。
- `seed.sql`：基础演示数据。
- `er.md`：实体关系说明。

数据库使用 MySQL 8.0，库名建议为 `ecommerce_after_sale`。

当前 `init.sql` 和 `seed.sql` 已切换为抖音售后中台模型，包含外部平台、店铺绑定、订单快照、同步日志、售后回写、客服工单、评价分析和 AI 训练数据。

演示账号默认密码为 `123456`，对应的 `password_hash` 使用 Spring Security 的 `{noop}` 格式，便于本地快速联调。

执行顺序：

```sql
SOURCE database/init.sql;
SOURCE database/seed.sql;
```

如果在命令行执行，可进入项目根目录后使用：

```bash
mysql -u root -p < database/init.sql
mysql -u root -p ecommerce_after_sale < database/seed.sql
```
