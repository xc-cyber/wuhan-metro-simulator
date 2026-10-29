# 武汉地铁模拟系统

华中科技大学《面向对象程序设计》大作业（2026）

基于武汉地铁真实数据（12 条线路、250 个站点、264 个区间），实现中转站识别、附近站点查询、全部路径枚举、最短路径、乘车方案打印与分段计价等功能，并提供图形界面（选做）。

## 一、项目结构

```
wuhan-metro-simulator/
├── data/
│   └── subway.txt            # 12 条线路、264 个区间的里程数据（Tab 分隔）
├── docs/
│   ├── report.pdf            # 大作业报告（含用例图、类图与全部说明）
│   ├── 01-class-diagram.puml/.png       # 类图（含 MetroUI）
│   ├── 02-use-case.puml/.png            # 用例图
│   └── 04-gui-screenshot.png            # 图形界面运行截图
├── src/metro/
│   ├── Station.java          # 站点类（实体）
│   ├── Line.java             # 线路类（实体）
│   ├── SubwayNetwork.java    # 地铁网络类（业务，内含 Edge 邻接边）
│   ├── SubwayFileReader.java # 数据读取类（业务）
│   ├── PathFinder.java       # 路径搜索类（业务）
│   ├── PathPrinter.java      # 乘车方案打印类（业务）
│   ├── FareCalculator.java   # 计价类（业务，内含 TicketType 枚举）
│   ├── TransferStation.java  # 中转站记录类（实体）
│   ├── RideSegment.java      # 乘车段记录类（实体）
│   ├── SubwaySystem.java     # 系统门面/控制类
│   ├── MetroUI.java          # 图形界面（选做，Swing）
│   ├── Test.java             # 测试类（main）
│   └── exception/
│       ├── InvalidInputException.java   # 受检异常：输入不合规
│       └── StationNotFoundException.java # 非受检异常：站点/线路不存在
├── run.bat                   # 命令行版一键编译运行
├── run-gui.bat               # 图形界面版一键编译运行
└── README.md
```

## 二、功能与评分点对应

| 功能 | 分值 | 说明 |
|---|---|---|
| 功能一 | 10 分 | 识别所有地铁中转站（共 33 个） |
| 功能二 | 10 分 | 某站 n 公里内的所有站点（按实际里程判断） |
| 功能三 | 10 分 | 两站之间的所有无环路径集合（DFS + 序列去重） |
| 功能四 | 10 分 | 两站之间的一条最短路径（Dijkstra） |
| 功能五 | 10 分 | printPath 简洁打印乘车段（动态规划求换乘最少组合） |
| 功能六 | 10 分 | 用户选定路径的单程票价（分段计价） |
| 功能七 | 10 分 | 同一路径的武汉通 9 折 / 日票票价 |
| 选做 | +10 分 | Swing 图形界面（MetroUI） |

## 三、编译与运行

需要 JDK 8 及以上。

```bash
# 方式一：双击批处理
run.bat       # 命令行版：编译 + 运行全部功能自测
run-gui.bat   # 图形界面版：编译 + 启动窗口

# 方式二：手动命令
javac -encoding UTF-8 -d bin src/metro/*.java src/metro/exception/*.java
java -Dfile.encoding=UTF-8 -cp bin metro.Test     # 命令行测试
java -Dfile.encoding=UTF-8 -cp bin metro.MetroUI  # 图形界面
```

## 四、设计要点

- **三层结构**：实体类（Station / Line 等）→ 业务类（SubwayNetwork / PathFinder 等）→ 控制类（SubwaySystem），界面层（MetroUI / Test）零业务逻辑。
- **printPath 全局最优**：以动态规划求"换乘次数最少"的线路组合，与官方样例逐字一致。
- **路径枚举去重**：共线平行边产生的重复站点序列以序列指纹去重。
- **附近站点**：按实际里程（Dijkstra 变体）而非站数判断"n 公里内"。

详细设计见 `docs/report.pdf`。
