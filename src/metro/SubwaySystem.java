package metro;

import metro.exception.InvalidInputException;
import metro.exception.StationNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 系统门面类（控制层）。
 * 对界面层只暴露“输入站点名 / 范围 -> 输出结果”的方法，
 * 具体的解析、搜索、计价全部委托给下层业务类完成。
 *
 * 功能编号严格对应《OOP 大作业-2026》的评分点：
 *   功能一  识别所有地铁中转站
 *   功能二  某站 n 公里内的所有站点
 *   功能三  连接起点与终点的所有无环路径集合
 *   功能四  起点到终点的一条最短路径
 *   功能五  printPath 简洁打印乘车段
 *   功能六  用户从路径集合中选定一条路径后计算普通单程票价
 *   功能七  同一条路径下武汉通 / 日票的票价
 */
public class SubwaySystem {

    private SubwayNetwork network;
    private PathFinder pathFinder;
    private FareCalculator fareCalculator;
    private PathPrinter pathPrinter;
    private String dataFile;

    /** 默认从工作目录下的 data/subway.txt 装载 */
    public SubwaySystem() throws IOException, InvalidInputException, StationNotFoundException {
        this(SubwayFileReader.locateDataFile());
    }

    public SubwaySystem(String dataFile) throws IOException, InvalidInputException {
        load(dataFile);
    }

    /** 装载（或重新装载）数据 */
    public void load(String dataFile) throws IOException, InvalidInputException {
        SubwayFileReader reader = new SubwayFileReader();
        this.network = reader.read(new File(dataFile));
        this.pathFinder = new PathFinder(network);
        this.fareCalculator = new FareCalculator();
        this.pathPrinter = new PathPrinter(network);
        this.dataFile = dataFile;
    }

    public SubwayNetwork getNetwork() {
        return network;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public FareCalculator getFareCalculator() {
        return fareCalculator;
    }

    public PathPrinter getPathPrinter() {
        return pathPrinter;
    }

    public String getDataFile() {
        return dataFile;
    }

    // ================================================================ 功能一

    /**
     * 功能一：在所有地铁车站中识别出所有中转站（至少有两条线经过的站）。
     *
     * @return 中转站集合，每条记录含站点名称与经过的所有线路
     */
    public List<TransferStation> getTransferStations() {
        List<TransferStation> result = new ArrayList<TransferStation>();
        List<Station> list = network.getTransferStations();
        for (int i = 0; i < list.size(); i++) {
            result.add(new TransferStation(list.get(i)));
        }
        return result;
    }

    /** 功能一的文本输出 */
    public String listTransferStations() {
        List<TransferStation> list = getTransferStations();
        StringBuilder sb = new StringBuilder();
        sb.append("地铁中转站共 ").append(list.size()).append(" 个：\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(list.get(i)).append('\n');
        }
        return sb.toString();
    }

    /** 某一站点拥有的线路数 / 某条线路拥有的站点数（作业“线路查询”模块要求） */
    public String countLinesOfStation(String stationName) throws StationNotFoundException {
        Station station = network.requireStation(stationName);
        StringBuilder sb = new StringBuilder();
        sb.append(station.getName()).append(" 共有 ").append(station.getLines().size()).append(" 条线路：");
        List<String> lines = station.getLines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /** 全部线路概览 */
    public String listAllLines() {
        StringBuilder sb = new StringBuilder();
        sb.append("线路名称\t站点数\t区间数\t全长(km)\n");
        for (Line line : network.getLines()) {
            sb.append(line.getName()).append('\t')
              .append(line.stationCount()).append('\t')
              .append(line.sectionCount()).append('\t')
              .append(String.format("%.3f", Double.valueOf(line.totalLength()))).append('\n');
        }
        sb.append("合计：").append(network.getLines().size()).append(" 条线路，")
          .append(network.stationCount()).append(" 个站点，")
          .append(network.transferStationCount()).append(" 个中转站");
        return sb.toString();
    }

    /** 某条线路的全部站点与相邻间距 */
    public String describeLine(String lineName) throws StationNotFoundException {
        Line line = network.getLine(lineName);
        if (line == null) {
            throw new StationNotFoundException("不存在名为 [" + lineName + "] 的线路");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(line.getName()).append("（共 ").append(line.stationCount())
          .append(" 站，").append(line.sectionCount()).append(" 个区间，全长 ")
          .append(String.format("%.3f", Double.valueOf(line.totalLength()))).append(" km）\n");
        List<Station> stations = line.getStations();
        for (int i = 0; i < stations.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(stations.get(i).getName());
            if (stations.get(i).isTransferStation()) {
                sb.append("  <换乘 ").append(join(stations.get(i).getLines(), "、")).append(">");
            }
            if (i < stations.size() - 1) {
                sb.append("   --").append(String.format("%.3f", Double.valueOf(line.distanceToNext(i))))
                  .append(" km-->");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ================================================================ 功能二

    /**
     * 功能二：输入某一站点，输出线路距离小于 n 的所有站点集合
     * （含站点名称、所在线路、距离给定站点的距离）。
     * 输入不合规时抛出异常。
     */
    public List<PathFinder.NearbyStation> queryNearbyStations(String stationName, double n)
            throws InvalidInputException, StationNotFoundException {
        if (stationName == null || stationName.trim().isEmpty()) {
            throw new InvalidInputException("站点名称不能为空");
        }
        if (n <= 0) {
            throw new InvalidInputException("距离范围 n 必须为正数，当前为 " + n);
        }
        Station station = network.requireStation(stationName);
        return pathFinder.findNearbyStations(station, n);
    }

    /** 功能二的文本输出 */
    public String queryNearby(String stationName, double n)
            throws InvalidInputException, StationNotFoundException {
        List<PathFinder.NearbyStation> list = queryNearbyStations(stationName, n);
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(stationName).append("] 距离小于 ")
          .append(String.format("%.1f", Double.valueOf(n))).append(" km 的站点共 ")
          .append(list.size()).append(" 个：\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(list.get(i)).append('\n');
        }
        return sb.toString();
    }

    // ================================================================ 功能三

    /**
     * 功能三：输入起点站和终点站名称，返回连接两站的所有路径集合，
     * 每条路径经过的站点不重复（不含环路），按里程升序排列。
     *
     * @param startName 起点站
     * @param stopName  终点站
     * @param maxHops   最多经过的区间数，用于在路网较密时限制搜索规模
     */
    public List<List<Station>> getAllPaths(String startName, String stopName, int maxHops)
            throws InvalidInputException, StationNotFoundException {
        if (maxHops <= 0) {
            throw new InvalidInputException("区间上限 maxHops 必须为正数");
        }
        final Station start = network.requireStation(startName);
        Station stop = network.requireStation(stopName);
        List<List<Station>> paths = pathFinder.findAllPaths(start, stop, maxHops);
        // 按里程升序（匿名内部类实现 Comparator）
        Collections.sort(paths, new Comparator<List<Station>>() {
            @Override
            public int compare(List<Station> a, List<Station> b) {
                return Double.compare(pathFinder.pathDistance(a), pathFinder.pathDistance(b));
            }
        });
        return paths;
    }

    /** 功能三的文本输出，只显示前 limit 条避免刷屏 */
    public String describeAllPaths(String startName, String stopName, int maxHops, int limit)
            throws InvalidInputException, StationNotFoundException {
        List<List<Station>> paths = getAllPaths(startName, stopName, maxHops);
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(startName).append("] -> [").append(stopName).append("] 共 ")
          .append(paths.size()).append(" 条无环路径（最多 ").append(maxHops).append(" 个区间）：\n");
        int show = Math.min(limit, paths.size());
        for (int i = 0; i < show; i++) {
            List<Station> path = paths.get(i);
            sb.append("  ").append(i + 1).append(". ").append(joinPath(path))
              .append("  ").append(String.format("%.3f", Double.valueOf(pathFinder.pathDistance(path))))
              .append(" km\n");
        }
        if (paths.size() > show) {
            sb.append("  …… 其余 ").append(paths.size() - show).append(" 条略\n");
        }
        return sb.toString();
    }

    // ================================================================ 功能四

    /**
     * 功能四：给定起点和终点，返回一条最短路径
     * （包含从起点开始直到终点所需经过的所有站点的数组）。
     *
     * 方法名与参数类型按《关于路径的函数说明》第3问规定。
     *
     * @param start 起始站点
     * @param stop  终止站点
     * @return 从起始站点开始、到终止站点结束的站点数组（含起点和终点）
     */
    public List<Station> getShortestPath(Station start, Station stop) {
        return pathFinder.getShortestPath(start, stop);
    }

    /** 功能四：按站名查最短路径，输入不合规时抛异常 */
    public List<Station> getShortestPath(String startName, String stopName)
            throws InvalidInputException, StationNotFoundException {
        if (startName == null || startName.trim().isEmpty()) {
            throw new InvalidInputException("起点站名称不能为空");
        }
        if (stopName == null || stopName.trim().isEmpty()) {
            throw new InvalidInputException("终点站名称不能为空");
        }
        return pathFinder.getShortestPath(network.requireStation(startName), network.requireStation(stopName));
    }

    /** 功能四：以文本形式给出最短路径、里程与分段 */
    public String describeShortestPath(String startName, String stopName)
            throws InvalidInputException, StationNotFoundException {
        List<Station> path = getShortestPath(startName, stopName);
        double distance = pathFinder.pathDistance(path);
        List<RideSegment> segments = getRideSegments(path);
        StringBuilder sb = new StringBuilder();
        sb.append("起点：").append(path.get(0).getName())
          .append("    终点：").append(path.get(path.size() - 1).getName()).append('\n');
        sb.append("最短路径（共 ").append(path.size()).append(" 站，")
          .append(segments.size() - 1).append(" 次换乘）：\n  ")
          .append(joinPath(path)).append('\n');
        sb.append("总里程：").append(String.format("%.3f", Double.valueOf(distance))).append(" km\n");
        sb.append("乘车方案：").append(pathPrinter.buildRidePlan(path)).append('\n');
        sb.append("分段明细：\n");
        for (int i = 0; i < segments.size(); i++) {
            RideSegment seg = segments.get(i);
            sb.append("  ").append(i + 1).append(". ").append(seg).append("  ")
              .append(String.format("%.3f", Double.valueOf(seg.getDistance()))).append(" km\n");
        }
        return sb.toString();
    }

    // ================================================================ 功能五

    /**
     * 功能五：把路径以简洁形式打印至标准输出，仅包含每段乘车路径的起止。
     *
     * 方法名与输出格式按《关于路径的函数说明》第4问规定：
     *   输入 &lt;宝通寺、中南路、梅苑小区、武昌火车站&gt;
     *   输出 乘2号线[宝通寺，中南路]，换乘4号线[中南路，武昌火车站]
     *
     * @param path 包含从起点到终点在内顺序经过的所有站点的数组
     */
    public void printPath(List<Station> path) {
        pathPrinter.printPath(path);
    }

    /** 功能五：把路径拆成若干换乘段（与 printPath 的切段结果完全一致） */
    public List<RideSegment> getRideSegments(List<Station> path) {
        List<RideSegment> result = new ArrayList<RideSegment>();
        if (path == null || path.size() < 2) {
            return result;
        }
        List<List<Station>> groups = pathPrinter.splitByLine(path);
        List<String> lines = pathPrinter.linesOfGroups(groups);
        for (int g = 0; g < groups.size(); g++) {
            List<Station> group = groups.get(g);
            double d = 0;
            for (int k = 0; k + 1 < group.size(); k++) {
                d += pathFinder.edgeDistance(group.get(k), group.get(k + 1));
            }
            result.add(new RideSegment(lines.get(g), group.get(0), group.get(group.size() - 1), d));
        }
        return result;
    }

    // ================================================================ 功能六

    /**
     * 功能六：用户从路径集合中选定一条路径后，计算该路径对应的普通单程票价。
     *
     * @param chosen 用户选定的路径（来自功能三的结果集）
     * @return 单程票票价（元）
     */
    public double calculateSingleFare(List<Station> chosen) {
        return fareCalculator.fare(pathFinder.pathDistance(chosen), FareCalculator.TicketType.SINGLE);
    }

    /** 功能六：用户选定路径的票价说明文本 */
    public String describeSingleFare(List<Station> chosen) {
        double distance = pathFinder.pathDistance(chosen);
        StringBuilder sb = new StringBuilder();
        sb.append("所选路径：").append(joinPath(chosen)).append('\n');
        sb.append("总里程：").append(String.format("%.3f", Double.valueOf(distance))).append(" km\n");
        sb.append("普通单程票票价：").append(String.format("%.1f",
                Double.valueOf(fareCalculator.fare(distance, FareCalculator.TicketType.SINGLE))))
          .append(" 元（")
          .append(fareCalculator.describe(distance, FareCalculator.TicketType.SINGLE)).append("）");
        return sb.toString();
    }

    // ================================================================ 功能七

    /**
     * 功能七：同一条路径下，武汉通（9 折）与日票（0 元）的票价。
     */
    public double calculateFare(List<Station> chosen, FareCalculator.TicketType type) {
        return fareCalculator.fare(pathFinder.pathDistance(chosen), type);
    }

    /** 功能七：同一条路径三种票种的对照说明 */
    public String describeAllFareTypes(List<Station> chosen) {
        double distance = pathFinder.pathDistance(chosen);
        StringBuilder sb = new StringBuilder();
        sb.append("路径：").append(joinPath(chosen)).append('\n');
        sb.append("总里程：").append(String.format("%.3f", Double.valueOf(distance))).append(" km（")
          .append("单程票基数 ").append(fareCalculator.baseFare(distance)).append(" 元）\n");
        sb.append("  普通单程票：").append(String.format("%.1f",
                Double.valueOf(fareCalculator.fare(distance, FareCalculator.TicketType.SINGLE)))).append(" 元\n");
        sb.append("  武汉通 9 折：").append(String.format("%.1f",
                Double.valueOf(fareCalculator.fare(distance, FareCalculator.TicketType.WUHAN_TONG)))).append(" 元\n");
        sb.append("  日    票   ：").append(String.format("%.1f",
                Double.valueOf(fareCalculator.fare(distance, FareCalculator.TicketType.DAY_PASS)))).append(" 元");
        return sb.toString();
    }

    // ================================================================ 工具

    /** 拼接线路名列表 */
    private String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /** 把路径拼成 A -> B -> C */
    public String joinPath(List<Station> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(path.get(i).getName());
        }
        return sb.toString();
    }
}
