# 基于 Spring Boot 的零售银行数据分析系统 (RBDAS)

本项目是一套专为零售银行数字化转型设计的精细化运营分析平台。系统采用 **Java 17 (业务逻辑)** 与 **Python 3 (机器学习引擎)** 异构协同架构，实现了从数据导入到 AI 决策的全流程闭环。

## 🚀 核心技术亮点
- **AI 客户分群**：集成 Scikit-learn 的 **K-Means 聚类算法**，支持动态 K 值训练，并引入 **StandardScaler 标准化处理** 与 **轮廓系数 (Silhouette)** 评估模型。
- **高维数据可视化**：基于 ECharts 实现客户资产空间分布散点图、近6个月交易趋势折线图及业务规模柱状图。
- **金融级安全**：由简易 Token 升级为标准的 **JWT (JSON Web Token)** 鉴权，并对用户密码执行 **BCrypt 强哈希加密**。
- **高性能架构**：引入 **Redis 缓存机制** 存储 Dashboard 统计数据，响应耗时优化至 20ms 以内。
- **自动化审计**：利用 **Spring AOP** 实现非侵入式日志记录，自动追踪用户操作及方法执行时长。
- **标准化报表**：集成 **iText 7** 库，支持一键导出包含业务概览与风险名单的 PDF 审计报告。

## 📂 业务功能模块
1. **数据集成中心**：支持客户基础表与流水表并排实时导入，具备自动数据清洗功能。
2. **AI 分群分析**：支持 K=2~5 的动态聚类，提供分群特征的“语义化解读”表格。
3. **风险预警监控**：规则引擎自动识别高负债、大额支出及僵尸账户风险，支持风控决策。
4. **业务综合看板**：汇总全行资产、贷款及交易趋势，提供营销转化数据分析。
5. **系统维护管理**：完善的 RBAC 角色体系（ADMIN, ANALYST, RISK）及操作日志追踪。

## 🛠️ 技术栈
- **后端**：Spring Boot 3.4.1, MyBatis-Plus, Spring Security, JWT, iText 7
- **前端**：Vue 3, Element Plus, ECharts, Axios
- **算法**：Python 3.x, Scikit-learn, Pandas, Openpyxl
- **存储**：MySQL 8.0, Redis 5.0+

## 📥 快速启动
1. 启动 Redis 服务与 MySQL。
2. 修改 `application.yml` 数据库配置及 `AnalysisController.java` 中的 Python 解释器路径。
3. 后端启动：运行 `RetailBankAnalysisSystemApplication` (内置 DataInitializer 自动创建 admin/123456)。
4. 前端启动：`cd frontend && npm install && npm run serve`。

# 基于 Spring Boot 的零售银行数据分析系统 (RBDAS)

本项目是一套专为零售银行数字化转型设计的精细化运营分析平台。系统采用 **Java 17 (业务逻辑)** 与 **Python 3 (机器学习引擎)** 异构协同架构，实现了从数据导入到 AI 决策的全流程闭环。

## 🚀 核心技术亮点
- **AI 客户分群**：集成 Scikit-learn 的 **K-Means 聚类算法**，支持动态 K 值训练，并引入 **StandardScaler 标准化处理** 与 **轮廓系数 (Silhouette)** 评估模型。
- **高维数据可视化**：基于 ECharts 实现客户资产空间分布散点图、近6个月交易趋势折线图及业务规模柱状图。
- **金融级安全**：由简易 Token 升级为标准的 **JWT (JSON Web Token)** 鉴权，并对用户密码执行 **BCrypt 强哈希加密**。
- **高性能架构**：引入 **Redis 缓存机制** 存储 Dashboard 统计数据，响应耗时优化至 20ms 以内。
- **自动化审计**：利用 **Spring AOP** 实现非侵入式日志记录，自动追踪用户操作及方法执行时长。
- **标准化报表**：集成 **iText 7** 库，支持一键导出包含业务概览与风险名单的 PDF 审计报告。

## 📂 业务功能模块
1. **数据集成中心**：支持客户基础表与流水表并排实时导入，具备自动数据清洗功能。
2. **AI 分群分析**：支持 K=2~5 的动态聚类，提供分群特征的“语义化解读”表格。
3. **风险预警监控**：规则引擎自动识别高负债、大额支出及僵尸账户风险，支持风控决策。
4. **业务综合看板**：汇总全行资产、贷款及交易趋势，提供营销转化数据分析。
5. **系统维护管理**：完善的 RBAC 角色体系（ADMIN, ANALYST, RISK）及操作日志追踪。

## 🛠️ 技术栈
- **后端**：Spring Boot 3.4.1, MyBatis-Plus, Spring Security, JWT, iText 7
- **前端**：Vue 3, Element Plus, ECharts, Axios
- **算法**：Python 3.x, Scikit-learn, Pandas, Openpyxl
- **存储**：MySQL 8.0, Redis 5.0+

## 📥 快速启动
1. 启动 Redis 服务与 MySQL。
2. 修改 `application.yml` 数据库配置及 `AnalysisController.java` 中的 Python 解释器路径。
3. 后端启动：运行 `RetailBankAnalysisSystemApplication` (内置 DataInitializer 自动创建 admin/123456)。
4. 前端启动：`cd frontend && npm install && npm run serve`。
