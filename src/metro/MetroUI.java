package metro;

import metro.exception.InvalidInputException;
import metro.exception.StationNotFoundException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * 图形界面（选做题，+10 分）。
 *
 * 设计要点：界面层不含任何业务算法，只负责“收集输入 → 调用 SubwaySystem →
 * 把返回的字符串显示到文本区”。所有路径搜索、换乘切分、票价计算仍然由
 * 下层的 PathFinder / PathPrinter / FareCalculator 完成，符合分层设计原则。
 */
public class MetroUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private SubwaySystem system;

    private final JComboBox<String> startBox = new JComboBox<>();
    private final JComboBox<String> endBox = new JComboBox<>();
    private final JComboBox<String> ticketBox = new JComboBox<>();
    private final JSpinner hopSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 60, 1));
    private final JTextArea output = new JTextArea();

    public MetroUI() {
        super("武汉地铁模拟系统");
        initSystem();
        initView();
        setSize(980, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /** 装载数据；失败则弹窗后退出，避免界面空转 */
    private void initSystem() {
        try {
            system = new SubwaySystem();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "数据装载失败：" + e.getMessage(),
                    "启动错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initView() {
        setLayout(new BorderLayout(8, 8));

        // ---------- 顶部：查询条件 ----------
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // 起点 / 终点：把全部站名塞进下拉框，支持直接输入前缀筛选
        startBox.setEditable(true);
        endBox.setEditable(true);
        for (String name : system.getNetwork().getStationNames()) {
            startBox.addItem(name);
            endBox.addItem(name);
        }
        startBox.setPreferredSize(new Dimension(170, 28));
        endBox.setPreferredSize(new Dimension(170, 28));

        ticketBox.addItem(FareCalculator.TicketType.SINGLE.getLabel());
        ticketBox.addItem(FareCalculator.TicketType.WUHAN_TONG.getLabel());
        ticketBox.addItem(FareCalculator.TicketType.DAY_PASS.getLabel());

        c.gridx = 0; c.gridy = 0; top.add(new JLabel("起点："), c);
        c.gridx = 1; top.add(startBox, c);
        c.gridx = 2; top.add(new JLabel("终点："), c);
        c.gridx = 3; top.add(endBox, c);
        c.gridx = 4; top.add(new JLabel("票种："), c);
        c.gridx = 5; top.add(ticketBox, c);
        c.gridx = 6; top.add(new JLabel("路径最大站数："), c);
        c.gridx = 7; top.add(hopSpinner, c);
        add(top, BorderLayout.NORTH);

        // ---------- 中部：结果区 ----------
        output.setEditable(false);
        output.setFont(new Font("SimSun", Font.PLAIN, 15));
        output.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(new TitledBorder("结果"));
        add(scroll, BorderLayout.CENTER);

        // ---------- 底部：功能按钮 ----------
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        bottom.add(button("查询最短路径", e -> doShortest()));
        bottom.add(button("查询全部路径", e -> doAllPaths()));
        bottom.add(button("查看中转站", e -> doTransfers()));
        bottom.add(button("查看线路", e -> doLines()));
        bottom.add(button("清空", e -> output.setText("")));

        add(bottom, BorderLayout.SOUTH);

        // 启动时先给一段引导信息
        output.setText("请选择起点与终点，然后点击「查询最短路径」。\n\n"
                + "已装载：" + system.getDataFile() + "\n"
                + "站点总数：" + system.getNetwork().getStationNames().size() + "\n");
    }

    private JButton button(String text, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(new Font("SimHei", Font.PLAIN, 14));
        b.addActionListener(action::actionPerformed);
        return b;
    }

    /* ==================== 各功能按钮的响应 ==================== */

    /** 功能四 + 五 + 七：最短路径、乘车方案、三种票种票价 */
    private void doShortest() {
        String start = selected(startBox);
        String end = selected(endBox);
        try {
            StringBuilder sb = new StringBuilder();
            List<Station> path = system.getShortestPath(start, end);

            sb.append("【最短路径】\n");
            sb.append(system.joinPath(path)).append("\n\n");

            sb.append("【乘车方案】\n");
            sb.append(system.getPathPrinter().buildRidePlan(path)).append("\n\n");

            sb.append("【票价】\n");
            sb.append(system.describeAllFareTypes(path)).append("\n");
            output.setText(sb.toString());
            output.setCaretPosition(0);
        } catch (InvalidInputException | StationNotFoundException ex) {
            warn(ex.getMessage());
        }
    }

    /** 功能三：全部无环路径，并逐条列出里程与票价 */
    private void doAllPaths() {
        String start = selected(startBox);
        String end = selected(endBox);
        int maxHops = (Integer) hopSpinner.getValue();
        try {
            List<List<Station>> paths = system.getAllPaths(start, end, maxHops);
            if (paths.isEmpty()) {
                output.setText("两站之间不存在路径（可能已超出最大站数限制，可调大后再试）");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(paths.size()).append(" 条无环路径：\n\n");
            int i = 1;
            for (List<Station> p : paths) {
                double km = system.getPathFinder().pathDistance(p);
                double fare = system.getFareCalculator()
                        .fare(km, FareCalculator.TicketType.SINGLE);
                sb.append("方案 ").append(i++).append("（")
                  .append(String.format("%.3f", km)).append(" km，")
                  .append((int) fare).append(" 元）\n");
                sb.append("  ").append(system.getPathPrinter().buildRidePlan(p)).append("\n\n");
            }
            output.setText(sb.toString());
            output.setCaretPosition(0);
        } catch (InvalidInputException | StationNotFoundException ex) {
            warn(ex.getMessage());
        }
    }

    /** 功能一：全部中转站 */
    private void doTransfers() {
        output.setText("【中转站列表】\n\n" + system.listTransferStations());
        output.setCaretPosition(0);
    }

    /** 附加：线路概览 */
    private void doLines() {
        output.setText("【线路概览】\n\n" + system.listAllLines());
        output.setCaretPosition(0);
    }

    private String selected(JComboBox<String> box) {
        Object v = box.getEditor().getItem();
        return v == null ? "" : v.toString().trim();
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        // 界面外观跟随系统（Windows 下为原生风格）
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new MetroUI().setVisible(true));
    }
}
