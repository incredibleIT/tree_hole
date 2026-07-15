---
title: "产品简报详情包：以外 RKOS"
type: llm-distillate
source: "product-brief-rkos.md"
created: "2026-07-15"
purpose: "供下游 PRD 创建使用的 token 高效上下文"
---

# 产品简报详情包：以外 RKOS

## 产品决策变更

- **一生三篇已去除：** 不再限制用户发布故事数量，用户可自由提交感情经历。原因：RKOS 一阶段需要大量知识沉淀，数量限制与目标冲突
- **隐私暂不涉及：** 用户明确表示当前阶段不考虑隐私策略，后续再补充
- **开放 API 加入愿景：** RKOS 知识图谱通过 API 对外开放，可被心理咨询师、研究者、其他产品调用

## RKOS 完整技术架构

### 系统架构图

```mermaid
graph TB
    subgraph 用户交互层
        A[用户输入<br/>故事 / 图片 / 聊天记录]
        B[用户接收<br/>Agent 建议 / 对话反馈]
    end

    subgraph 故事理解层
        C[Story Understanding Agent<br/>故事理解与信息抽取]
        D[Relationship Genome<br/>关系基因组 · 标准化结构]
    end

    subgraph 双存储层
        E[(MongoDB<br/>原始故事 · 图片 · 聊天记录<br/>非结构化数据)]
        F[(PostgreSQL<br/>Genome · Rule · Pattern<br/>结构化 · 可版本管理)]
    end

    subgraph 知识演化引擎 · 双路并行
        G[Embedding Service<br/>向量化服务]
        H[(Milvus 向量库<br/>Embedding + Metadata 索引)]
        I[Relationship Rule Agent<br/>规则挖掘 Rule Mining]
        J[Relationship Pattern Agent<br/>模式发现 Pattern Discovery]
    end

    subgraph 知识图谱层
        K[Knowledge Compiler<br/>知识编译器]
        L[(Neo4j 图数据库<br/>关系知识图谱 · 可解释可审计)]
    end

    subgraph 智能推理层
        M[Reasoning Planner<br/>推理规划器]
        N[Relationship AI Agent<br/>LLM 驱动 · 推理分析建议对话]
        O[Rule Ranking Engine<br/>按置信度和证据排序]
    end

    subgraph 反馈闭环
        P[Feedback Understanding Agent<br/>理解采纳结果与后续变化]
        Q[知识更新<br/>Genome 更新 · Rule Confidence 调整<br/>Pattern Weight 更新]
    end

    A --> C
    C --> D
    D --> E
    D --> F
    F --> G
    F --> I
    F --> J
    G --> H
    I --> K
    J --> K
    K --> L
    L --> M
    M --> N
    N --> O
    O --> B
    N -->|用户反馈| P
    P --> Q
    Q -->|回流更新| I
    Q -->|回流更新| J
    Q -->|更新向量索引| G
```

### 架构说明

- **故事输入层：** 用户上传故事（文字、图片、聊天记录）→ Story Understanding Agent 抽取信息 → 生成 Relationship Genome
- **双存储架构：**
  - MongoDB → 原始故事、图片、聊天记录等非结构化数据
  - PostgreSQL → 结构化 Genome、Rule、Pattern（可版本管理、可追溯）
- **知识演化引擎（双路并行）：**
  - Embedding Service → Milvus 向量库（语义检索，仅存 Embedding + Metadata 索引）
  - Knowledge Evolution Engine → Relationship Rule Agent（规则挖掘）+ Relationship Pattern Agent（模式发现）
- **知识图谱：** Knowledge Compiler 将规则和模式编译同步到 Neo4j 图数据库
- **智能推理层：**
  - Reasoning Planner → 规划需要检索哪些知识、制定推理流程
  - Relationship AI Agent (LLM) → 推理、分析、建议生成、对话
  - Rule Ranking Engine → 按置信度和证据排序规则
- **反馈闭环：** 用户是否采纳建议 → 是否成功挽回 → 后续关系变化 → Feedback Understanding Agent → 更新 Genome / Rule Confidence / Pattern Weight → 回到 Knowledge Evolution Engine

## 双阶段使用模式

- **一阶段（知识沉淀）：** 大量用户输入感情遗憾经历 → 走完整知识沉淀流程（理解 → 基因组生成 → 存储 → 规则挖掘 → 模式发现 → 图谱编译）
- **二阶段（智能建议）：** 用户描述当前感情现状 → RKOS 检索知识库 → Agent 提供基于真实案例的可靠解决建议 → 反馈回流

## 竞品详细分析

- **测测APP（5500万用户）：** 国内最大 AI 情感陪伴平台。AI 陪伴智能体"陪伴小星"，叙事疗法数字化，自研"心元大模型"已备案。走通用陪伴路线，无结构化关系知识。**启示：** 验证了"讲故事 + AI 理解"模式有真实需求
- **CoupleAI：** 上传聊天记录 → AI 分析关系动态。一次性分析，知识不沉淀
- **关系助手AI (iweaver.ai)：** 综合关系工具，包含戈特曼理论、依恋评估、爱情语言分析等框架。军级加密、HIPAA/GDPR 合规。功能丰富但无知识演化
- **爱情类型测试AI：** 60 道心理测评题 → 恋爱风格分析。静态问卷，无 AI 深度理解
- **AICA：** 基于心理学书籍训练的 AI 聊天机器人。角色扮演对话练习。无专属知识体系
- **Relationship Analyzer：** 匿名行为分析，模式识别。不构建个人知识图谱
- **连信数字"洞见人和"：** 1400+ 心理学概念、16000+ 规则的知识图谱，情绪识别准确率 97%。偏临床心理方向，非关系叙事方向

## 市场数据

- 2025 年全球 AI 情感陪伴市场规模约 1835 亿美元
- 具身智能虚拟人占比预计达 43%，年复合增长率 41.3%
- AI 心理治疗正从"通用聊天"向"专业知识服务"升级
- 知识图谱 + 心理学已有学术基础（同济大学情绪事件知识图谱专利等）

## 差异化核心论点

- **关系基因组是独一无二的：** 市面上没有产品将人类关系模式结构化为可查询、可演化的知识系统
- **知识闭环是真正的护城河：** 竞品的建议都是一次性的，RKOS 让知识随使用而成长（规则置信度调整、模式权重更新）
- **显式知识 vs 隐式知识：** 竞品的知识在模型权重中（黑盒），RKOS 的知识在图谱中（可解释、可审计）

## 已有前端技术栈

- 前端：React + Vite + TypeScript
- 动画：gsap（含 ScrollTrigger）
- 3D：Three.js + @react-three/fiber + @react-three/drei + @react-three/rapier
- 已集成组件：Lanyard（挂绳卡片）、ChromaGrid（聚光灯网格）、LightRays（光线效果）、InfiniteMenu（3D 球体菜单）、ProfileCard（个人资料卡）
- 部署：GitHub Pages（静态前端）
- 后端规划：FastAPI（Python）

## 产品约束与上下文

- **项目性质：** 公益开源，单人独立开发维护
- **核心定位：** 讲述与错过的他/她之间不可替代的故事
- **情感核心：** "世间好多遗憾，一切都有替代，但除了你以外"
- **冷启动策略：** 先上线概念验证网站（每天展示一篇故事，只读不写），用稀缺感引爆传播，再开放写作功能
- **Web 优先：** 以 Web 应用为第一形态，后续根据数据决定是否做 App
- **成功标准：** 技术影响力（开源社区采用、知识图谱规模、建议可信度、知识演化验证）

## MVP 范围信号

**第一版包含：**
- Story Understanding Agent（故事理解与信息抽取）
- Relationship Genome 生成与存储
- 基础知识演化引擎（Rule Mining + Pattern Discovery）
- Knowledge Retrieval + Relationship AI Agent（检索与建议生成）
- 反馈闭环最小实现

**第一版不包含：**
- 多模态深度分析（图片/语音情感识别）
- 实时对话式建议（先做异步分析）
- 商业化功能

## 开放问题

- 关系基因组的实际维度/特征尚未定义（依恋模式、沟通风格、冲突类型等仅为初步设想）
- 一阶段知识沉淀的用户获取策略未明确（如何吸引足够多用户贡献数据）
- 开放 API 的权限控制和定价策略（开源免费 vs 分级授权）
- 反馈闭环的用户交互设计（如何低摩擦地收集"是否采纳"和"后续结果"）
- 前后端技术栈是否调整（当前前端 React + Vite，后端规划 FastAPI，但 RKOS 架构涉及 MongoDB + PostgreSQL + Milvus + Neo4j 四种存储）
