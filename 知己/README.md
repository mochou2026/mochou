# 知己

前后端分离的账号系统，三个页面：**登录页**（一张卡片：用户名 / 密码 / 登录，附注册与修改密码入口）、**首页**（首屏轮播图 + 照片墙 + 下拉菜单 + 背景音乐）和 **个人中心**（返回按钮 / 个人名字 / 修改密码 / 上传图片 / 我的照片墙）。

| 层次 | 技术选型 |
| --- | --- |
| 前端 | Vue 3 + Vite（原生 CSS，浅色系） |
| 后端 | Spring Boot 3.3.5 / JDK 17 / Maven |
| 数据库 | MySQL 8，库名 `mochou` |

### 登录页 `/login`

- **卡片永远在正中央**：`min-height:100dvh` + flex 居中，实测 360×640、420×780、900×1200、1280×800、1600×500 五种窗口尺寸下卡片中心与视口中心偏差均为 0，缩放/改窗口大小都不跑偏。
- **浅色系**：淡蓝紫渐变背景 + 白色卡片 + 淡蓝主色按钮 + 淡灰描边输入框。
- 一张卡片三种模式 —— 登录（默认）、注册、修改密码，用卡片底部的链接互相切换，不需要跳页。
- 用户名实时显示 `0/10` 字数、错误与成功提示行内展示。
- **登录成功 → 跳转首页**；**注册成功 → 回到登录模式**并提示「注册成功，请登录」。

### 首页 `/home`

- **背景「莫愁前路无知己」7 个字居中且若隐若现**：`position:fixed` + flex 双向居中（实测 7 字整体中心与视口中心偏差 dx=0、dy=0），**置于最底层**（在首屏内容之下），每个字独立做 6.4s 呼吸动画、延迟按 0.85s 递增形成波浪，透明度只在 0.06 ~ 0.24 之间。
- **首屏轮播图**：`PhotoCarousel` 组件，多张图时 5 秒自动切换（悬停暂停）、带左右箭头与圆点、交叉淡入淡出；只有一张图时不显示箭头圆点。
- **照片墙**：3 列以上的自适应宫格（固定 220px 列宽 + 整体居中），**数据库（`chou` 库）里上传的图片排在前面，本地 `assets/gallery` 的图片排在后面**；滚入视口时渐显、滚出视口后渐隐，鼠标悬停放大到 1.06 倍。
- **下滑动效**：滚动时首屏（轮播图 + 向下滑动提示）按滚动进度线性淡出并上移缩小，滚过约一屏后完全消失；背景那 7 个字同步变淡。
- **左上角「知己」点开右侧列表**：个人中心 / 退出登录 / 反馈·投诉作者，列表从右侧滑出，带遮罩，点遮罩或 × 关闭。
- 顶栏左侧是可点击的「知己」，右侧只显示问候语「你好，用户名」（未登录时显示「去登录」按钮）；**退出登录只放在「知己」菜单里**，顶栏不再重复放按钮。
- **背景音乐**：右下角圆形按钮（播放时音符转圈）。进入首页自动尝试播放，被自动播放策略拦截时提示「点击播放背景音乐」，在页面上点一下即可开始；点按钮可暂停 / 继续；音量 0.45，循环播放；**切走标签页、窗口失焦或离开首页时自动停止**，回来再续上。

### 个人中心 `/profile`

- 从首页左上角「知己」→「个人中心」进入；**左上角有返回按钮**回到首页。
- 展示头像、个人名字与知己号。
- **修改密码**：原密码 + 新密码 + 确认新密码（复用 `/api/resetPwd`）。
- **上传图片**：选文件即上传，图片以二进制存进数据库的 `chou.photo` 表（单张 ≤ 12MB，仅限图片类型）。
- **我的照片墙**：读取当前用户上传的图片并展示；上传成功后首页照片墙也会出现这张图。

## 目录结构

```
知己/
├─ backend/                                  Spring Boot 后端（8080）
│  ├─ .mvn/                                  项目内 Maven 设置（见文末说明）
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/mochou/zhiji/
│     │  ├─ ZhijiApplication.java            启动类
│     │  ├─ common/                         统一响应体、业务异常、全局异常处理
│     │  ├─ config/WebConfig.java            跨域配置
│     │  ├─ security/PasswordHasher.java     密码哈希（PBKDF2 加盐）
│     │  ├─ user/                            用户：控制器 / 服务 / 仓储 / DTO
│     │  ├─ photo/                           照片墙：上传 / 列表 / 取原图（chou 库）
│     │  └─ feedback/                        反馈与投诉
│     └─ resources/
│        ├─ application.yml                  端口、数据源、上传大小、跨域白名单
│        └─ schema.sql                       启动时自动建库建表（可重复执行）
├─ frontend/                                 Vue 3 + Vite 前端（5173）
│  ├─ index.html
│  ├─ vite.config.js                         /api 代理到 8080
│  ├─ public/favicon.svg
│  ├─ public/audio/bgm.mp3                   背景音乐（固定文件，不做自动收录）
│  └─ src/
│     ├─ App.vue                             根据路由渲染登录页 / 首页 / 个人中心
│     ├─ router.js                           极简路由（/login、/home、/profile），零依赖
│     ├─ session.js                          登录状态（sessionStorage）
│     ├─ photos.js                           自动收集 assets/gallery 里的图片
│     ├─ music.js                            背景音乐地址（固定指向 public/audio/bgm.mp3）
│     ├─ assets/gallery/                     本地图片放这里（当前 7 张）
│     ├─ views/LoginView.vue                 登录页外壳（满屏居中 + 背景光晕）
│     ├─ views/HomeView.vue                  首页（背景 7 字 + 轮播图 + 照片墙 + 菜单 + 音乐）
│     ├─ views/ProfileView.vue               个人中心（返回 / 改密码 / 上传 / 我的照片墙）
│     ├─ components/AuthCard.vue             登录 / 注册 / 修改密码卡片
│     ├─ components/PhotoCarousel.vue        轮播图组件
│     ├─ components/MusicToggle.vue          右下角背景音乐开关
│     ├─ components/FeedbackDialog.vue       反馈 / 投诉作者弹窗
│     ├─ api.js                              接口封装
│     ├─ style.css                           浅色系配色变量
│     └─ main.js
└─ scripts/
   ├─ start-mysql.ps1                        启动开发用 MySQL（普通权限即可）
   ├─ start-dev.ps1                          一键启动 MySQL + 后端 + 前端
   └─ test-api.ps1                           接口自测（20 项断言）
```

## 加图片 / 换音乐

- **本地图片**：丢进 `frontend/src/assets/gallery/`，首屏轮播和照片墙会自动收录（`import.meta.glob` 扫描该目录，按文件名排序）：支持 `jpg / jpeg / png / webp / avif / gif`。只有一张图时轮播不显示箭头和圆点，放到两张以上会自动开始轮播。
- **用户上传的图片**：走个人中心上传，存进数据库 `chou.photo` 表，展示在首页照片墙最前面。
- **音乐**：固定使用 `frontend/public/audio/bgm.mp3`（**不做自动收录**），换歌直接覆盖这个文件、刷新页面即可，文件名保持不变就不用改代码。

## 快速开始

环境要求：JDK 17、Maven 3.9+、Node 18+、MySQL 8（`mysql`/`mysqld` 命令可用）。

### 方式一：一键启动（推荐）

```powershell
cd D:\桌面\言\mochou\知己
powershell -ExecutionPolicy Bypass -File scripts\start-dev.ps1
```

脚本依次做三件事：启动 MySQL →（首次运行会自动 `mvn clean package` 构建后端）→ 分别弹出「知己-后端」「知己-前端」两个窗口。

### 方式二：三个窗口分别启动（看得最清楚）

```powershell
# 窗口 1 —— MySQL（首次运行会自动初始化数据目录，并把 root 密码设为 123456）
cd D:\桌面\言\mochou\知己
powershell -ExecutionPolicy Bypass -File scripts\start-mysql.ps1

# 窗口 2 —— 后端（等窗口 1 打印「完成：MySQL 已就绪」后再执行）
cd D:\桌面\言\mochou\知己\backend
mvn clean package -DskipTests
java -jar target\zhiji-backend-1.0.0.jar

# 窗口 3 —— 前端
cd D:\桌面\言\mochou\知己\frontend
npm install
npm run dev
```

看到 `Local: http://127.0.0.1:5173/` 后，浏览器打开 <http://127.0.0.1:5173>。

验证三个服务是否就绪：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test-api.ps1   # 后端 20 项断言
```

> **注意：本机不要用 `mvn spring-boot:run`。** 项目路径含中文（`桌面`、`知己`），该 goal 会报
> `ClassNotFoundException: com.mochou.zhiji.ZhijiApplication`（已实测，加不加 `-Dspring-boot.run.fork=false` 都一样），
> 而 `mvn clean package` + `java -jar` 完全正常。若确实想用 `spring-boot:run`，把项目挪到纯英文路径（如 `D:\projects\zhiji`）即可。

### 停止

- 前端 / 后端：关掉对应窗口，或 `Stop-Process -Name java,node`（会一并关掉其它 Java/Node 程序，慎用）。
- MySQL：`D:\shujuku\bin\mysqladmin.exe -h 127.0.0.1 -P 3306 -u root -p123456 shutdown`，或直接注销/重启。

### 换一台电脑跑

1. 装好 JDK 17、Maven、Node 18+、MySQL 8。
2. 改 `backend/src/main/resources/application.yml` 里的 `spring.datasource.username / password`，
   以及 `url` 中的地址端口（默认 `127.0.0.1:3306`）。
3. 数据库不用手工建：连不上库时 JDBC 的 `createDatabaseIfNotExist=true` 会自动建 `mochou` 库，
   启动时的 `schema.sql` 会自动建 `user` 表。
4. 若沿用 `scripts/start-mysql.ps1` 起独立实例，用 `-MySqlHome` 指定该机 MySQL 安装目录，例如：
   `powershell -ExecutionPolicy Bypass -File scripts\start-mysql.ps1 -MySqlHome "C:\Program Files\MySQL\MySQL Server 8.0"`。

## 接口

统一响应体，HTTP 状态码恒为 200，业务结果看 `code`：

```json
{ "code": 200, "msg": "登录成功", "data": { "id": 1, "username": "xxx" } }
```

`code`：200 成功、400 参数或业务错误、500 服务端异常。

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/register` | `{ "username": "...", "password": "..." }` | 注册 |
| POST | `/api/login` | `{ "username": "...", "password": "..." }` | 登录 |
| POST | `/api/resetPwd` | `{ "username": "...", "oldPassword": "...", "newPassword": "..." }` | 修改密码（登录页 / 个人中心都用它） |
| POST | `/api/photos` | multipart：`file` + `username` | 上传图片，存进 `chou.photo` |
| GET  | `/api/photos` | `?username=`（可选，只看某人的） | 照片墙列表（不含图片二进制） |
| GET  | `/api/photos/{id}/raw` | — | 图片原图（带 Content-Type，可直接放进 `img.src`） |
| POST | `/api/feedback` | `{ "content": "...", "username": "..." }` | 反馈 / 投诉作者 |
| GET  | `/api/health` | — | 健康检查 |

前端通过 Vite 的 `/api` 代理访问后端，浏览器侧同源、无跨域问题；如需直连后端，跨域白名单在 `application.yml` 的 `zhiji.cors.allowed-origins`。

## 业务规则

| 项 | 规则 | 落地位置 |
| --- | --- | --- |
| 用户名 | 非空、去首尾空格、**不超过 10 个字符**（按字符数，中文/emoji 各算 1 个） | `UserService.normalizeUsername` + 输入框 `maxlength` |
| 用户名 | **不可重复** | 先查重，再由 `user` 表唯一索引 `uk_user_username` 兜底（并发安全） |
| 密码 | 简易、无复杂度要求，仅要求非空（另设 64 字符安全上限） | `UserService.normalizePassword` |
| 密码 | 绝不明文存储，PBKDF2WithHmacSHA256 + 16 字节随机盐，存 `salt:hash` | `PasswordHasher` |

接口自测覆盖以上全部规则：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test-api.ps1
```

## 数据库

两个库，都在同一台 MySQL 上（同一个连接，跨库直接写 `库名.表名`）：

**`mochou`** —— 账号与反馈

```sql
CREATE TABLE `user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(10)     NOT NULL COMMENT '用户名：唯一、非空、最多10个字符',
  `password`    VARCHAR(200)    NOT NULL COMMENT 'PBKDF2 加盐哈希（salt:hash）',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `feedback` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(10)     NOT NULL DEFAULT '',
  `content`     VARCHAR(1000)   NOT NULL,
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

**`chou`** —— 照片墙（图片本体存这里）

```sql
CREATE TABLE `chou`.`photo` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(10)     NOT NULL COMMENT '上传者',
  `filename`     VARCHAR(255)    NOT NULL COMMENT '原始文件名',
  `content_type` VARCHAR(100)    NOT NULL COMMENT 'MIME 类型',
  `size_bytes`   INT UNSIGNED    NOT NULL,
  `data`         LONGBLOB        NOT NULL COMMENT '图片二进制内容',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_photo_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

建库建表无需手工执行：JDBC 连接串带 `createDatabaseIfNotExist=true` 会自动建 `mochou`，
启动时的 `schema.sql` 还会执行 `CREATE DATABASE IF NOT EXISTS chou` 并建好两张表，可重复运行。

## 常见问题

**为什么 `start-mysql.ps1` 要另起一个 MySQL 实例？**
本机原有的 `MYSQL82` 服务把数据目录放在 `C:\ProgramData\MySQL\MySQL Server 8.2\Data`，启动它需要管理员权限；脚本改用用户目录 `%USERPROFILE%\.mochou\mysql-data` 作为数据目录，普通权限即可运行，不影响原有数据和配置。若你已用管理员身份启动了 `MYSQL82`，可跳过此脚本（库名、账号密码保持一致）。

**Maven 报 `Could not create local repository at C:\Program Files\Java\repo`？**
本机全局 `conf/settings.xml` 把 `localRepository` 指到了不可写目录。项目里 `backend/.mvn/settings.xml` + `backend/.mvn/maven.config` 已把它改回默认的 `~/.m2/repository`，只要在 `backend` 目录下执行 `mvn` 即可，无需改动全局 Maven 安装。

**为什么不用 vue-router？**
本机 `package.json` 里 `vite` 是 8.x 而 `@vitejs/plugin-vue` 是 5.x，两者 peer 依赖冲突，任何 `npm install`（包括装 vue-router）都会报 `ERESOLVE`。本站只有登录页、首页和个人中心三个页面，`src/router.js` 用「一个响应式变量 + History API」实现了同样效果（真实 URL、浏览器前进后退都正常），也就不需要动依赖。以后页面多了想换 vue-router：先把 `@vitejs/plugin-vue` 升到与 Vite 8 匹配的版本，再 `npm install vue-router@4` 即可。

**端口被占用？**
后端 8080、前端 5173、MySQL 3306。改后端端口改 `backend/src/main/resources/application.yml`，改前端端口同时要改 `frontend/vite.config.js` 的代理目标。

**怎么清掉自测/联调产生的测试数据？**
`scripts\test-api.ps1` 和浏览器端到端测试每次运行都会注册随机测试账号（还会上传图片、提交反馈）。想只保留自己的账号，按用户名删掉其余数据即可（把 `1` 换成你的用户名）：

```sql
DELETE FROM mochou.user     WHERE username <> '1';
DELETE FROM mochou.feedback WHERE username <> '1';
DELETE FROM chou.photo      WHERE username <> '1';
```

**怎么部署？**
`cd frontend && npm run build` 产出 `frontend/dist`，交给 Nginx 托管并把 `/api` 反向代理到后端；或设置 `frontend/.env.production` 的 `VITE_API_BASE` 直连后端地址（参考 `.env.production.example`）。
因为是前端路由，Nginx 需要把未知路径回落到 `index.html`，否则直接刷新 `/home` 会 404：

```nginx
location / {
    root /path/to/frontend/dist;
    try_files $uri $uri/ /index.html;
}
```
