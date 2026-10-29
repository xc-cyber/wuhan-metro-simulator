package metro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 乘车方案打印类。
 * 严格按《关于路径的函数说明》第4问规定的格式输出：
 *
 *   输入：&lt;天河机场、航空总部、宋家岗、巨龙大道、盘龙城&gt;
 *   输出：乘2号线[天河机场，盘龙城]
 *
 *   输入：&lt;宝通寺、中南路、梅苑小区、武昌火车站&gt;
 *   输出：乘2号线[宝通寺，中南路]，换乘4号线[中南路，武昌火车站]
 *
 * 规则：把路径按线路切段，同一线路的连续区间合并为一段，
 * 段内只显示首末两站；换乘站既是上一段的终点，也是下一段的起点。
 */
public class PathPrinter {

    private static final String COMMA = "，";

    private final SubwayNetwork network;

    public PathPrinter(SubwayNetwork network) {
        this.network = network;
    }

    /**
     * 第4问：用直观的方式打印给定路径对应的乘车方式。
     *
     * @param path 包含从起点到终点在内、顺序经过的所有站点的数组
     */
    public void printPath(List<Station> path) {
        System.out.println(buildRidePlan(path));
    }

    /** 生成乘车方案文本（不打印），便于单元测试与文档引用 */
    public String buildRidePlan(List<Station> path) {
        if (path == null || path.isEmpty()) {
            return "路径为空";
        }
        if (path.size() == 1) {
            return "起点与终点相同：" + path.get(0).getName();
        }
        // 每段的站点列表，段内属于同一条线路
        List<List<Station>> groups = splitByLine(path);
        List<String> lines = linesOfGroups(groups);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            List<Station> group = groups.get(i);
            if (i == 0) {
                sb.append("乘").append(lines.get(i));
            } else {
                sb.append(COMMA).append("换乘").append(lines.get(i));
            }
            sb.append('[');
            sb.append(group.get(0).getName());
            sb.append(COMMA);
            sb.append(group.get(group.size() - 1).getName());
            sb.append(']');
        }
        return sb.toString();
    }

    /**
     * 把路径切分为若干“单线路段”。
     *
     * 采用动态规划求换乘次数最少的切法：
     *   state[i][L] = 前 i 个区段已经乘完、且最后一段乘坐线路 L 时的最小换乘次数。
     * 相邻区段若存在共线（同一条线路都能坐），就可以不换乘地连下去；
     * 否则必须在中间站点换乘，次数 +1。
     * 这样得到的段数最少，且每段内线路唯一，符合“乘X号线[a，b]”的表达要求。
     */
    public List<List<Station>> splitByLine(List<Station> path) {
        int sectionCount = path.size() - 1;
        List<List<String>> options = new ArrayList<List<String>>();
        for (int i = 0; i < sectionCount; i++) {
            options.add(network.commonLines(path.get(i), path.get(i + 1)));
        }

        List<Map<String, Integer>> states = new ArrayList<Map<String, Integer>>();
        // 初始化第 0 段
        Map<String, Integer> base = new LinkedHashMap<String, Integer>();
        List<String> firstOptions = options.get(0);
        for (int k = 0; k < firstOptions.size(); k++) {
            base.put(firstOptions.get(k), Integer.valueOf(0));
        }
        states.add(base);

        List<Map<String, String>> backtrace = new ArrayList<Map<String, String>>();
        backtrace.add(new LinkedHashMap<String, String>());

        for (int i = 1; i < sectionCount; i++) {
            Map<String, Integer> current = new LinkedHashMap<String, Integer>();
            Map<String, String> from = new LinkedHashMap<String, String>();
            List<String> curOptions = options.get(i);
            Map<String, Integer> last = states.get(i - 1);
            for (int k = 0; k < curOptions.size(); k++) {
                String line = curOptions.get(k);
                int best = Integer.MAX_VALUE;
                String bestPrev = null;
                for (Map.Entry<String, Integer> entry : last.entrySet()) {
                    String prevLine = entry.getKey();
                    int cost = entry.getValue().intValue() + (prevLine.equals(line) ? 0 : 1);
                    if (cost < best) {
                        best = cost;
                        bestPrev = prevLine;
                    }
                }
                current.put(line, Integer.valueOf(best));
                from.put(line, bestPrev);
            }
            states.add(current);
            backtrace.add(from);
        }

        // 选末段换乘次数最少的线路
        Map<String, Integer> tail = states.get(sectionCount - 1);
        String bestLine = null;
        int bestCost = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : tail.entrySet()) {
            if (entry.getValue().intValue() < bestCost) {
                bestCost = entry.getValue().intValue();
                bestLine = entry.getKey();
            }
        }

        // 回溯每段线路
        String[] sectionLine = new String[sectionCount];
        String cursor = bestLine;
        for (int i = sectionCount - 1; i >= 0; i--) {
            sectionLine[i] = cursor;
            if (i > 0) {
                cursor = backtrace.get(i).get(cursor);
            }
        }

        // 按线路变化切段
        List<List<Station>> groups = new ArrayList<List<Station>>();
        int start = 0;
        for (int i = 1; i <= sectionCount; i++) {
            boolean end = (i == sectionCount) || !sectionLine[i].equals(sectionLine[i - 1]);
            if (end) {
                List<Station> group = new ArrayList<Station>();
                for (int k = start; k <= i; k++) {
                    group.add(path.get(k));
                }
                groups.add(group);
                start = i;
            }
        }
        return groups;
    }

    /** 每个分段所用的线路名（按段内首末两站的公共线路确定） */
    public List<String> linesOfGroups(List<List<Station>> groups) {
        List<String> names = new ArrayList<String>();
        for (int g = 0; g < groups.size(); g++) {
            List<Station> group = groups.get(g);
            List<String> options = network.commonLines(group.get(0), group.get(1));
            names.add(options.isEmpty() ? "未知线路" : options.get(0));
        }
        return names;
    }
}
