package metro;

import metro.exception.InvalidInputException;
import metro.exception.StationNotFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据读取类。
 * 负责把 subway.txt 解析成 Line 与 Station 组成的网络。
 *
 * 文件格式（按线路分块，12 条线路共 264 个区间）：
 *
 *     1号线站点间距
 *
 *     站点名称	间距（KM）
 *
 *     径河---三店	1.110
 *     三店---码头潭公园	1.971
 *     ......
 *
 * 说明：区间两端的连接符在文件中存在 “---” 与 “—” 两种写法，解析时统一兼容。
 */
public class SubwayFileReader {

    /** 支持的区段连接符，长串在前，避免 “—” 抢先匹配 */
    private static final String[] SEPARATORS = {"---", "——", "—", "－", "-"};

    public SubwayNetwork read(String filePath) throws IOException, InvalidInputException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("数据文件不存在：" + filePath);
        }
        return read(file);
    }

    public SubwayNetwork read(File file) throws IOException, InvalidInputException {
        SubwayNetwork network = new SubwayNetwork();
        BufferedReader reader = null;
        int lineNo = 0;
        String currentLineName = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String text;
            while ((text = reader.readLine()) != null) {
                lineNo++;
                String row = text.replace("\uFEFF", "").trim();
                if (row.isEmpty()) {
                    continue;
                }
                // 表头行：站点名称	间距（KM）
                if (row.startsWith("站点名称")) {
                    continue;
                }
                // 线路标题行：以“站点间距”结尾，如“1号线站点间距”
                int titleIndex = row.indexOf("站点间距");
                if (titleIndex > 0) {
                    currentLineName = row.substring(0, titleIndex).trim();
                    network.addLine(new Line(currentLineName));
                    continue;
                }
                if (currentLineName == null) {
                    throw new InvalidInputException("第 " + lineNo + " 行：区段数据出现在任何线路标题之前 -> " + row);
                }
                parseSection(network, currentLineName, row, lineNo);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return network;
    }

    /** 解析一行区段数据，形如 “径河---三店	1.110” */
    private void parseSection(SubwayNetwork network, String lineName, String row, int lineNo)
            throws InvalidInputException {
        int tab = row.indexOf('\t');
        if (tab < 0) {
            tab = row.lastIndexOf(' ');
        }
        if (tab < 0) {
            throw new InvalidInputException("第 " + lineNo + " 行：缺少间距数值 -> " + row);
        }
        String pairPart = row.substring(0, tab).trim();
        String distancePart = row.substring(tab + 1).trim();

        double distance;
        try {
            distance = Double.parseDouble(distancePart);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("第 " + lineNo + " 行：间距不是合法数字 -> " + distancePart);
        }
        if (distance <= 0) {
            throw new InvalidInputException("第 " + lineNo + " 行：间距必须为正数 -> " + distancePart);
        }

        String[] pair = splitPair(pairPart, lineNo);
        String fromName = pair[0];
        String toName = pair[1];

        Station from = network.getOrCreateStation(fromName);
        Station to = network.getOrCreateStation(toName);

        Line line = network.getLine(lineName);
        if (line == null) {
            throw new InvalidInputException("第 " + lineNo + " 行：线路 [" + lineName + "] 未登记");
        }
        // 保证首站入线
        if (line.stationCount() == 0) {
            line.addStation(from);
        }
        Station tail = line.getStations().get(line.stationCount() - 1);
        if (!tail.equals(from)) {
            // 兼容文件中区间反向书写的情况：若本行终点恰是线路尾站，则反向接入
            if (tail.equals(to)) {
                line.appendSection(from, distance);
            } else {
                throw new InvalidInputException("第 " + lineNo + " 行：区段 " + pairPart
                        + " 与线路 [" + lineName + "] 的走向不连续，当前尾站为 [" + tail.getName() + "]");
            }
        } else {
            line.appendSection(to, distance);
        }
        network.link(from, to, lineName, distance);
    }

    /** 按连接符切分 “站A---站B”，返回长度 2 的数组 */
    private String[] splitPair(String pairPart, int lineNo) throws InvalidInputException {
        for (int i = 0; i < SEPARATORS.length; i++) {
            String sep = SEPARATORS[i];
            int at = pairPart.indexOf(sep);
            if (at > 0) {
                String left = pairPart.substring(0, at).trim();
                String right = pairPart.substring(at + sep.length()).trim();
                // 消化 “站A——站B” 这类双字符写法
                while (right.startsWith("—") || right.startsWith("-")) {
                    right = right.substring(1).trim();
                }
                if (!left.isEmpty() && !right.isEmpty()) {
                    return new String[]{left, right};
                }
            }
        }
        throw new InvalidInputException("第 " + lineNo + " 行：无法识别区段两端站点 -> " + pairPart);
    }

    /** 若文件缺省，按常见工作目录自动定位 subway.txt */
    public static String locateDataFile() throws StationNotFoundException {
        String[] candidates = {
                "data/subway.txt", "subway.txt",
                "../data/subway.txt", "../subway.txt",
                "MetroSimulator/data/subway.txt", "../MetroSimulator/data/subway.txt"};
        for (int i = 0; i < candidates.length; i++) {
            File f = new File(candidates[i]);
            if (f.exists() && f.isFile()) {
                return f.getPath();
            }
        }
        throw new StationNotFoundException("未找到数据文件 subway.txt，请把 data 目录与程序放在同一工作目录下");
    }

    /** 便于外部预览统计信息 */
    public static Map<String, Integer> summarize(SubwayNetwork network) {
        Map<String, Integer> stat = new LinkedHashMap<String, Integer>();
        List<Line> lines = new ArrayList<Line>(network.getLines());
        int sections = 0;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            stat.put(line.getName(), Integer.valueOf(line.stationCount()));
            sections += line.sectionCount();
        }
        stat.put("区间总数", Integer.valueOf(sections));
        stat.put("站点总数", Integer.valueOf(network.stationCount()));
        stat.put("换乘站数", Integer.valueOf(network.transferStationCount()));
        return stat;
    }
}
