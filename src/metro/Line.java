package metro;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路类。
 * 一条线路按行车顺序保存站点序列，以及相邻两站之间的里程。
 */
public class Line {

    /** 线路名称，如“2号线”“阳逻线” */
    private final String name;

    /** 按行车顺序排列的站点 */
    private final List<Station> stations;

    /** distances.get(i) 表示 stations[i] 到 stations[i + 1] 的里程（公里） */
    private final List<Double> distances;

    public Line(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("线路名称不能为空");
        }
        this.name = name.trim();
        this.stations = new ArrayList<Station>();
        this.distances = new ArrayList<Double>();
    }

    public String getName() {
        return name;
    }

    public List<Station> getStations() {
        return new ArrayList<Station>(stations);
    }

    public List<Double> getDistances() {
        return new ArrayList<Double>(distances);
    }

    public int stationCount() {
        return stations.size();
    }

    /** 区间数量（站点数减一） */
    public int sectionCount() {
        return distances.size();
    }

    /** 线路总里程 */
    public double totalLength() {
        double sum = 0;
        for (int i = 0; i < distances.size(); i++) {
            sum += distances.get(i).doubleValue();
        }
        return sum;
    }

    /**
     * 追加一个区段：上一站 -> station，间距 distance。
     * 若线路为空，则先把 station 作为首站放入。
     */
    public void appendSection(Station station, double distance) {
        if (stations.isEmpty()) {
            addStation(station);
            return;
        }
        Station last = stations.get(stations.size() - 1);
        if (last.equals(station)) {
            // 原地重复的防御：不允许 0 长度区间
            return;
        }
        stations.add(station);
        station.addLine(name);
        distances.add(Double.valueOf(distance));
    }

    /** 单独加入首站 */
    public void addStation(Station station) {
        if (stations.isEmpty() || !stations.get(stations.size() - 1).equals(station)) {
            stations.add(station);
            station.addLine(name);
        }
    }

    /** 站点在本线路中的序号，不在本线返回 -1 */
    public int indexOf(Station station) {
        return stations.indexOf(station);
    }

    /** 该站在本线路上与下一站的距离；终点站返回 0 */
    public double distanceToNext(int index) {
        if (index < 0 || index >= distances.size()) {
            return 0;
        }
        return distances.get(index).doubleValue();
    }

    /** 判断给定站点序列是否为本线路的连续区间（同向或反向） */
    public boolean isContinuousSection(List<Station> path) {
        if (path == null || path.size() < 2) {
            return false;
        }
        int from = indexOf(path.get(0));
        int to = indexOf(path.get(path.size() - 1));
        if (from < 0 || to < 0) {
            return false;
        }
        int step = from <= to ? 1 : -1;
        int expected = Math.abs(to - from) + 1;
        if (expected != path.size()) {
            return false;
        }
        for (int i = 0; i < path.size(); i++) {
            int idx = from + step * i;
            if (!stations.get(idx).equals(path.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
