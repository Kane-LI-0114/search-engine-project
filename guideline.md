# CSIT5930 搜索引擎项目 代码生成全量指令规范
## 核心执行目标
为香港科技大学CSIT5930课程开发**完整可运行、100%符合作业要求、可直接编译部署复现结果**的Web搜索引擎。你必须严格遵循本指令的所有规范、约束和功能要求，生成标准化的Java项目代码，不得偏离、遗漏或修改作业硬性要求。

---

## 一、项目合规红线（必须100%遵守，违反则项目不合格）
以下为作业明确规定的不可突破规则，所有代码实现必须严格遵循：
1.  禁止将索引、链接关系等核心数据存储在内存数组/HashMap等主存数据结构中，必须使用JDBM库进行磁盘持久化存储
2.  爬虫必须使用**广度优先BFS策略**，从指定起始URL抓取固定300个页面
3.  必须实现**正文、标题两个完全独立的倒排索引文件**
4.  必须支持双引号包裹的短语搜索，标题和正文均需完整支持
5.  检索排序必须严格使用作业指定公式：`TF-IDF = (tf * idf) / max(tf)`，文档相似度必须使用**余弦相似度**计算
6.  必须实现标题匹配加权机制，标题中的匹配项需显著提升页面排名
7.  必须使用JSP实现Web交互界面，严格遵循作业指定的结果展示格式
8.  文本预处理必须使用课程Lab3提供的**英文停用词表**、**Java版Porter词干提取算法**
9.  必须完整处理循环链接、页面更新校验、无最后修改时间/页面大小的兜底逻辑
10. 最终交付仅提交源代码、README、演示PPT大纲，禁止提交数据库文件

---

## 二、技术栈与依赖规范（固定不可修改）
| 组件 | 技术选型 | 版本要求 | 对应作业要求 |
|------|----------|----------|--------------|
| 开发语言 | Java | 8+ 兼容课程环境 | 作业推荐Java实现 |
| Web框架 | JSP + Servlet | 兼容Tomcat 9+ | 作业明确要求JSP实现查询传递 |
| 依赖管理 | Maven | 3.6+ | 统一管理依赖，确保可复现 |
| HTML解析 | htmlparser | 2.1 | 作业指定http://htmlparser.sourceforge.net/ 库 |
| 持久化存储 | JDBM | 1.0 | 作业指定http://jdbm.sourceforge.net/ 库 |
| 文本处理 | 课程Lab3提供的Java版Porter算法、stopwords.txt停用词表 | 无修改 | 作业明确指定资源 |

---

## 三、项目工程结构规范（必须严格遵循该目录生成）
```
csit5930-search-engine/
├── pom.xml                          # Maven依赖配置，必须包含所有指定依赖
├── stopwords.txt                    # 课程Lab3提供的英文停用词表，每行1个停用词
├── README.txt                       # 作业要求的运行说明文档，完整编译/运行/复现步骤
├── presentation-slides-outline.md   # 作业要求的演示PPT内容大纲，覆盖所有强制板块
└── src/
    ├── main/
    │   ├── java/
    │   │   └── hk/ust/csit5930/search/
    │   │       ├── crawler/                # 爬虫模块，对应作业需求1
    │   │       │   ├── Spider.java         # 爬虫核心主类，BFS调度入口
    │   │       │   ├── PageFetcher.java    # 页面HTTP请求、元数据提取
    │   │       │   ├── LinkGraphManager.java # 父子链接关系管理，PageID双向查询
    │   │       │   ├── PageIDMapper.java   # URL与PageID双向映射，全局唯一ID分配
    │   │       │   ├── URLValidator.java   # URL抓取前校验、循环链接处理、更新时间检查
    │   │       │   └── model/              # 爬虫模块数据模型
    │   │       │       ├── CrawledPage.java # 抓取页面的标准化数据封装
    │   │       │       └── LinkRelation.java # 父子链接关系封装
    │   │       ├── indexer/                # 索引器模块，对应作业需求2
    │   │       │   ├── Indexer.java        # 索引构建核心主类
    │   │       │   ├── TextPreprocessor.java # 文本预处理：停用词过滤、Porter词干提取
    │   │       │   ├── PorterStemmer.java  # 课程Lab3提供的Porter词干提取算法实现
    │   │       │   ├── InvertedIndexManager.java # 倒排索引管理，标题/正文双索引构建
    │   │       │   └── model/              # 索引模块数据模型
    │   │       │       ├── PostingList.java # 倒排记录表，词干对应的全量文档记录
    │   │       │       ├── PostingEntry.java # 倒排项，单文档的词频、位置、统计信息
    │   │       │       └── PageMetadata.java # 页面元数据，用于结果展示
    │   │       ├── search/                 # 检索排序模块，对应作业需求3
    │   │       │   ├── SearchEngine.java   # 检索核心主类，对外统一检索入口
    │   │       │   ├── QueryParser.java    # 查询解析，短语识别、文本预处理
    │   │       │   ├── SimilarityCalculator.java # 相似度计算：TF-IDF、余弦相似度
    │   │       │   ├── Ranker.java         # 结果排序，标题加权、Top50结果截断
    │   │       │   ├── PhraseMatcher.java  # 短语匹配核心实现
    │   │       │   └── model/              # 检索模块数据模型
    │   │       │       ├── Query.java       # 查询封装：普通词项、短语词项
    │   │       │       ├── PhraseTerm.java  # 短语词项封装
    │   │       │       └── SearchResult.java # 检索结果封装，完全匹配前端展示格式
    │   │       ├── web/                    # Web交互模块，对应作业需求4
    │   │       │   └── SearchServlet.java  # 检索请求处理Servlet
    │   │       ├── enhancement/            # 可选增强功能模块，作业加分项
    │   │       │   └── SimilarPageRecommender.java # 推荐实现：相关反馈"get similar pages"
    │   │       └── common/                 # 公共工具类
    │   │           ├── JDBMManager.java    # JDBM连接、事务管理单例
    │   │           ├── Config.java         # 全局配置常量，禁止硬编码
    │   │           └── ExceptionHandler.java # 全局异常处理工具
    │   └── webapp/                         # Web应用根目录，Tomcat部署目录
    │       ├── index.jsp                   # 首页，查询输入表单
    │       ├── result.jsp                  # 结果展示页面，严格遵循作业格式
    │       └── WEB-INF/
    │           ├── web.xml                 # Web应用部署描述符
    │           └── lib/                    # 本地依赖jar包（备用）
    └── test/                               # 单元测试类，覆盖核心模块
        └── java/
            └── hk/ust/csit5930/search/
                ├── crawler/
                ├── indexer/
                ├── search/
                └── web/
```

---

## 四、分模块详细实现指令（按作业需求优先级排序）
### 模块1：爬虫模块（Crawler）- 100%覆盖作业需求1
#### 核心职责
从指定起始URL，使用BFS广度优先策略递归抓取指定数量的页面，完成抓取前合规校验，提取链接构建父子链接关系，输出标准化页面数据给索引器。
#### 固定全局配置（必须定义在Config.java中，禁止硬编码）
```java
// 作业指定起始URL
public static final String START_URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
// 作业指定抓取页数
public static final int MAX_CRAWL_PAGES = 300;
// 抓取超时时间10秒
public static final int CRAWL_TIMEOUT = 10000;
// 标题加权系数
public static final double TITLE_BOOST_FACTOR = 3.0;
// 最大返回结果数
public static final int MAX_SEARCH_RESULTS = 50;
```
#### 必须实现的核心功能（按执行顺序）
1.  **BFS广度优先调度**
    - 必须用队列实现BFS，禁止使用DFS
    - 起始URL入队，循环处理队列URL，直到达到300页抓取上限或队列为空
    - 每个URL仅处理一次，避免重复抓取
2.  **抓取前合规校验（作业强制要求的3项检查，必须按顺序执行）**
    对每个待抓取URL，必须全部通过以下检查才允许抓取：
    - 检查1：循环链接防护：URL已在已处理集合中，直接跳过
    - 检查2：索引存在性检查：URL不存在于倒排索引中，允许抓取
    - 检查3：页面更新检查：URL已存在于索引，但页面最后修改时间晚于索引记录时间，允许抓取；否则直接跳过
3.  **页面抓取与元数据提取**
    - 使用htmlparser库发起HTTP请求，获取页面HTML内容
    - 提取核心元数据：页面标题、正文内容、HTTP响应头最后修改时间、页面大小
    - 兜底逻辑：响应头无最后修改时间，使用抓取时间作为兜底；无页面大小，使用HTML内容字符数作为兜底
    - 异常处理：抓取失败的URL直接跳过，记录日志，不中断整体抓取流程
4.  **超链接提取与规范化**
    - 使用htmlparser库提取页面所有<a>标签的href超链接
    - 链接规范化：相对路径转绝对路径，过滤非http/https协议、锚点链接、无效链接
5.  **PageID与链接关系管理**
    - 为每个唯一URL分配全局唯一整型PageID，维护URL与PageID双向映射，持久化到JDBM
    - 构建父子链接关系文件结构，必须支持双向查询：
      - 输入父页面PageID，返回所有子页面PageID列表
      - 输入子页面PageID，返回所有父页面PageID列表
    - 链接关系必须持久化到JDBM，禁止内存存储
6.  **数据流转**
    - 单页面抓取完成后，必须将标准化CrawledPage对象传递给索引器，执行索引构建
    - 所有抓取元数据、链接关系必须在抓取完成后提交JDBM事务，持久化到磁盘
#### 核心方法强制规范
- `Spider.java` 必须包含入口方法：`public void startCrawl() throws Exception`
- `PageFetcher.java` 必须包含核心方法：`public CrawledPage fetchPage(String url) throws Exception`
- `LinkGraphManager.java` 必须包含两个双向查询方法：`public List<Integer> getChildPageIds(int parentPageId)`、`public List<Integer> getParentPageIds(int childPageId)`
- `URLValidator.java` 必须包含校验方法：`public boolean needFetch(String url) throws Exception`

---

### 模块2：索引器模块（Indexer）- 100%覆盖作业需求2
#### 核心职责
对抓取的页面进行文本预处理，构建标题、正文两个独立的倒排索引，支持短语搜索，所有索引持久化到JDBM。
#### 必须实现的核心功能（按执行顺序）
1.  **文本预处理（严格按顺序执行）**
    - 步骤1：文本转小写、分词，去除非字母字符
    - 步骤2：使用stopwords.txt过滤所有停用词
    - 步骤3：使用PorterStemmer算法将单词转为词干
    - 步骤4：记录每个词干在文本中的位置信息（用于短语搜索）
2.  **双倒排索引构建（作业强制要求）**
    - 索引1：标题倒排索引：存储从页面标题提取的所有词干，及对应的倒排记录
    - 索引2：正文倒排索引：存储从页面正文提取的所有词干，及向量空间模型所需的全量统计信息（tf、max(tf)、位置、文档频率等）
    - 两个索引完全独立，分别持久化到JDBM的不同HTree中
3.  **短语搜索支持**
    - 倒排记录中必须存储每个词干在文档中的位置信息
    - 位置信息必须可用于校验词项的连续性，实现短语匹配
4.  **页面元数据存储**
    - 存储每个页面的Top5高频词干（排除停用词）及对应词频、最后修改时间、页面大小、URL、标题等信息，持久化到JDBM，用于结果展示
#### 核心方法强制规范
- `Indexer.java` 必须包含核心方法：`public void indexPage(int pageId, CrawledPage page) throws Exception`
- `TextPreprocessor.java` 必须包含方法：`public List<TokenInfo> processText(String text) throws Exception`，返回词干、位置、词频信息
- `InvertedIndexManager.java` 必须包含方法：`public void addPosting(String stem, PostingEntry entry, boolean isTitle) throws Exception`

---

### 模块3：检索排序模块（Search Engine）- 100%覆盖作业需求3
#### 核心职责
解析用户查询，匹配倒排索引，严格按指定公式计算相似度，实现标题加权，返回排序后的Top50结果，支持短语搜索。
#### 必须实现的核心功能（按执行顺序）
1.  **查询解析**
    - 识别用户查询中双引号包裹的短语，区分普通词项和短语词项
    - 对查询文本执行与索引构建完全一致的预处理流程：转小写、分词、停用词过滤、Porter词干提取
2.  **短语匹配过滤**
    - 基于词项位置信息，校验短语中所有词项在文档中是否连续出现
    - 仅保留完整匹配短语的文档，参与后续相似度计算
3.  **TF-IDF权重计算（严格遵循作业指定公式，禁止修改）**
    - 公式：`TF-IDF = (tf * idf) / max(tf)`
    - tf：词项在文档中的词频
    - max(tf)：文档中所有词项的最大词频（归一化用）
    - idf：逆文档频率，公式：`idf = ln(总文档数 / 包含该词项的文档数)`
4.  **余弦相似度计算（严格遵循作业指定）**
    - 构建查询向量和文档向量，向量维度为词项的TF-IDF权重
    - 相似度公式：`余弦相似度 = 向量点积 / (查询向量模长 * 文档向量模长)`
5.  **标题匹配加权机制（作业强制要求）**
    - 词项在标题中匹配时，其TF-IDF权重乘以Config.java中定义的`TITLE_BOOST_FACTOR`
    - 词项同时在标题和正文匹配时，总权重为 正文TF-IDF + 标题TF-IDF*加权系数
6.  **结果排序与截断**
    - 所有文档按最终相似度得分降序排列
    - 最多返回Top50条结果，符合作业要求
#### 核心方法强制规范
- `SearchEngine.java` 必须包含统一检索入口：`public List<SearchResult> search(String queryStr) throws Exception`
- `QueryParser.java` 必须包含方法：`public Query parse(String queryStr) throws Exception`
- `SimilarityCalculator.java` 必须包含方法：`public double calculateCosineSimilarity(Map<String, Double> queryVector, Map<String, Double> docVector) throws Exception`
- `PhraseMatcher.java` 必须包含方法：`public Set<Integer> getPhraseMatchedPageIds(PhraseTerm phrase) throws Exception`

---

### 模块4：Web交互模块（Web Interface）- 100%覆盖作业需求4
#### 核心职责
接收用户查询，调用检索引擎，严格按作业指定格式展示结果。
#### 必须实现的核心功能
1.  **首页 index.jsp**
    - 包含name为`query`的文本输入框，GET方法提交到`/search`路径
    - 包含提交按钮，提示用户支持双引号短语搜索（示例：`"hong kong" universities`）
2.  **检索Servlet SearchServlet.java**
    - 接收GET请求的query参数，调用SearchEngine执行检索
    - 将查询词和检索结果传递给result.jsp，完成页面渲染
3.  **结果页 result.jsp（必须严格遵循作业指定的展示格式，每条结果按顺序包含以下字段）**
    1.  文档得分score，保留6位小数
    2.  页面标题，必须是超链接，跳转至原始URL，target="_blank"
    3.  页面URL纯文本展示
    4.  最后修改日期、页面大小，格式：`最后修改时间: xxx | 页面大小: xxx 字符`
    5.  关键词列表：最多5个高频词干，格式：`keyword1 freq1; keyword2 freq2; ...`
    6.  父链接列表：所有父页面URL，每个URL为可跳转超链接
    7.  子链接列表：所有子页面URL，每个URL为可跳转超链接
    - 所有结果按得分降序排列，最多展示50条，全部在同一页面展示
    - 界面无需复杂样式，但必须清晰可读

---

### 模块5：增强功能模块（作业加分项，必须实现1项）
#### 推荐实现：相关反馈功能 "get similar pages"（作业指定选项1，实现成本低、加分明确）
#### 必须实现的核心功能
1.  在result.jsp的每条结果中，添加"get similar pages"按钮/超链接
2.  点击按钮后，提取该页面的Top5高频词干（排除停用词）
3.  用这5个词干重写查询，自动提交新的检索请求
4.  新结果页展示与该页面相似的页面，按相似度降序排列
5.  复用现有检索引擎核心逻辑，禁止重复开发
#### 代码规范
- 核心逻辑在`enhancement/SimilarPageRecommender.java`中实现
- 可复用现有SearchServlet，或新增独立Servlet处理请求

---

## 五、提交要求相关生成规范（作业强制要求）
### 1. 源代码规范
- 所有类、方法必须有Javadoc注释，核心逻辑必须有行内注释，标注对应作业需求
- 所有资源（HTTP连接、JDBM连接、文件流）必须使用try-with-resources语法关闭
- JDBM操作必须使用事务，批量操作后commit，异常时rollback
- 禁止硬编码，所有配置常量必须定义在Config.java中
- 必须生成单元测试类，覆盖核心模块的核心方法

### 2. README.txt 必须包含的内容
- 项目简介
- 环境要求（Java、Tomcat、Maven版本）
- 完整编译步骤（Maven命令）
- 完整运行步骤（Tomcat部署、爬虫启动、服务启动）
- 结果复现步骤（抓取300页、执行查询、功能验证）
- 增强功能说明

### 3. presentation-slides-outline.md 必须包含作业要求的所有板块
1.  系统整体设计架构
2.  索引数据库使用的文件结构
3.  核心算法实现（含标题匹配加权机制）
4.  安装运行步骤
5.  超出需求的增强功能亮点
6.  功能测试结果（含截图说明）
7.  结论：系统优势与不足、重构优化方向、未来可扩展功能

---

## 最终执行指令
请严格按照以上所有规范，生成**完整可运行、100%符合CSIT5930课程作业要求**的搜索引擎项目全量代码，包含所有指定的类、配置文件、JSP页面、README、PPT大纲，确保代码可直接通过Maven编译、Tomcat部署运行，完整复现作业要求的所有功能。