/**
 * Interface for any Othello board implementation
 * Othello is 8 by 8
 * 
 * @author Nicholas Ver Hoeve
 */
public interface OthelloBoard {
	//valid square states
	static final int EMPTY = -1;
	static final int WHITE = 0;
	static final int BLACK = 1;
	
	/**
	 * @param x : horizontal coordinate (0-7)
	 * @param y : vertical coordinate (0-7)
	 * @return the state of the square (x, y)
	 */
	public int getSquare(int x, int y);
	
	/**
	 * Simply sets the state of the square (x, y). 
	 * Does not verify the legality of the move in any way
	 * 
	 * @param x : horizontal coordinate (0-7)
	 * @param y : vertical coordinate (0-7)
	 */
	public void setSquare(int x, int y, int state);
	
	/**
	 * empties the board
	 */
	public void clear();
	
	/**
	 * puts the board in it's legal starting state
	 */
	public void newGame();
	
	/**
	 * @param x : proposed horizontal coordinate (0-7)
	 * @param y : proposed vertical coordinate (0-7)
	 * @param state : set as either WHITE or BLACK
	 * @return true if the move (x, y) is legal
	 */
	public boolean moveIsLegal(int x, int y, int state);
	
	/**
	 * Performs a move and all updating involved in an in-game move.
	 * The move is assumed to be legal.
	 * 
	 * @param x : proposed horizontal coordinate (0-7)
	 * @param y : proposed vertical coordinate (0-7)
	 * @param state : set as either WHITE or BLACK
	 */
	public void makeMove(int x, int y, int state);
	
	/**
	 * @param state : set as either WHITE or BLACK
	 * @return the number of one color's pieces on the board
	 */
	public int countPieces(int state);
	
	/**
	 * @return true if the game is over (neither player can move)
	 */
	public boolean gameIsSet();
}
