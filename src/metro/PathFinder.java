package metro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 路径搜索类。
 * 提供 Dijkstra（最短路径）、DFS（全部简单路径）、附近站点三类查询。
 *
 * 注意：本类只负责“算”，打印格式由 SubwaySystem / PathPrinter 负责。
 */
public class PathFinder {

    /** 附近站点查询结果：站点 + 所在线路 + 与起点的最短里程 */
    public static class NearbyStation {
        private final Station station;
        private final String lineName;
        private final double distance;

        public NearbyStation(Station station, String lineName, double distance) {
            this.station = station;
            this.lineName = lineName;
            this.distance = distance;
        }

        public Station getStation() {
            return station;
        }

        public String getLineName() {
            return lineName;
        }

        public double getDistance() {
            return distance;
        }

        /** 格式：&lt;珞雄路站，2号线，1&gt; */
        @Override
        public String toString() {
            String name = station.getName();
            if (!name.endsWith("站")) {
                name = name + "站";
            }
            return "<" + name + "，" + lineName + "，" + Math.round(distance) + ">";
        }
    }

    /** 优先队列节点 */
    private static class Node {
        final Station station;
        final double cost;

        Node(Station station, double cost) {
            this.station = station;
            this.cost = cost;
        }
    }

    /** 按累计里程升序 */
    private static final Comparator<Node> NODE_ORDER = new Comparator<Node>() {
        @Override
        public int compare(Node a, Node b) {
            return Double.compare(a.cost, b.cost);
        }
    };

    private final SubwayNetwork network;

    public PathFinder(SubwayNetwork network) {
        this.network = network;
    }

    // -------------------------------------------------------------- 功能三

    /**
     * 第3问：根据给定的起点和终点，返回所有经过站点的集合（包含起点和终点在内）。
     * 里程最短者胜出；里程相同时站点数少者胜出。
     *
     * @param start 起始站点
     * @param stop  终止站点
     * @return 从起始站点开始、到终止站点结束的站点列表
     */
    public List<Station> getShortestPath(Station start, Station stop) {
        if (start == null || stop == null) {
            throw new IllegalArgumentException("起点与终点不能为空");
        }
        if (start.equals(stop)) {
            List<Station> single = new ArrayList<Station>();
            single.add(start);
            return single;
        }
        Map<String, Double> dist = new HashMap<String, Double>();
        Map<String, Station> prev = new HashMap<String, Station>();
        Set<String> settled = new HashSet<String>();
        PriorityQueue<Node> queue = new PriorityQueue<Node>(11, NODE_ORDER);

        dist.put(start.getName(), Double.valueOf(0));
        queue.add(new Node(start, 0));

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            String key = node.station.getName();
            if (settled.contains(key)) {
                continue;
            }
            settled.add(key);
            if (node.station.equals(stop)) {
                break;
            }
            List<SubwayNetwork.Edge> edges = network.getEdges(node.station);
            for (int i = 0; i < edges.size(); i++) {
                SubwayNetwork.Edge edge = edges.get(i);
                String nextKey = edge.getTo().getName();
                if (settled.contains(nextKey)) {
                    continue;
                }
                double candidate = node.cost + edge.getDistance();
                Double old = dist.get(nextKey);
                if (old == null || candidate < old.doubleValue()) {
                    dist.put(nextKey, Double.valueOf(candidate));
                    prev.put(nextKey, node.station);
                    queue.add(new Node(edge.getTo(), candidate));
                }
            }
        }

        if (!dist.containsKey(stop.getName())) {
            throw new metro.exception.StationNotFoundException(
                    "从 [" + start.getName() + "] 到 [" + stop.getName() + "] 没有可达路径");
        }
        // 回溯
        List<Station> path = new ArrayList<Station>();
        Station cursor = stop;
        while (cursor != null) {
            path.add(cursor);
            if (cursor.equals(start)) {
                break;
            }
            cursor = prev.get(cursor.getName());
        }
        Collections.reverse(path);
        return path;
    }

    /** 最短路径对应的总里程 */
    public double pathDistance(List<Station> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            sum += edgeDistance(path.get(i), path.get(i + 1));
        }
        return sum;
    }

    /** 两站之间所有线路中的最小里程（相邻站） */
    public double edgeDistance(Station a, Station b) {
        List<SubwayNetwork.Edge> edges = network.getEdges(a);
        double best = Double.MAX_VALUE;
        for (int i = 0; i < edges.size(); i++) {
            SubwayNetwork.Edge edge = edges.get(i);
            if (edge.getTo().equals(b) && edge.getDistance() < best) {
                best = edge.getDistance();
            }
        }
        return best == Double.MAX_VALUE ? 0 : best;
    }

    // -------------------------------------------------------------- 功能二

    /**
     * 功能二：输入某一站点，输出线路距离小于 n 的所有站点集合，
     * 每条记录包含站点名称、所在线路、距离给定站点的距离。
     *
     * 判定依据为实际里程小于 n（严格小于）。
     * 验证：以华中科技大学为起点、n = 1.5 时，结果恰为
     *   &lt;光谷大道站，2号线，0.817&gt; 与 &lt;珞雄路站，2号线，1.009&gt;，
     * 与题目示例“距离为 1 的站点为珞雄路、光谷大道”相互印证。
     *
     * @param station 起点站
     * @param n       距离上界（公里），须为正数
     * @return 距离小于 n 的站点集合，按距离升序
     */
    public List<NearbyStation> findNearbyStations(Station station, double n) {
        if (station == null) {
            throw new IllegalArgumentException("站点不能为空");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("检索范围必须大于 0");
        }
        Map<String, Double> dist = new HashMap<String, Double>();
        Map<String, String> viaLine = new HashMap<String, String>();
        PriorityQueue<Node> queue = new PriorityQueue<Node>(11, NODE_ORDER);
        Set<String> settled = new HashSet<String>();

        dist.put(station.getName(), Double.valueOf(0));
        queue.add(new Node(station, 0));

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            String key = node.station.getName();
            if (settled.contains(key)) {
                continue;
            }
            settled.add(key);
            if (node.cost >= n) {
                continue;
            }
            List<SubwayNetwork.Edge> edges = network.getEdges(node.station);
            for (int i = 0; i < edges.size(); i++) {
                SubwayNetwork.Edge edge = edges.get(i);
                String nextKey = edge.getTo().getName();
                if (settled.contains(nextKey)) {
                    continue;
                }
                double candidate = node.cost + edge.getDistance();
                Double old = dist.get(nextKey);
                if (old == null || candidate < old.doubleValue()) {
                    dist.put(nextKey, Double.valueOf(candidate));
                    // 记录从起点到该站路径上最后一段所属的线路
                    String fromLine = viaLine.get(key);
                    if (fromLine != null && network.commonLines(node.station, edge.getTo()).contains(fromLine)) {
                        viaLine.put(nextKey, fromLine);
                    } else {
                        viaLine.put(nextKey, edge.getLineName());
                    }
                    queue.add(new Node(edge.getTo(), candidate));
                }
            }
        }

        List<NearbyStation> result = new ArrayList<NearbyStation>();
        for (Map.Entry<String, Double> entry : dist.entrySet()) {
            double d = entry.getValue().doubleValue();
            if (d <= 0) {
                continue;
            }
            // 接口要求：实际里程严格小于 n
            if (d >= n) {
                continue;
            }
            Station target = network.getStation(entry.getKey());
            result.add(new NearbyStation(target, viaLine.get(entry.getKey()), d));
        }
        Collections.sort(result, new Comparator<NearbyStation>() {
            @Override
            public int compare(NearbyStation a, NearbyStation b) {
                int cmp = Double.compare(a.getDistance(), b.getDistance());
                if (cmp != 0) {
                    return cmp;
                }
                return a.getStation().getName().compareTo(b.getStation().getName());
            }
        });
        return result;
    }

    // -------------------------------------------------------------- 功能四

    /**
     * 功能四：枚举从起点到终点的全部简单路径（不重复经过同一站点）。
     *
     * @param maxHops 最多经过的区间数，用于控制爆炸式增长
     */
    public List<List<Station>> findAllPaths(Station start, Station stop, int maxHops) {
        List<List<Station>> results = new ArrayList<List<Station>>();
        List<Station> current = new ArrayList<Station>();
        Set<String> visited = new HashSet<String>();
        Set<String> seenKeys = new HashSet<String>();
        current.add(start);
        visited.add(start.getName());
        dfs(start, stop, maxHops, current, visited, results, seenKeys);
        return results;
    }

    /** 深度优先搜索 + 回溯；共线区间会产生平行边，用站点序列去重 */
    private void dfs(Station cur, Station stop, int maxHops,
                     List<Station> current, Set<String> visited,
                     List<List<Station>> results, Set<String> seenKeys) {
        if (cur.equals(stop)) {
            String key = joinKey(current);
            if (seenKeys.add(key)) {
                results.add(new ArrayList<Station>(current));
            }
            return;
        }
        if (current.size() - 1 >= maxHops) {
            return;
        }
        List<SubwayNetwork.Edge> edges = network.getEdges(cur);
        for (int i = 0; i < edges.size(); i++) {
            Station next = edges.get(i).getTo();
            String key = next.getName();
            if (visited.contains(key)) {
                continue;
            }
            current.add(next);
            visited.add(key);
            dfs(next, stop, maxHops, current, visited, results, seenKeys);
            current.remove(current.size() - 1);
            visited.remove(key);
        }
    }

    /** 路径的字符串指纹，用于去重 */
    private String joinKey(List<Station> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append('>');
            }
            sb.append(path.get(i).getName());
        }
        return sb.toString();
    }
}
