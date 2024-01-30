package editor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
    private final JFileChooser jfc;
    JTextArea textArea;
    LinkedList<MatchResult> locationList;
    int index;
    private File workingFile;

    public TextEditor() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
//        setLayout(null);
        setLocationRelativeTo(null);
        setTitle(this.getClass().getSimpleName());

        JPanel upPanel = new JPanel();

        JTextField searchInputField = new JTextField(30);
        searchInputField.setName("SearchField");
        searchInputField.setPreferredSize(new Dimension(100, 22));

        JButton loadButton = new JButton(new ImageIcon("resources/open.png"));
        loadButton.setName("OpenButton");
        JButton saveButton = new JButton(new ImageIcon("resources/save.png"));
        saveButton.setName("SaveButton");
        JButton searchButton = new JButton(new ImageIcon("resources/search.png"));
        searchButton.setName("StartSearchButton");
        JButton prevButton = new JButton(new ImageIcon("resources/left.png"));
        prevButton.setName("PreviousMatchButton");
        JButton nextButton = new JButton(new ImageIcon("resources/right.png"));
        nextButton.setName("NextMatchButton");
        JCheckBox regCheck = new JCheckBox("Use Regex");
        regCheck.setName("UseRegExCheckbox");

        jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jfc.setName("FileChooser");
        jfc.setDialogTitle("Select text file");
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("Text Documents (*.txt)", "txt"));

        add(jfc);

        this.textArea = new JTextArea();
        textArea.setName("TextArea");
        // textArea.setBounds(10, 10, this.getWidth() - 20, this.getHeight() - 20);
        JScrollPane scrollBox = new JScrollPane(textArea);
        scrollBox.setName("ScrollPane");


        add(upPanel, BorderLayout.NORTH);

        upPanel.add(loadButton, FlowLayout.LEFT);
        upPanel.add(saveButton, FlowLayout.LEFT);
        upPanel.add(searchInputField, FlowLayout.RIGHT);
        upPanel.add(searchButton, FlowLayout.RIGHT);
        upPanel.add(prevButton);
        upPanel.add(nextButton);
        upPanel.add(regCheck);

        add(scrollBox, BorderLayout.CENTER);

        saveButton.addActionListener(actionEvent -> {
            int returnValue = jfc.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                workingFile = jfc.getSelectedFile();
                try (FileWriter fileWriter = new FileWriter(workingFile)) {
                    fileWriter.write(textArea.getText());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        loadButton.addActionListener(actionEvent -> {

            int returnValue = jfc.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {


                workingFile = jfc.getSelectedFile();
                try (FileReader fileReader = new FileReader(workingFile)) {
                    textArea.read(fileReader, null);
                } catch (FileNotFoundException e) {
                    textArea.setText("");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        searchButton.addActionListener(actionEvent -> {
            new SwingWorkerSearch(searchInputField.getText(), textArea.getText(), regCheck.isSelected()).execute();
        });
        nextButton.addActionListener(actionEvent -> {
            if (!locationList.isEmpty()) {
                index = index == locationList.size() - 1 ? 0 : index + 1;
                updateSelection(textArea, locationList.get(index % locationList.size()));
            }
        });
        prevButton.addActionListener(actionEvent -> {
            if (!locationList.isEmpty()) {
                index = index == 0 ? locationList.size() - 1 : index - 1;
                updateSelection(textArea, locationList.get(index));
            }
        });
//Menu stuff
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
//file
        JMenu file = new JMenu("File");
        file.setName("MenuFile");

        menuBar.add(file);

        JMenuItem menuLoad = new JMenuItem("Open");
        menuLoad.setName("MenuOpen");
        file.add(menuLoad);
        JMenuItem menuSave = new JMenuItem("Save");
        menuSave.setName("MenuSave");
        file.add(menuSave);
        file.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.setName("MenuExit");
        file.add(exit);
// search
        JMenu search = new JMenu("Search");
        search.setName("MenuSearch");

        menuBar.add(search);

        JMenuItem menuStartSearch = new JMenuItem("Start search");
        menuStartSearch.setName("MenuStartSearch");
        search.add(menuStartSearch);
        JMenuItem menuPrevMatch = new JMenuItem("Previous search");
        menuPrevMatch.setName("MenuPreviousMatch");
        search.add(menuPrevMatch);
        JMenuItem menuNextMatch = new JMenuItem("Next match");
        menuNextMatch.setName("MenuNextMatch");
        search.add(menuNextMatch);
        JMenuItem menuUseRegex = new JMenuItem("Use regular expressions");
        menuUseRegex.setName("MenuUseRegExp");
        search.add(menuUseRegex);

        menuStartSearch.addActionListener(searchButton.getActionListeners()[0]);
        menuPrevMatch.addActionListener(prevButton.getActionListeners()[0]);
        menuNextMatch.addActionListener(nextButton.getActionListeners()[0]);
        menuUseRegex.addActionListener(actionEvent -> {
            regCheck.setSelected(!regCheck.isSelected());
        });

        menuSave.addActionListener(saveButton.getActionListeners()[0]);
        menuLoad.addActionListener(loadButton.getActionListeners()[0]);

        exit.addActionListener(actionEvent -> {
            this.dispose();
        });
        setVisible(true);
        pack();
    }

    void updateSelection(JTextArea area, MatchResult result) {
        int index = result.start();
        int length = result.group().length();
        area.setCaretPosition(index + length);
        area.select(index, index + length);
        area.grabFocus();
    }

    class SwingWorkerSearch extends SwingWorker<LinkedList<MatchResult>, LinkedList<MatchResult>> {
        private final String searchBody;
        private final String searchString;
        private final boolean useRegex;

        SwingWorkerSearch(String searchString, String searchBody, boolean useRegex) {
            this.searchString = searchString;
            this.searchBody = searchBody;
            this.useRegex = useRegex;
        }

        @Override
        public LinkedList<MatchResult> doInBackground() {
            Pattern pattern = useRegex ? Pattern.compile(searchString) : Pattern.compile(Pattern.quote(searchString));
            Matcher matcher = pattern.matcher(searchBody);
            return new LinkedList<>(matcher.results().toList());


        }

        @Override
        protected void done() {
            try {
                locationList = get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (!locationList.isEmpty()) {
                updateSelection(textArea, locationList.getFirst());
            }
            index = 0;

        }
    }
}
