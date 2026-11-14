# Note.app — 简易记事本

一个 Android 本地记事本 App：账号登录/注册、笔记增删改查、分组管理、按编辑时间排序、标题搜索，全部数据存在本机 SQLite 中，不联网。

## 功能

- 注册 / 登录 / 修改密码 / 切换账号 / 注销账号
- 笔记：新建、编辑、删除、保存时记录编辑时间
- 分组：新建、重命名、删除（删除分组会连带删除组内笔记）；「全部笔记」是内置的 `groupId = 0` 聚合视图
- 笔记可归入某个分组，同时保留在「全部笔记」中
- 按编辑时间升序 / 降序排序
- 按标题模糊搜索

## 技术栈

| 项 | 版本 / 说明 |
| --- | --- |
| 语言 | Java 11 |
| 构建 | Gradle 8.11.1 + AGP 8.9.1 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 21 |
| UI | View + Fragment + ViewBinding，Material Components，单 Activity + 底部导航 |
| 状态 | ViewModel + LiveData（`SharedDataRepository` 单例共享数据） |
| 存储 | SQLite（`SQLiteOpenHelper`，库名 `database.db`，连接由 `MyRepository` 单例统一持有） |

## 目录结构

```
app/src/main/java/com/iteam/examproject/
├── adapter/      RecyclerView 适配器
├── db/           MyDatabaseHelper（建表）
├── entity/       User / NoteGroup / Notes
├── repo/         MyRepository（全部 SQL 操作）、SharedDataRepository（内存共享数据）
├── ui/activity/  LoginAndRegisterActivity、ContentActivity、EditNoteActivity
├── ui/fragment/  LoginAndRegister / Note / Group / Mine / EditNote
├── utils/        DensityUtil、RvDecorationUtil、ToastUtil
└── viewmodel/    MyViewModel
```

## 数据库表

| 表 | 字段 |
| --- | --- |
| `table_users` | userId(PK)、account、password |
| `table_groups` | userId、groupId(PK)、groupName |
| `table_notes` | userId、groupId、notesId、title、content、editedTime |

`table_notes` 里同一条笔记在不同分组下会有多行记录，它们共享同一个 `notesId`。

## 运行

1. 需要 JDK 17（AGP 8.9 要求）与 Android SDK 35。
2. 在项目根目录创建 `local.properties`，写入 `sdk.dir=<你的 SDK 路径>`。
3. 执行 `./gradlew assembleDebug`，或直接用 Android Studio 打开运行。

> release 构建需要在 `app/build.gradle.kts` 中自行配置签名信息，仓库内不含 keystore。

## 密码存储

`table_users.password` 存的是加盐摘要，格式 `sha256$迭代次数$盐(Base64)$摘要(Base64)`，每个密码单独随机盐、10000 次 SHA-256 迭代，实现见 `utils/PasswordUtil`。
早期版本写入的明文密码仍可登录，登录成功时自动升级为摘要；`SharedPreferences` 中不再保存密码。

## 已知问题

- `SharedDataRepository` 用 `setValue()` 更新，只能在主线程调用
- 界面逻辑层仍用匿名内部类而非 lambda，`ContentActivity` 的 Fragment 切换为手写实现
- 数据库读写仍在主线程执行，笔记量大时可能卡顿

## License

暂未指定 License。
