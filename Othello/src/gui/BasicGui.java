package gui;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 * Basic GUI to build board configurations.
 *
 * @author Joseph Pecoraro
 */
public class BasicGui extends JPanel {

	/** Gameboard */
	private JLabel[][] m_gameboard;

	/** Constants */
	static final int ROWS = 8;
	static final int COLS = 8;

	/**
	 * Constructor creates the board
	 */
    public BasicGui() {

    	// 8x8 Game Board (rows x cols)
        super(new GridLayout(ROWS, COLS));

        // Images
        // TODO: Create Images!
        // ImageIcon emptyImage = new ImageIcon("images/blah.gif", "empty");
        // ImageIcon blackImage = new ImageIcon("images/blah.gif", "black");
        // ImageIcon whiteImage = new ImageIcon("images/blah.gif", "white");

        // Creating the Labels
        m_gameboard = new JLabel[ROWS][COLS];
        for (int i=0; i<ROWS; ++i) {
        	for (int j=0; j<COLS; j++) {

        		// Create each label
        		JLabel lbl = new JLabel();
        		lbl.setPreferredSize( new Dimension(50, 50) );
        		lbl.setBorder( BorderFactory.createLineBorder(Color.black) );

        		// Click Listener
        		final int ii = i;
        		final int jj = j;
        		lbl.addMouseListener( new MouseAdapter() {
        			public void mousePressed(MouseEvent e) {
        				System.out.println("Clicked " + ii + ":" + jj);
        			}
        		});

        		// Add to the game board and display
				m_gameboard[i][j] = lbl;
				add( m_gameboard[i][j] );

			}
        }


    }


    /**
     * Default way to run a GUI.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Othello Basic GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new BasicGui());
        frame.pack();
        frame.setVisible(true);
    }


    /**
     * Driver
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	UIManager.put("swing.boldMetal", Boolean.FALSE);
            	createAndShowGUI();
            }
        });
    }
}
