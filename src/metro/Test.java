package metro;

import metro.exception.InvalidInputException;
import metro.exception.StationNotFoundException;

import java.util.List;

/**
 * 测试类（含 main 函数）。
 * 按《OOP 大作业-2026》的七个功能评分点逐项测试：
 *
 *   功能一（10 分）识别所有地铁中转站
 *   功能二（10 分）某站 n 公里内的所有站点
 *   功能三（10 分）两站之间的所有无环路径集合
 *   功能四（20 分）两站之间的一条最短路径
 *   功能五（10 分）printPath 简洁打印乘车段
 *   功能六（10 分）用户选定路径的单程票价
 *   功能七（10 分）同一路径的武汉通 / 日票票价
 */
public class Test {

    public static void main(String[] args) {
        print("武汉地铁模拟系统  功能自测");

        SubwaySystem system;
        try {
            system = new SubwaySystem();
        } catch (InvalidInputException e) {
            System.out.println("[数据格式错误] " + e.getMessage());
            return;
        } catch (Exception e) {
            System.out.println("[系统初始化失败] " + e.getMessage());
            return;
        }
        System.out.println("数据文件：" + system.getDataFile());
        System.out.println();

        testModule1(system);
        testModule2(system);
        testModule3(system);
        List<Station> shortest = testModule4(system);
        testModule5(system, shortest);
        testModule6(system);
        testModule7(system);
        testExceptions(system);

        System.out.println();
        System.out.println("全部测试执行完毕。");
    }

    /** 作业“线路查询”模块：线路数与站点数 */
    private static void testModule1(SubwaySystem system) {
        section("功能一（10 分）识别所有地铁中转站");
        System.out.println(system.listAllLines());
        System.out.println();
        System.out.println(system.listTransferStations());
        try {
            System.out.println(system.countLinesOfStation("中南路"));
            System.out.println(system.countLinesOfStation("宏图大道"));
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 功能二：n 公里内的站点 */
    private static void testModule2(SubwaySystem system) {
        section("功能二（10 分）输出线路距离小于 n 的所有站点");
        try {
            // 作业原例：华中科技大学站，距离为 1 的站点为珞雄路、光谷大道
            // 两站实际里程 0.817 km 与 1.009 km，故取 n = 1.5 即可完整复现该例
            System.out.println("【作业原例】华中科技大学，n = 1.5");
            System.out.println(system.queryNearby("华中科技大学", 1.5));
            System.out.println("华中科技大学，n = 3");
            System.out.println(system.queryNearby("华中科技大学", 3));
            System.out.println("天河机场，n = 10");
            System.out.println(system.queryNearby("天河机场", 10));
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 功能三：所有无环路径 */
    private static void testModule3(SubwaySystem system) {
        section("功能三（10 分）连接起点与终点的所有路径（不含环路）");
        try {
            System.out.println(system.describeAllPaths("宝通寺", "武昌火车站", 10, 5));
            System.out.println(system.describeAllPaths("华中科技大学", "光谷广场", 10, 5));
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 功能四：最短路径，返回给后续功能复用 */
    private static List<Station> testModule4(SubwaySystem system) {
        section("功能四（20 分）返回一条最短路径");
        List<Station> result = null;
        try {
            System.out.println(system.describeShortestPath("天河机场", "光谷广场"));
            result = system.getShortestPath("宝通寺", "武昌火车站");
            System.out.println("宝通寺 -> 武昌火车站 的最短路径：");
            System.out.println("  " + system.joinPath(result));
            System.out.println("  里程 "
                    + String.format("%.3f", Double.valueOf(
                            system.getPathFinder().pathDistance(result))) + " km");
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
        return result;
    }

    /** 功能五：printPath 的格式校验 */
    private static void testModule5(SubwaySystem system, List<Station> path) {
        section("功能五（10 分）printPath 简洁打印乘车段");
        try {
            List<Station> case1 = system.getShortestPath("天河机场", "盘龙城");
            System.out.println("用例一  路径 <天河机场、航空总部、宋家岗、巨龙大道、盘龙城>");
            System.out.print("  输出：");
            system.printPath(case1);
            check("乘2号线[天河机场，盘龙城]", system.getPathPrinter().buildRidePlan(case1));

            List<Station> case2 = system.getShortestPath("宝通寺", "武昌火车站");
            System.out.println("用例二  路径 <宝通寺、中南路、梅苑小区、武昌火车站>");
            System.out.print("  输出：");
            system.printPath(case2);
            check("乘2号线[宝通寺，中南路]，换乘4号线[中南路，武昌火车站]",
                    system.getPathPrinter().buildRidePlan(case2));

            if (path != null) {
                System.out.println("用例三  功能四得到的最短路径");
                System.out.print("  输出：");
                system.printPath(path);
            }
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 功能六：用户从路径集合中选一条，算单程票 */
    private static void testModule6(SubwaySystem system) {
        section("功能六（10 分）用户选定路径的单程票票价");
        try {
            List<List<Station>> paths = system.getAllPaths("宝通寺", "武昌火车站", 10);
            System.out.println("路径集合共 " + paths.size() + " 条，逐条计价：");
            for (int i = 0; i < paths.size(); i++) {
                System.out.println("  方案 " + (i + 1) + "：" + system.describeSingleFare(paths.get(i)));
                System.out.println();
            }
            System.out.println("用户选择方案 1，票价 = "
                    + String.format("%.1f", Double.valueOf(system.calculateSingleFare(paths.get(0))))
                    + " 元");
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 功能七：武汉通与日票 */
    private static void testModule7(SubwaySystem system) {
        section("功能七（10 分）武汉通与日票票价");
        try {
            List<List<Station>> paths = system.getAllPaths("宝通寺", "武昌火车站", 10);
            System.out.println(system.describeAllFareTypes(paths.get(0)));
            System.out.println();
            System.out.println(system.describeAllFareTypes(system.getShortestPath("天河机场", "光谷广场")));
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 异常处理演示（作业要求“输入不合规时进行异常处理”） */
    private static void testExceptions(SubwaySystem system) {
        section("异常处理");
        try {
            system.getShortestPath("不存在的站", "光谷广场");
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
        try {
            system.getShortestPath("", "光谷广场");
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
        try {
            system.queryNearby("光谷广场", -1);
        } catch (InvalidInputException e) {
            System.out.println("[InvalidInputException] " + e.getMessage());
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
        try {
            system.describeLine("99号线");
        } catch (StationNotFoundException e) {
            System.out.println("[StationNotFoundException] " + e.getMessage());
        }
    }

    /** 与函数说明给出的标准答案比对 */
    private static void check(String expected, String actual) {
        System.out.println("  期望：" + expected);
        System.out.println("  实际：" + actual);
        System.out.println("  结论：" + (expected.equals(actual) ? "一致 √" : "不一致 ×"));
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("======== " + title + " ========");
    }

    private static void print(String title) {
        System.out.println("================ " + title + " ================");
    }
}
