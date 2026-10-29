package metro;

/**
 * 乘车段：一次连续乘坐某条线路从 from 到 to。
 */
public class RideSegment {

    private final String lineName;
    private final Station from;
    private final Station to;
    private final double distance;

    public RideSegment(String lineName, Station from, Station to, double distance) {
        this.lineName = lineName;
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public String getLineName() {
        return lineName;
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return lineName + "[" + from.getName() + "，" + to.getName() + "]";
    }
}
