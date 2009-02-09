package gui;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

import core.OthelloBitBoard;
import core.OthelloBoard;

/**
 * Basic GUI to build board configurations.
 *
 * @author Joseph Pecoraro
 */
public class BasicGui extends JPanel {

	/** Game board **/
	private JLabel[][] m_gameboard;
	private OthelloBoard m_othello;
	private int m_player;

	/** Constants **/
	static final int ROWS = 8;
	static final int COLS = 8;

	/** Globals **/
	private static ImageIcon emptyImage;
	private static ImageIcon blackImage;
	private static ImageIcon whiteImage;

	/**
	 * Constructor creates the board
	 */
    public BasicGui() {

    	// 8x8 Game Board (rows x cols)
        super(new GridLayout(ROWS, COLS));

        // Constructor creates a new game automatically
        m_othello = new OthelloBitBoard();
        m_player = m_othello.canMove( OthelloBoard.BLACK ) ? OthelloBoard.BLACK : OthelloBoard.WHITE;

        // Images
        emptyImage = new ImageIcon("images/empty.gif", "empty");
        blackImage = new ImageIcon("images/black.gif", "black");
        whiteImage = new ImageIcon("images/white.gif", "white");

        // Creating the Board
        m_gameboard = new JLabel[ROWS][COLS];
        for (int i=0; i<ROWS; ++i) {
        	for (int j=0; j<COLS; ++j) {

        		// Create each label
        		JLabel lbl = new JLabel(emptyImage);
        		lbl.setPreferredSize( new Dimension(50, 50) );
        		lbl.setBorder( BorderFactory.createLineBorder(Color.black) );

        		// Use the OthelloBoard for determining initial game states
        		switch ( m_othello.getSquare(i, j) ) {
				case OthelloBoard.WHITE:
					lbl.setIcon(whiteImage);
					break;
				case OthelloBoard.BLACK:
					lbl.setIcon(blackImage);
					break;
				}

        		// Click Listener
        		final int x = i;
        		final int y = j;
        		lbl.addMouseListener( new MouseAdapter() {
        			public void mousePressed(MouseEvent e) {

        				// Debug
        				System.out.println("Clicked " + x + ":" + y);

        				// When it is a Legal Move
        				if ( m_othello.moveIsLegal(x, y, m_player) ) {
        					m_othello.makeMove(x, y, m_player);
        					togglePlayer();
        					updateBoard();

            				// Game is over
            				// TODO: For now it prints results to stdout
            				if ( m_othello.gameIsSet() ) {
            					int black = m_othello.countPieces(OthelloBoard.BLACK);
            					int white = m_othello.countPieces(OthelloBoard.WHITE);
            					System.out.println("Results:");
            					System.out.println("Black: " + black);
            					System.out.println("White: " + white);
            				}

        				}

        			}
        		});

        		// Add to the game board and display
				m_gameboard[i][j] = lbl;
				add( m_gameboard[i][j] );

			}
        }


    }

    /**
     * Redraw the board
     */
    private void updateBoard() {
        for (int x=0; x<ROWS; ++x) {
        	for (int y=0; y<COLS; ++y) {
        		switch ( m_othello.getSquare(x, y) ) {
        		case OthelloBoard.EMPTY:
        			m_gameboard[x][y].setIcon(emptyImage);
					break;
				case OthelloBoard.WHITE:
					m_gameboard[x][y].setIcon(whiteImage);
					break;
				case OthelloBoard.BLACK:
					m_gameboard[x][y].setIcon(blackImage);
					break;
				}
        	}
        }
    }

    /**
     * Toggle the player
     */
    private void togglePlayer() {
    	m_player = m_player == OthelloBoard.WHITE ? OthelloBoard.BLACK : OthelloBoard.WHITE;
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
