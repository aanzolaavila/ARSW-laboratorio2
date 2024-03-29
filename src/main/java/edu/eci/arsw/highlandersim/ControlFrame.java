package edu.eci.arsw.highlandersim;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollBar;

public class ControlFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_IMMORTAL_HEALTH = 100;
    private static final int DEFAULT_DAMAGE_VALUE = 10;

    private static AtomicBoolean isPaused;
    private static AtomicBoolean isStopped;
    private static AtomicInteger immortalsPaused;
    private static Thread originalThread;

    private final JPanel contentPane;

    private List<Immortal> immortals;

    private final JTextArea output;
    private final JLabel statisticsLabel;
    private final JScrollPane scrollPane;
    private final JTextField numOfImmortals;

    private final JButton btnStart;
    private final JButton btnStop;

    /**
     * Launch the application.
     *
     * @param args
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    ControlFrame frame = new ControlFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public ControlFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 647, 248);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JToolBar toolBar = new JToolBar();
        contentPane.add(toolBar, BorderLayout.NORTH);

        isPaused = new AtomicBoolean();
        isStopped = new AtomicBoolean();
        immortalsPaused = new AtomicInteger();
        originalThread = Thread.currentThread();

        btnStart = new JButton("Start");
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlFrame.this.startGame();
            }
        });
        toolBar.add(btnStart);

        JButton btnPauseAndCheck = new JButton("Pause and check");
        btnPauseAndCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlFrame.this.pauseGame();
            }
        });
        toolBar.add(btnPauseAndCheck);

        JButton btnResume = new JButton("Resume");

        btnResume.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlFrame.this.resumeGame();
            }
        });

        toolBar.add(btnResume);

        JLabel lblNumOfImmortals = new JLabel("num. of immortals:");
        toolBar.add(lblNumOfImmortals);

        numOfImmortals = new JTextField();
        numOfImmortals.setText("3");
        toolBar.add(numOfImmortals);
        numOfImmortals.setColumns(10);

        btnStop = new JButton("STOP");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlFrame.this.stopGame();
            }
        });
        btnStop.setForeground(Color.RED);
        toolBar.add(btnStop);

        scrollPane = new JScrollPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);

        output = new JTextArea();
        output.setEditable(false);
        scrollPane.setViewportView(output);

        statisticsLabel = new JLabel("Immortals total health:");
        contentPane.add(statisticsLabel, BorderLayout.SOUTH);

    }

    public List<Immortal> setupInmortals() {

        ImmortalUpdateReportCallback ucb = new TextAreaUpdateReportCallback(output, scrollPane);

        try {
            int ni = Integer.parseInt(numOfImmortals.getText());

            List<Immortal> il = new LinkedList<>();
            ImmortalCleaner.getInstance(il, isStopped);

            for (int i = 0; i < ni; i++) {
                Immortal i1 = new Immortal("im" + i, il, DEFAULT_IMMORTAL_HEALTH, DEFAULT_DAMAGE_VALUE, ucb, isPaused, isStopped, originalThread);
                il.add(i1);
            }
            return il;
        } catch (NumberFormatException e) {
            JOptionPane.showConfirmDialog(null, "Número inválido.");
            return null;
        }

    }

    private void startGame() {
        immortals = setupInmortals();

        if (immortals != null) {
            for (Immortal im : immortals) {
                im.start();
            }
        }

        ImmortalCleaner.getInstance().start();

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void stopGame() {
        isStopped.set(true);
        btnStop.setEnabled(false);
        this.pauseGame();
        System.out.println("Game stopped");
    }

    private void resumeGame() {
        isPaused.set(false);
        synchronized (originalThread) {
            originalThread.notifyAll();
        }
    }

    private void pauseGame() {
        isPaused.set(true);

        while(immortalsPaused.get() != immortals.size()) {
            System.out.println(immortalsPaused.get());
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(ControlFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        int sum = 0;
        for (Immortal im : immortals) {
            sum += im.getHealth();
        }

        statisticsLabel.setText("<html>" + immortals.toString() + "<br>Health sum:" + sum);
        System.out.println("Game paused (Size of list: " + immortals.size() + ")");
    }

    public static void reportImmortalPaused() {
        immortalsPaused.addAndGet(1);
    }
    
    public static void reportImmortalResumed() {
        immortalsPaused.addAndGet(-1);
    }
}

class TextAreaUpdateReportCallback implements ImmortalUpdateReportCallback {

    JTextArea ta;
    JScrollPane jsp;

    public TextAreaUpdateReportCallback(JTextArea ta, JScrollPane jsp) {
        this.ta = ta;
        this.jsp = jsp;
    }

    @Override
    public void processReport(String report) {
        ta.append(report);

        //move scrollbar to the bottom
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JScrollBar bar = jsp.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        }
        );
    }
}
