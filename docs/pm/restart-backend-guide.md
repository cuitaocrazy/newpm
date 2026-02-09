# 重启后端服务指南

## 修改内容

已修复"更新人"显示问题，添加了字符集排序规则：

```sql
LEFT JOIN sys_user updater ON p.update_by COLLATE utf8mb4_unicode_ci = updater.user_name
```

## 重启步骤

### 1. 停止旧的后端服务

```bash
# 查找 Java 进程
ps aux | grep ruoyi-admin.jar | grep -v grep

# 如果有进程，记下 PID 并停止
kill <PID>

# 或者强制停止
pkill -f ruoyi-admin.jar
```

### 2. 启动新的后端服务

```bash
cd /Users/kongli/ws-claude/PM/newpm
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 3. 验证服务启动

等待看到以下日志：
```
若依启动成功
```

### 4. 刷新前端页面

- 按 `Cmd+Shift+R` 强制刷新浏览器

### 5. 测试

1. 编辑任意项目，修改备注
2. 保存后返回列表
3. 查看"更新人"列
4. ✅ 应该显示用户昵称（如"张三"）

## 如果还是不显示

### 检查 1：后端是否使用了新的 JAR

```bash
# 查看 JAR 文件的修改时间
ls -lh ruoyi-admin/target/ruoyi-admin.jar

# 应该是最新的时间戳
```

### 检查 2：查看后端日志

```bash
# 查看最近的日志
tail -f logs/sys-info.log

# 或者查看控制台输出
```

### 检查 3：测试 SQL

```bash
docker exec 3523a41063b7 mysql -uroot -ppassword ry-vue -e "
SELECT
  p.project_name,
  p.update_by AS original_update_by,
  u.nick_name AS updater_nick_name,
  COALESCE(u.nick_name, p.update_by) AS final_update_by
FROM pm_project p
LEFT JOIN sys_user u ON p.update_by COLLATE utf8mb4_unicode_ci = u.user_name
ORDER BY p.update_time DESC
LIMIT 3;
"
```

### 检查 4：清除浏览器缓存

1. 打开浏览器开发者工具（F12）
2. 右键点击刷新按钮
3. 选择"清空缓存并硬性重新加载"

### 检查 5：查看网络请求

1. 打开浏览器开发者工具（F12）
2. 切换到 Network 标签
3. 刷新页面
4. 找到 `/project/project/list` 请求
5. 查看 Response，检查 `updateBy` 字段的值

## 常见问题

### 问题 1：后端没有重启

**症状：** 修改后没有效果

**解决：** 确保停止旧进程，启动新进程

### 问题 2：浏览器缓存

**症状：** 后端已更新，前端还是旧数据

**解决：** 强制刷新（Cmd+Shift+R）或清除缓存

### 问题 3：MyBatis 缓存

**症状：** SQL 已修改，但查询结果没变

**解决：** 重启后端服务，MyBatis 会重新加载 XML

## 快速重启命令

```bash
# 一键重启
pkill -f ruoyi-admin.jar && sleep 2 && java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```
