package metro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 站点类。
 * 一个物理站点只创建一个 Station 对象，所有经过它的线路都记录在 lines 中。
 * 换乘站即 lines.size() > 1 的站点。
 */
public class Station {

    /** 站点名称，如“中南路” */
    private final String name;

    /** 经过该站点的线路名称列表，按录入顺序保存，如 [2号线, 4号线] */
    private final List<String> lines;

    public Station(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("站点名称不能为空");
        }
        this.name = name.trim();
        this.lines = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    /** 经过本站的线路（只读视图） */
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /** 是否为换乘站 */
    public boolean isTransferStation() {
        return lines.size() > 1;
    }

    /** 登记一条经过本站的线路；重复登记不会重复添加 */
    public void addLine(String lineName) {
        if (lineName != null && !lines.contains(lineName)) {
            lines.add(lineName);
        }
    }

    /** 判断两条线路是否在本站交汇（即本站能否完成该换乘） */
    public boolean canTransferBetween(String from, String to) {
        return lines.contains(from) && lines.contains(to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Station other = (Station) obj;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
