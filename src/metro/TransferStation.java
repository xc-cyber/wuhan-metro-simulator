package metro;

import java.util.ArrayList;
import java.util.List;

/**
 * 中转站记录类。
 * 对应作业功能一要求的记录形式：&lt;中南路站，&lt;2号线、4号线&gt;&gt;
 */
public class TransferStation {

    private final Station station;

    public TransferStation(Station station) {
        this.station = station;
    }

    public Station getStation() {
        return station;
    }

    public String getName() {
        return station.getName();
    }

    /** 经过该站的所有线路 */
    public List<String> getLines() {
        return new ArrayList<String>(station.getLines());
    }

    /** 格式：&lt;中南路站，&lt;2号线、4号线&gt;&gt; */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(station.getName());
        if (!station.getName().endsWith("站")) {
            sb.append("站");
        }
        sb.append("，<");
        List<String> lines = station.getLines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(lines.get(i));
        }
        sb.append(">>");
        return sb.toString();
    }
}
