import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class Main {
    private JFrame frame;
    private JButton browseButton;
    private JButton convertButton;
    private JProgressBar totalProgressBar;
    private JLabel statusLabel;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private DefaultTableModel tableModel;
    private JTable fileTable;
    private Vector<File> selectedFiles = new Vector<>();

    public Main() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Flactory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout(10, 10));

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        browseButton = new JButton("Add Files");
        convertButton = new JButton("Convert All");
        convertButton.setEnabled(false);
        topPanel.add(browseButton);
        topPanel.add(convertButton);

        // Create table for file list
        String[] columnNames = {"File Name", "Status", "Progress"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fileTable = new JTable(tableModel);
        fileTable.getColumnModel().getColumn(2).setCellRenderer(new ProgressBarRenderer());
        JScrollPane scrollPane = new JScrollPane(fileTable);
        
        // Bottom panel for total progress
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        totalProgressBar = new JProgressBar();
        totalProgressBar.setStringPainted(true);
        statusLabel = new JLabel("Ready");
        bottomPanel.add(totalProgressBar, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);

        // Add button listeners
        browseButton.addActionListener(e -> selectFiles());
        convertButton.addActionListener(e -> convertFiles());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(frame);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (!selectedFiles.contains(file)) {
                    selectedFiles.add(file);
                    tableModel.addRow(new Object[]{
                        file.getName(),
                        "Pending",
                        0
                    });
                }
            }
            convertButton.setEnabled(!selectedFiles.isEmpty());
            updateTotalProgress();
        }
    }

    private void convertFiles() {
        browseButton.setEnabled(false);
        convertButton.setEnabled(false);
        totalProgressBar.setValue(0);
        totalProgressBar.setMaximum(selectedFiles.size());
        int totalFiles = selectedFiles.size();
        
        for (int i = 0; i < selectedFiles.size(); i++) {
            final int fileIndex = i;
            File inputFile = selectedFiles.get(i);
            File outputFile = new File(inputFile.getParent(), 
                inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.')) + ".flac");

            tableModel.setValueAt("Converting...", fileIndex, 1);
            
            executorService.submit(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "d:/Documents/Java/Projects/flactory/utils/flac.exe",
                        "-8",
                        "-f",
                        "-o", outputFile.getAbsolutePath(),
                        inputFile.getAbsolutePath()
                    );
                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt(exitCode == 0 ? "Completed" : "Failed", fileIndex, 1);
                        tableModel.setValueAt(100, fileIndex, 2);
                        updateTotalProgress();
                        
                        // Check if all conversions are complete
                        boolean allDone = true;
                        for (int row = 0; row < tableModel.getRowCount(); row++) {
                            String status = (String) tableModel.getValueAt(row, 1);
                            if (status.equals("Converting...") || status.equals("Pending")) {
                                allDone = false;
                                break;
                            }
                        }
                        
                        if (allDone) {
                            browseButton.setEnabled(true);
                            convertButton.setEnabled(true);
                            statusLabel.setText("All conversions completed!");
                        }
                    });
                } catch (IOException | InterruptedException e) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt("Error: " + e.getMessage(), fileIndex, 1);
                        tableModel.setValueAt(0, fileIndex, 2);
                        updateTotalProgress();
                    });
                }
            });
        }
    }

    private void updateTotalProgress() {
        int completed = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, 1);
            if (status.equals("Completed") || status.equals("Failed")) {
                completed++;
            }
        }
        totalProgressBar.setValue(completed);
        statusLabel.setText(String.format("Converted %d of %d files", completed, tableModel.getRowCount()));
    }

    // Custom renderer for progress column
    private static class ProgressBarRenderer extends JProgressBar implements javax.swing.table.TableCellRenderer {
        public ProgressBarRenderer() {
            super(0, 100);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setValue((int) (value == null ? 0 : value));
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
