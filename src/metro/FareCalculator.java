package metro;

/**
 * 计价类。
 * 按武汉地铁现行分段计价规则，结合票种计算实付票价。
 *
 * 计价基数（普通单程票）：
 *   0  &lt; d &lt;= 4      -> 2 元
 *   4  &lt; d &lt;= 12     -> 每 4 公里加 1 元
 *   12 &lt; d &lt;= 24     -> 每 6 公里加 1 元
 *   24 &lt; d &lt;= 40     -> 每 8 公里加 1 元
 *   d  &gt; 40          -> 每 10 公里加 1 元（不足 1 段按 1 段计）
 */
public class FareCalculator {

    /** 票种 */
    public enum TicketType {

        SINGLE("单程票", 1.00),
        WUHAN_TONG("武汉通", 0.90),
        DAY_PASS("日票", 0.00);

        private final String label;
        private final double rate;

        TicketType(String label, double rate) {
            this.label = label;
            this.rate = rate;
        }

        public String getLabel() {
            return label;
        }

        public double getRate() {
            return rate;
        }
    }

    private static final double[] LIMITS = {4, 12, 24, 40};
    private static final int[] STEPS = {4, 6, 8, 10};

    /** 分段计价：返回普通单程票价格（单位：元） */
    public int baseFare(double distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("里程不能为负数");
        }
        if (distance <= LIMITS[0]) {
            return 2;
        }
        int fare = 2;
        double cursor = LIMITS[0];
        for (int i = 0; i < STEPS.length; i++) {
            double upper = (i + 1 < LIMITS.length) ? LIMITS[i + 1] : Double.MAX_VALUE;
            if (distance <= cursor) {
                break;
            }
            double span = Math.min(distance, upper) - cursor;
            int segments = (int) Math.ceil(span / STEPS[i]);
            fare += segments;
            cursor = upper;
        }
        return fare;
    }

    /** 按票种计算实付票价，保留一位小数 */
    public double fare(double distance, TicketType type) {
        if (type == null) {
            throw new IllegalArgumentException("票种不能为空");
        }
        if (type == TicketType.DAY_PASS) {
            return 0.0;
        }
        double paid = baseFare(distance) * type.getRate();
        return Math.round(paid * 10.0) / 10.0;
    }

    /** 说明本次计价走过的分段，便于报告展示 */
    public String describe(double distance, TicketType type) {
        StringBuilder sb = new StringBuilder();
        sb.append("里程 ").append(format(distance)).append(" km -> 单程票 ")
          .append(baseFare(distance)).append(" 元");
        if (type == TicketType.WUHAN_TONG) {
            sb.append("，武汉通 9 折 -> ").append(format(fare(distance, type))).append(" 元");
        } else if (type == TicketType.DAY_PASS) {
            sb.append("，日票不计费 -> 0.0 元");
        }
        return sb.toString();
    }

    private String format(double value) {
        return String.format("%.2f", Double.valueOf(value));
    }
}
