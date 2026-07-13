---
stepsCompleted: ["step-1-init", "step-2-context", "step-3-starter", "step-4-decisions", "step-5-patterns", "step-6-structure", "step-7-validation", "step-8-complete"]
lastStep: 8
status: 'complete'
completedAt: '2026-07-10'
workflowType: 'architecture'
project_name: '以外'
user_name: 'Yang'
date: '2026-07-10'
---

# 架构决策文档

_本文档通过逐步协作发现共同构建。章节随架构决策推进逐步追加。_

## 项目上下文分析

### 需求概览

**功能需求：**
43 条功能需求覆盖 8 个能力域。架构上可归纳为三个核心子系统：
1. **内容分发系统** — 概念验证阶段每日一篇（管理员手动排序），第二阶段随机展示（算法简单，无推荐引擎）
2. **用户与创作系统** — 匿名身份、草稿箱（本地+服务端双重保存）、一生三篇计数与永久锁定
3. **仪式引擎** — 精确状态机控制（静默→三问→确认→音效→动画），支持中途退出和恢复

**非功能需求：**
24 条非功能需求中，对架构影响最大的是：
- 落笔后故事不可删除/修改（NFR10）→ 需要在数据库层实现硬约束
- 零丢失（NFR11）→ 需要复制 + 每日备份策略
- 独立开发者维护 + 自动部署（NFR22-23）→ 必须选择低运维成本的托管方案

**规模与复杂度：**
- 主要领域：Web 应用（SPA）
- 复杂度级别：低
- 预估架构组件：4-6 个（前端应用、API 服务、数据库、邮件服务、文件存储、部署管道）

### 技术约束与依赖

- 独立开发者（Yang）维护，技术选型偏向低学习曲线和低运维成本
- 全新项目，无历史代码包袱
- 概念验证阶段无用户系统，第二阶段才引入注册
- 仪式动画需要客户端精确状态控制，可能限制服务端渲染选项

### 跨切关注点

- **永久数据完整性**：贯穿存储、备份、API 设计——落笔后的数据必须视为不可变记录
- **仪式状态机**：前端核心逻辑，需要防篡改和中断恢复
- **反社交设计约束**：API 和数据模型刻意不包含评论、关注、搜索等表结构
- **草稿自动保存**：本地缓存与服务端同步的一致性保障
- **可访问性降级**：动画和音效必须全局支持 `prefers-reduced-motion` 和用户静音

## 启动器模板评估

### 主要技术领域

**前后端分离架构** — React SPA + Python FastAPI + MySQL + Docker Compose

### 评估的启动器方案

| 方案 | 评估 | 结论 |
|------|------|------|
| Next.js 全栈 | 强制 Node.js 运行时，与 Python 后端偏好冲突；仪式动画需要完全客户端控制，SSR 增加复杂度 | 不推荐 |
| Vite + React（纯前端）+ FastAPI（纯后端） | 前后端完全独立，前端 SPA 支持动画精确控制，后端 Python 独立部署 | **推荐** |
| T3 Stack（Next.js + tRPC + Prisma） | TypeScript 全栈，后端锁定 Node.js，不符合 Python 偏好 | 不推荐 |

### 选定方案：Vite + React + TypeScript + FastAPI + MySQL

**选择理由：**

1. **前后端分离**：前端专注仪式动画和状态机，后端专注业务逻辑和数据持久化
2. **SPA 完全控制客户端**：落笔仪式的精确状态机控制需要完整客户端能力
3. **独立开发者友好**：两个独立模块可独立部署和升级，互不影响
4. **MySQL 持久可靠**：满足故事数据零丢失和永久存储需求
5. **Docker Compose 一键部署**：开发和生产环境一致

**初始化命令：**

前端：
```bash
pnpm create vite yiwai-frontend --template react-ts
```

后端：
```bash
mkdir yiwai-backend && cd yiwai-backend
python -m venv venv && source venv/bin/activate
pip install "fastapi[standard]" sqlalchemy pymysql alembic
```

**启动器提供的架构决策：**

| 决策维度 | 选择 | 理由 |
|----------|------|------|
| 语言 | TypeScript（前端）+ Python（后端） | 类型安全 + 开发效率 |
| 构建工具 | Vite | 极速热更新，生产构建优化 |
| 样式方案 | Tailwind CSS | 原子化 CSS，适合极简设计 |
| 状态管理 | Zustand 或 React Context | 轻量级，满足仪式状态机需求 |
| 路由 | React Router | SPA 标准路由 |
| API 通信 | Axios 或 fetch | 前后端分离标准方案 |
| 数据库 ORM | SQLAlchemy 2.0 | Python 成熟 ORM，支持异步 |
| 数据库迁移 | Alembic | 版本化数据库变更 |
| 部署 | Docker Compose | 一键启动全栈开发环境 |

## 核心架构决策

### 已确定的决策（来自启动器评估）

- 前端：Vite + React + TypeScript + Tailwind CSS
- 后端：FastAPI + Python + SQLAlchemy 2.0 + Alembic
- 数据库：MySQL
- 部署：Docker Compose

### 数据架构

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 数据建模 | 关系型表结构（用户、故事、草稿、等待列表、共鸣） | 简单清晰，MySQL 原生支持 |
| 落笔后不可变 | 数据库层硬约束（`is_published` 标志 + 应用层校验，无 UPDATE/DELETE API） | 满足 NFR10 |
| 缓存策略 | 概念验证阶段不做服务端缓存；第二阶段可加 Redis 缓存每日故事 | 低复杂度，独立开发者友好 |
| 草稿保存 | 前端 LocalStorage 自动保存 + 服务端定期同步（第二阶段） | 概念验证阶段草稿仅存本地 |

### 认证与安全

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 认证方式 | JWT 令牌（邮箱 + 验证码登录，无密码） | 概念验证阶段无用户系统；第二阶段用邀请码 + 邮箱验证 |
| 密码处理 | 不适用（无密码设计） | 极简认证，降低安全风险 |
| 邮箱加密 | bcrypt 哈希存储 | 满足 NFR6 |
| 管理员认证 | 独立 JWT + 环境变量中的管理员邮箱白名单 | 满足 NFR8 |
| API 安全 | HTTPS + CORS 白名单 + 速率限制 | 基础安全防线 |

### API 与通信模式

| 决策项 | 选择 | 理由 |
|--------|------|------|
| API 模式 | RESTful | 简单直接，FastAPI 自动生成 OpenAPI 文档 |
| 错误处理 | 标准 HTTP 状态码 + JSON 错误体 `{code, message}` | 前后端统一规范 |
| 速率限制 | 基础速率限制（每分钟 60 次请求） | 防止滥用，概念验证阶段足够 |
| 邮箱服务 | Resend（REST API） | 轻量级，Python SDK 简单 |

### 前端架构

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 状态管理 | Zustand | 轻量级，适合仪式状态机的精确控制 |
| 组件架构 | 函数组件 + 自定义钩子（仪式逻辑、音频管理） | 符合 React 现代最佳实践 |
| 动画方案 | CSS 动画 + Framer Motion（散步动画） | CSS 动画轻量，Framer Motion 处理复杂序列 |
| 音频管理 | Web Audio API + 预加载 | 满足 NFR5 |
| 路由策略 | React Router，路由级代码分割（React.lazy） | SPA 标准方案 + 性能优化 |

### 基础设施与部署

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 代码仓库 | 单一仓库（monorepo），`frontend/` + `backend/` 目录 | 独立开发者管理方便 |
| 持续集成 | GitHub Actions（代码检查 + 构建测试） | 与 GitHub 开源托管一致 |
| 持续部署 | 推送到主分支自动构建 Docker 镜像并部署 | 满足 NFR23 |
| 监控日志 | Python logging + Sentry（错误追踪） | 满足 NFR24，独立开发者成本可控 |
| 环境配置 | `.env` 文件（开发/生产分离） | 标准方案 |
| 反向代理 | Nginx（前端静态文件 + API 代理） | Docker Compose 内统一管理 |

## 实现模式与一致性规则

### 命名模式

**数据库命名（Python/MySQL 侧）：**

| 规则 | 约定 | 示例 |
|------|------|------|
| 表名 | 小写复数蛇形 | `users`, `stories`, `waitlist_entries` |
| 列名 | 小写蛇形 | `user_id`, `created_at`, `is_published` |
| 外键 | `{关联表单数}_id` | `user_id`, `story_id` |
| 索引 | `idx_{表名}_{列名}` | `idx_users_email`, `idx_stories_published` |
| 布尔列 | `is_` / `has_` 前缀 | `is_published`, `is_graduated`, `has_resonated` |

**API 命名（RESTful）：**

| 规则 | 约定 | 示例 |
|------|------|------|
| 端点 | 小写复数名词 | `/api/stories`, `/api/users`, `/api/waitlist` |
| 路径参数 | 蛇形 | `/api/stories/{story_id}` |
| 查询参数 | 蛇形 | `?page=1&sort=created_at` |

**前端代码命名（TypeScript/React）：**

| 规则 | 约定 | 示例 |
|------|------|------|
| 组件 | PascalCase | `StoryCard`, `CeremonyFlow` |
| 文件 | PascalCase（组件）/ camelCase（工具） | `StoryCard.tsx`, `useAudio.ts` |
| 函数/变量 | camelCase | `getStory`, `userName` |
| 钩子 | `use` 前缀 camelCase | `useCeremony`, `useAutoSave` |
| 常量 | UPPER_SNAKE_CASE | `MAX_STORIES`, `CEREMONY_DURATION` |
| 类型/接口 | PascalCase + Props/Response 后缀 | `StoryProps`, `ApiResponse` |

**后端代码命名（Python）：**

| 规则 | 约定 | 示例 |
|------|------|------|
| 函数/变量 | 蛇形 | `get_story`, `user_id` |
| 类 | PascalCase | `StoryService`, `WaitlistRepository` |
| 常量 | UPPER_SNAKE_CASE | `MAX_STORIES`, `DATABASE_URL` |

### 结构模式

**项目组织（单一仓库）：**

```
yiwai/
├── frontend/          # Vite + React 前端
│   ├── src/
│   │   ├── components/    # 按功能域分组的 UI 组件
│   │   ├── hooks/         # 自定义 React 钩子
│   │   ├── stores/        # Zustand 状态存储
│   │   ├── services/      # API 调用层
│   │   ├── types/         # TypeScript 类型定义
│   │   └── utils/         # 工具函数
│   └── public/            # 静态资源
├── backend/           # FastAPI 后端
│   ├── app/
│   │   ├── api/           # 路由端点
│   │   ├── models/        # SQLAlchemy 模型
│   │   ├── schemas/       # Pydantic 模式
│   │   ├── services/      # 业务逻辑层
│   │   ├── repositories/  # 数据访问层
│   │   └── core/          # 配置、安全、中间件
│   └── tests/
├── docker-compose.yml
└── nginx/
```

**组件组织原则：**
- 按功能域分组：`components/ceremony/`, `components/story/`, `components/admin/`
- 共享组件放 `components/shared/`
- 每个组件目录包含：组件文件 + 样式 + 类型 + 测试

### 格式模式

**API 响应格式：**

```json
// 成功响应
{
  "data": { },
  "meta": { "page": 1, "total": 10 }
}

// 错误响应
{
  "error": {
    "code": "STORY_NOT_FOUND",
    "message": "故事不存在或已被合上"
  }
}
```

**数据交换格式：**
- JSON 字段命名：蛇形（`snake_case`），与 Python 后端一致
- 日期时间：ISO 8601 字符串（`2026-07-10T12:00:00Z`）
- 布尔值：`true` / `false`（JSON 原生）
- 空值：`null`

### 过程模式

**错误处理：**
- 后端：FastAPI 异常处理器统一捕获，返回标准 `{error}` 格式
- 前端：API 服务层统一处理，组件层只关心 `data` / `error` / `loading` 三态
- 用户提示：中文友好文案，不暴露技术细节

**加载状态：**
- 三态模式：`loading` → `success` | `error`
- 仪式流程：使用 Zustand 状态机管理（`idle` → `silence` → `questions` → `confirm` → `publish` → `walk`）

**草稿自动保存：**
- 防抖策略：输入停止 2 秒后触发保存
- 保存优先级：LocalStorage → 服务端（第二阶段）

### 强制执行规则

**所有 AI 智能体必须遵守：**
1. 数据库表名和列名使用蛇形命名
2. API 端点使用复数名词蛇形
3. 前端组件使用 PascalCase，变量使用 camelCase
4. API 响应统一使用 `{data}` / `{error}` 格式
5. JSON 字段统一使用蛇形命名
. 落笔后的故事不提供 UPDATE/DELETE 端点
7. 所有时间戳使用 UTC + ISO 8601

## 项目结构与边界定义

### 完整项目目录结构

```
yiwai/
├── .github/
│   └── workflows/
│       ├── ci.yml                    # 代码检查 + 构建测试
│       └── deploy.yml                # 主分支自动部署
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── ceremony/             # 落笔仪式相关组件
│   │   │   │   ├── SilenceTimer.tsx      # 一分钟静默倒计时
│   │   │   │   ├── ThreeQuestions.tsx    # 三问确认流程
│   │   │   │   ├── PublishAnimation.tsx  # 笔落纸声 + 散步动画
│   │   │   │   └── CeremonyFlow.tsx     # 仪式状态机容器
│   │   │   ├── story/                # 故事展示组件
│   │   │   │   ├── StoryCard.tsx         # 单篇故事卡片
│   │   │   │   ├── StoryReader.tsx       # 阅读页面
│   │   │   │   └── ResonanceButton.tsx   # 共鸣按钮（光点动画）
│   │   │   ├── writing/              # 写作相关组件
│   │   │   │   ├── DraftEditor.tsx       # 草稿编辑器
│   │   │   │   └── DraftList.tsx         # 草稿箱列表
│   │   │   ├── admin/                # 管理后台组件
│   │   │   │   ├── StoryQueue.tsx        # 故事队列管理
│   │   │   │   ├── WaitlistView.tsx      # 等待列表查看
│   │   │   │   └── Dashboard.tsx         # 数据看板
│   │   │   ├── auth/                 # 认证组件
│   │   │   │   ├── InviteForm.tsx        # 邀请码输入
│   │   │   │   └── EmailVerify.tsx       # 邮箱验证
│   │   │   ├── onboard/              # 引导组件
│   │   │   │   └── FirstVisitGuide.tsx   # "你心里有没有一个无法替代的人？"
│   │   │   └── shared/               # 共享组件
│   │   │       ├── Layout.tsx            # 页面布局
│   │   │       ├── Loading.tsx           # 加载状态
│   │   │       └── ErrorBoundary.tsx     # 错误边界
│   │   ├── hooks/
│   │   │   ├── useCeremony.ts            # 仪式状态机逻辑
│   │   │   ├── useAudio.ts               # 音频播放管理
│   │   │   ├── useAutoSave.ts            # 草稿自动保存
│   │   │   ├── useStory.ts               # 故事数据获取
│   │   │   └── useAuth.ts                # 认证状态管理
│   │   ├── stores/
│   │   │   ├── ceremonyStore.ts          # 仪式状态（Zustand）
│   │   │   ├── userStore.ts              # 用户状态
│   │   │   └── storyStore.ts             # 故事状态
│   │   ├── services/
│   │   │   ├── api.ts                    # Axios 实例配置
│   │   │   ├── storyService.ts           # 故事 API
│   │   │   ├── authService.ts            # 认证 API
│   │   │   ├── waitlistService.ts        # 等待列表 API
│   │   │   └── adminService.ts           # 管理 API
│   │   ├── types/
│   │   │   ├── story.ts                  # 故事类型定义
│   │   │   ├── user.ts                   # 用户类型定义
│   │   │   └── api.ts                    # API 响应类型
│   │   ├── utils/
│   │   │   ├── storage.ts                # LocalStorage 工具
│   │   │   └── format.ts                 # 日期/文本格式化
│   │   ├── pages/
│   │   │   ├── Home.tsx                  # 首页（故事展示）
│   │   │   ├── Write.tsx                 # 写作页面
│   │   │   ├── Ceremony.tsx              # 仪式页面
│   │   │   ├── Admin.tsx                 # 管理后台
│   │   │   └── Invite.tsx                # 邀请注册
│   │   ├── App.tsx                       # 根组件 + 路由配置
│   │   ├── main.tsx                      # 入口文件
│   │   └── index.css                     # 全局样式 + Tailwind
│   ├── public/
│   │   ├── audio/
│   │   │   └── pen-drop.mp3              # 笔落纸声
│   │   └── fonts/                        # 自定义字体（手写体预留）
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── package.json
│   ├── Dockerfile
│   └── .env.example
├── backend/
│   ├── app/
│   │   ├── main.py                       # FastAPI 应用入口
│   │   ├── api/
│   │   │   ├── router.py                 # 路由聚合
│   │   │   ├── stories.py                # /api/stories
│   │   │   ├── waitlist.py               # /api/waitlist
│   │   │   ├── auth.py                   # /api/auth
│   │   │   └── admin.py                  # /api/admin
│   │   ├── models/
│   │   │   ├── base.py                   # SQLAlchemy 基类
│   │   │   ├── story.py                  # 故事模型
│   │   │   ├── user.py                   # 用户模型
│   │   │   ├── draft.py                  # 草稿模型
│   │   │   ├── waitlist.py               # 等待列表模型
│   │   │   └── resonance.py              # 共鸣记录模型
│   │   ├── schemas/
│   │   │   ├── story.py                  # 故事 Pydantic 模式
│   │   │   ├── user.py                   # 用户模式
│   │   │   └── waitlist.py               # 等待列表模式
│   │   ├── services/
│   │   │   ├── story_service.py          # 故事业务逻辑
│   │   │   ├── auth_service.py           # 认证业务逻辑
│   │   │   ├── waitlist_service.py       # 等待列表逻辑
│   │   │   └── email_service.py          # 邮件发送逻辑
│   │   ├── repositories/
│   │   │   ├── story_repo.py             # 故事数据访问
│   │   │   ├── user_repo.py              # 用户数据访问
│   │   │   └── waitlist_repo.py          # 等待列表数据访问
│   │   ├── core/
│   │   │   ├── config.py                 # 配置管理
│   │   │   ├── security.py               # JWT + 加密
│   │   │   ├── database.py               # 数据库连接
│   │   │   └── exceptions.py             # 统一异常处理
│   │   └── middleware/
│   │       ├── rate_limit.py             # 速率限制
│   │       └── cors.py                   # CORS 配置
│   ├── alembic/
│   │   ├── env.py                        # Alembic 配置
│   │   └── versions/                     # 迁移文件
│   ├── alembic.ini
│   ├── requirements.txt
│   ├── Dockerfile
│   └── .env.example
├── nginx/
│   ├── nginx.conf                        # Nginx 主配置
│   └── Dockerfile
├── docker-compose.yml                    # 全栈编排
├── docker-compose.dev.yml                # 开发环境覆盖
├── .gitignore
└── .env.example                          # 环境变量模板
```

### 架构边界

**API 边界：**

| 端点组 | 路径 | 认证 | 说明 |
|--------|------|------|------|
| 故事 | `/api/stories` | 可选 | 概念验证：公开；第二阶段：需登录 |
| 等待列表 | `/api/waitlist` | 无 | 邮箱收集，公开 |
| 认证 | `/api/auth` | 无 | 邮箱验证、JWT 发放 |
| 管理 | `/api/admin` | 管理员 JWT | 故事队列、数据看板 |

**组件通信模式：**
- 页面组件 → 钩子 → 服务层 → API
- 状态管理：Zustand 存储 ←→ 组件（订阅模式）
- 仪式状态机：`CeremonyFlow` 组件内 Zustand 管理，不跨页面共享

**数据流：**
```
用户操作 → React 组件 → Zustand 状态 → API 服务 → FastAPI → 数据库
                                                  ↓
                              Resend（邮件）← 邮箱服务
```

### 需求到结构映射

| PRD 功能域 | 前端位置 | 后端位置 |
|-----------|---------|----------|
| 内容展示（FR1-5） | `components/story/`, `pages/Home.tsx` | `api/stories.py`, `story_service.py` |
| 等待列表（FR6-9） | `components/auth/`, `pages/Invite.tsx` | `api/waitlist.py`, `waitlist_service.py` |
| 用户身份（FR10-14） | `hooks/useAuth.ts`, `stores/userStore.ts` | `api/auth.py`, `auth_service.py` |
| 内容创作（FR15-21） | `components/writing/`, `pages/Write.tsx` | `api/stories.py`, `story_service.py` |
| 落笔仪式（FR22-27） | `components/ceremony/`, `pages/Ceremony.tsx` | `api/stories.py`（落笔端点） |
| 社交互动（FR28-35） | `components/story/ResonanceButton.tsx` | `api/stories.py`（共鸣端点） |
| 运营管理（FR37-40） | `components/admin/`, `pages/Admin.tsx` | `api/admin.py` |

### 集成点

**外部集成：**
- Resend API → 邮件发送（邀请、确认）
- GitHub → 代码托管 + CI/CD

**内部通信：**
- 前端 ↔ 后端：HTTP REST（JSON）
- 后端 ↔ MySQL：SQLAlchemy ORM
- Nginx → 前端静态文件 + API 反向代理

## 架构验证结果

### 一致性验证 ✅

**决策兼容性：**
所有技术选型（Vite + React + TypeScript / FastAPI + Python / MySQL / Docker Compose / Nginx）完全兼容，前后端通过 REST API 解耦，无技术冲突。Zustand 状态管理与 SPA 架构和仪式状态机需求完美匹配。

**模式一致性：**
命名约定跨前后端统一（蛇形数据库/API，PascalCase 组件，camelCase 变量）。JSON 字段统一蛇形命名，API 响应格式 `{data}` / `{error}` 全局统一。

**结构对齐：**
项目目录结构与功能域划分完全对应，后端三层架构（API → Service → Repository）职责分明，集成点明确定义。

### 需求覆盖验证 ✅

**功能需求覆盖（43 条 FR，8 个能力域）：**

| 能力域 | 架构支撑 | 状态 |
|--------|---------|------|
| 内容展示（FR1-5） | `components/story/`, `story_service.py` | ✅ |
| 等待列表（FR6-9） | `components/auth/`, `waitlist_service.py` | ✅ |
| 用户身份（FR10-14） | `hooks/useAuth.ts`, `auth_service.py` | ✅ |
| 内容创作（FR15-21） | `components/writing/`, `story_service.py` | ✅ |
| 落笔仪式（FR22-27） | `components/ceremony/`, Zustand 状态机 | ✅ |
| 社交互动（FR28-35） | `ResonanceButton.tsx`, 共鸣端点 | ✅ |
| 可访问性（FR36） | CSS `prefers-reduced-motion` + 用户静音 | ✅ |
| 运营管理（FR37-40） | `components/admin/`, `admin.py` | ✅ |

**非功能需求覆盖（24 条 NFR）：**

| 维度 | 架构支撑 | 状态 |
|------|---------|------|
| 性能 | 概念验证无缓存 + Redis 扩展预留 | ✅ |
| 安全 | JWT + bcrypt + HTTPS + CORS + 速率限制 | ✅ |
| 可靠性 | 不可变记录 + MySQL 复制 + 每日备份 | ✅ |
| 可用性 | 极简 UI + 响应式 + 中文友好 | ✅ |
| 可维护性 | 单一仓库 + 清晰分层 + 类型安全 | ✅ |
| 运维 | Docker + GitHub Actions + Sentry + 日志 | ✅ |

### 实现就绪度验证 ✅

- 五个决策类别全面覆盖，实现模式足够详细
- 7 条强制执行规则防止 AI 智能体冲突
- 完整目录树从根目录到具体文件已定义
- 组件边界清晰，集成点全部映射

### 差距分析

**关键差距（阻塞实现）：** 无

**重要差距（不阻塞实现，建议后续补充）：**
1. 数据库 ER 关系详细定义
2. 仪式状态机中间态（中断恢复、超时处理）细化
3. 环境变量清单明确列出

**未来增强方向：**
1. 第二阶段引入 Redis 缓存和草稿服务端同步
2. 前端设计系统规范（色彩/字体/间距）
3. 测试策略详细规划和性能基准指标

### 架构就绪度评估

**整体状态：** 可以开始实现

**信心级别：** 高

**核心优势：**
1. 前后端分离清晰，适合独立开发者并行推进
2. 技术选型成熟稳定，学习曲线低
3. 反社交设计约束在数据模型层强制执行，防止架构蔓延
4. 概念验证→第二阶段路线明确，架构扩展性良好

### 实现交接指引

**AI 智能体指引：**
- 严格按照文档中的架构决策实现
- 使用实现模式保持一致性
- 尊重项目结构和边界
- 所有架构问题参考本文档

**第一实现优先级：**
1. 初始化前后端项目（Vite + FastAPI）
2. 配置 Docker Compose 开发环境
3. 实现核心数据模型（故事、等待列表）
4. 搭建基础 API 端点
