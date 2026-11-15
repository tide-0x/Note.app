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
| 状态 | MVVM：页面级 ViewModel（Java + LiveData，单向数据流） |
| 存储 | SQLite（`SQLiteOpenHelper`，库名 `database.db`，连接由 `MyRepository` 单例统一持有） |

## 目录结构

```
app/src/main/java/com/iteam/examproject/
├── adapter/      RecyclerView 适配器
├── db/           MyDatabaseHelper（建表）
├── entity/       User / NoteGroup / Notes
├── repo/         MyRepository（全部 SQL）、NoteListOps（列表的内存增删改）
├── ui/activity/  LoginAndRegisterActivity、ContentActivity、EditNoteActivity
├── ui/fragment/  LoginAndRegister / Note / Group / Mine / EditNote（只做渲染、输入校验、对话框）
├── utils/        DensityUtil、RvDecorationUtil、ToastUtil
└── viewmodel/    MainViewModel（会话/列表/排序）、EditNoteViewModel、AuthViewModel、GroupManageViewModel、NoteListState、Event
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

单元测试（纯 JVM，不需要模拟器）：`./gradlew testDebugUnitTest`，覆盖 `NoteListOps` 与 `NoteListState` 的排序与增删改行为。

## 密码存储

`table_users.password` 存的是加盐摘要，格式 `sha256$迭代次数$盐(Base64)$摘要(Base64)`，每个密码单独随机盐、10000 次 SHA-256 迭代，实现见 `utils/PasswordUtil`。
早期版本写入的明文密码仍可登录，登录成功时自动升级为摘要；`SharedPreferences` 中不再保存密码。

## 数据流与线程

- `MyRepository` 是单例，全进程只有一条 SQLite 连接，生命周期跟随进程，业务代码不需要开关连接。
- 所有数据库操作都在一条名为 `note-db` 的后台线程上**串行**执行：界面层用 `runAsync(work, callback)` 提交，
  结果经主线程 Handler 回调。串行既保证「先写后读」的顺序，也天然避免并发写。
- 界面层不要直接调用 `MyRepository` 上的同步方法，那会在调用线程上当场执行 SQL。
- `runAsync` 的回调收到的是 `Result<T>`：`isSuccess()` 区分「执行失败」与「业务上没有结果」。
  - 登录密码错误属于后者：`isSuccess() == true`，`getData() == null`；
  - SQL 抛异常属于前者：`isSuccess() == false`，界面统一提示「操作失败，请重试」。
  - 无返回值的写操作用三参数版本 `runAsync(work, onSuccess, onError)`。
- **分层**：View（Fragment/Activity）只做渲染、输入校验、对话框与事件转发；数据访问、线程调度、业务判断都在 ViewModel；
  ViewModel 只调 `MyRepository`。界面层不出现 `MyRepository.getInstance()`。
- **状态归属**：`MainViewModel` 作用域是 ContentActivity（`ViewModelProvider(requireActivity())`），
  持有当前用户、当前分组、笔记列表和排序方式，列表页与分组页共享同一份，退出登录随页面销毁；
  `EditNoteViewModel` / `AuthViewModel` / `GroupManageViewModel` 各自管自己页面的状态。
- **跨页传参**：编辑页要编辑哪篇笔记由 Intent 传入（`Notes` 实现了 `Serializable`），
  保存/删除结果通过 Activity Result 回传列表页，取代原来「先写单例再 startActivity」的时序约定。
- **切换账号**：从「我的」正常 `startActivity` 叠一层登录页，不清任务栈、也不 `finish()` 内容界面——
  手势返回 / 返回箭头就是「反悔」，回到「我的」，登录态全程没被动过（切换模式只关掉自动登录，不写 SP）；
  按 Home、手势回桌面也算放弃切换：`LoginAndRegisterActivity.onUserLeaveHint()` 会把自己关掉，
  避免任务栈里留着一个已经离开过的切换页（用 `onUserLeaveHint` 而非 `onStop`，旋转屏幕和灭屏不受影响）；
  登录成功后用 `FLAG_ACTIVITY_CLEAR_TOP` 重建内容界面，避免旧账号的实例还压在栈里。
  只有「注销 / 退出登录」才用 `CLEAR_TASK`，因为那时已经没有可返回的会话了。
- **一次性事件**（Toast、跳登录页）用 `viewmodel/Event` 包装，观察者取过一次就不再重放，旋转屏幕不会重复弹。
- **提示统一走 `utils/ToastUtil`**：内部复用同一个 Toast 实例，新提示会顶掉正在显示的那条，
  不会因为连续操作排成一串；界面层不直接调 `Toast.makeText`（一次调用就是一条会排队的提示）。
- 笔记列表走**增量更新**：写库成功后不再把整张表查回来，而是按 `notesId` 在内存列表上插入 / 替换 / 删除这一条，
  状态机在 `viewmodel/NoteListState`（纯 Java，可单元测试），底层操作在 `repo/NoteListOps`。
  只有「登录」和「切换分组」这种换了一整份数据的场景才调 `MyRepository.getNotesList` 重新查库；
  若列表里认不出被改的那一条（早期数据 `notesId` 为 0 且标题、时间都变了），会退回一次整表重查兜底。
- 列表顺序只有一个来源：`NoteListState.sortWay`（默认按编辑时间倒序）。整份替换时的排序、写操作后的插入位置、
  用户点「排序」都按它算，排序本身也在内存里完成，不再查库；代价是排序方式一旦选了就一直沿用，切换分组后也不重置。

## 已知问题

- 分组与笔记的多行冗余存储（同一篇笔记在「全部笔记」与分组里各一行）靠 `notesId` 约定同步，
  改动任一处都要记得两边一起改（`updateNotes` 已按 `notesId` 一次改所有分组，`deleteNotes` 视 `groupId` 决定删一行还是全部）

## License

暂未指定 License。
