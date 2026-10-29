package metro;

import metro.exception.StationNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 地铁网络类。
 * 内部维护两份结构：
 *  1）lines：线路名 -> Line 对象；
 *  2）adjacency：站点名 -> 邻接区段表，供路径搜索使用。
 */
public class SubwayNetwork {

    /** 邻接边：从某站出发，可沿某条线路到达 to，里程 distance */
    public static class Edge {
        private final Station from;
        private final Station to;
        private final String lineName;
        private final double distance;

        public Edge(Station from, Station to, String lineName, double distance) {
            this.from = from;
            this.to = to;
            this.lineName = lineName;
            this.distance = distance;
        }

        public Station getFrom() {
            return from;
        }

        public Station getTo() {
            return to;
        }

        public String getLineName() {
            return lineName;
        }

        public double getDistance() {
            return distance;
        }

        @Override
        public String toString() {
            return from.getName() + "-(" + lineName + "," + distance + ")->" + to.getName();
        }
    }

    private final Map<String, Line> lines = new LinkedHashMap<String, Line>();
    private final Map<String, Station> stations = new LinkedHashMap<String, Station>();
    private final Map<String, List<Edge>> adjacency = new LinkedHashMap<String, List<Edge>>();

    // ------------------------------------------------------------------ 构建

    public void addLine(Line line) {
        lines.put(line.getName(), line);
    }

    public Line getLine(String lineName) {
        return lines.get(lineName);
    }

    public Collection<Line> getLines() {
        return new ArrayList<Line>(lines.values());
    }

    /**
     * 取站点对象；若为同名站首次出现则新建。
     * 换乘站天然复用同一个对象，因此不同线路的“中南路”指向同一实例。
     */
    public Station getOrCreateStation(String name) {
        Station station = stations.get(name);
        if (station == null) {
            station = new Station(name);
            stations.put(name, station);
            adjacency.put(name, new ArrayList<Edge>());
        }
        return station;
    }

    /** 建立双向邻接关系 */
    public void link(Station a, Station b, String lineName, double distance) {
        if (a.equals(b)) {
            return;
        }
        adjacency.get(a.getName()).add(new Edge(a, b, lineName, distance));
        adjacency.get(b.getName()).add(new Edge(b, a, lineName, distance));
    }

    // ------------------------------------------------------------------ 查询

    public List<Edge> getEdges(Station station) {
        List<Edge> list = adjacency.get(station.getName());
        return list == null ? new ArrayList<Edge>() : new ArrayList<Edge>(list);
    }

    public Station getStation(String name) {
        return stations.get(name);
    }

    /**
     * 按名称查找站点，找不到抛 StationNotFoundException。
     * 名字前后空格、全角空格都会被忽略。
     */
    public Station requireStation(String name) throws StationNotFoundException {
        if (name == null) {
            throw new StationNotFoundException("站点名称不能为空");
        }
        String key = name.replace('\u3000', ' ').trim();
        Station station = stations.get(key);
        if (station == null) {
            throw new StationNotFoundException("不存在名为 [" + key + "] 的站点");
        }
        return station;
    }

    public Collection<Station> getStations() {
        return new ArrayList<Station>(stations.values());
    }

    public int stationCount() {
        return stations.size();
    }

    public int transferStationCount() {
        int count = 0;
        for (Station s : stations.values()) {
            if (s.isTransferStation()) {
                count++;
            }
        }
        return count;
    }

    public List<Station> getTransferStations() {
        List<Station> result = new ArrayList<Station>();
        for (Station s : stations.values()) {
            if (s.isTransferStation()) {
                result.add(s);
            }
        }
        return result;
    }

    /** 所有站点名称（按录入顺序） */
    public List<String> getStationNames() {
        return new ArrayList<String>(stations.keySet());
    }

    /** 经过某条线路的所有站点 */
    public List<Station> stationsOfLine(String lineName) {
        Line line = lines.get(lineName);
        return line == null ? new ArrayList<Station>() : line.getStations();
    }

    /** 某站可换乘到的其它线路（不含 originLine） */
    public Set<String> transferOptions(Station station, String originLine) {
        Set<String> options = new LinkedHashSet<String>();
        List<String> all = station.getLines();
        for (int i = 0; i < all.size(); i++) {
            String name = all.get(i);
            if (!name.equals(originLine)) {
                options.add(name);
            }
        }
        return options;
    }

    /**
     * 站点直达可达的相邻站（只走一条区间，不换乘）。
     */
    public List<Station> neighbors(Station station) {
        List<Station> result = new ArrayList<Station>();
        List<Edge> edges = getEdges(station);
        for (int i = 0; i < edges.size(); i++) {
            Station to = edges.get(i).getTo();
            if (!result.contains(to)) {
                result.add(to);
            }
        }
        return result;
    }

    public boolean contains(String stationName) {
        return stations.containsKey(stationName);
    }

    /**
     * 找出 a、b 相邻两站所有可乘坐的线路（共线区间会产生多条候选）。
     */
    public List<String> commonLines(Station a, Station b) {
        Set<String> aLines = new LinkedHashSet<String>(a.getLines());
        List<String> result = new ArrayList<String>();
        List<String> bLines = b.getLines();
        for (int i = 0; i < bLines.size(); i++) {
            String name = bLines.get(i);
            if (aLines.contains(name) && !result.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }
}
