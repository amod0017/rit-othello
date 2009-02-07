/**
 * 
 */

/**
 * Bitboard data structure implementation of OthelloBoard
 * 
 * this data structure, although dense and hard to read, is needed for
 * 'industrial stength' rapid move generation and move execution.
 * 
 * Notations used in this file:
 * Rx : Row x on the board
 * Cx : Column x on the board
 * DA0 : Ascending Diagonal (no offset)
 * DD0 : Descending Diagonal (no offset)
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloBitBoard implements OthelloBoard {
	long white;
	long black;
	
	/**
	 * construct new board from a bitboard of each piece type
	 * 
	 * @param white : bitboard corrosponding to white piece placement
	 * @param black : bitboard corrosponding to black peice placement
	 */
	OthelloBitBoard(long white, long black) {
		this.white = white;
		this.black = black;
	}
	
	/**
	 * Copy an OthelloBitBoard
	 * 
	 * @param toCopy
	 */
	public OthelloBitBoard(OthelloBitBoard toCopy) {
		white = toCopy.white;
		black = toCopy.black;
	}
	
	/**
	 * merges x, y coordinate pairs into a single value, (0-63)
	 * 
	 * @param x : x coordinate, (0, 7)
	 * @param y : y coordinate, (0, 7)
	 * @return an index, in range 0-63
	 */
	static int xyMerge(int x, int y) {
		return x | (y << 3);
	}
	
	/**
	 * extracts x coordinate from an 'xy' int
	 * 
	 * @param xy : position (0-63)
	 * @return extracted x coordinate
	 */
	static int xyTox(int xy) {
		return xy & 7;
	}
	
	/**
	 * extracts y coordinate from an 'xy' int
	 * 
	 * @param xy : position (0-63)
	 * @return extracted y coordinate
	 */
	static int xyToy(int xy) {
		return xy >> 3;
	}
	
	/**
	 * remaps the bit values in C1 to be the bit values of R1
	 * 
	 * @param x : the bitboard
	 * @return new bitboard
	 */
	private static int mapC1toR1(long x) {
		x &= 0x0101010101010101L;
		x |= x >> 28;
		x |= x >> 14;
		x |= x >> 7;
		
		return (int)x & 0xFF;
	}
	
	/**
	 * remaps the bit values in DA0 (the ascending diagonal) to be the bit 
	 * values of R1
	 * 
	 * @param x : the bitboard
	 * @return new bitboard
	 */
	private static int mapDA0ToR1(long x) {
		x &= 0x8040201008040201L;
		x |= x >> 32;
		x |= x >> 16;
		x |= x >> 8;
		return (int)x & 0xFF;
	}
	
	/**
	 * remaps the bit values in DD0 (the descending diagonal) to be the bit 
	 * values of R1
	 * 
	 * @param x : the bitboard
	 * @return new bitboard
	 */
	private static int mapDD0ToR1(long x) {
		x &= 0x0102040810204080L;
		x |= x >> 32;
		x |= x >> 16;
		x |= x >> 8;
		return (int)x & 0xFF;
	}
	
	/**
	 * remaps the bit values in R1 to be the bit values of C1
	 * the output is empty except for C1.  
	 * 
	 * @param x : the bitboard
	 * @return new bitboard
	 */
	private static long mapR1toC1(int x) {
		x |= (x & 0xAA) << 7;
		x |= (x & 0x4444) << 14;
		long z = (long)x | ((long)(x & 0x10101010) << 28);
		
		return z & 0x0101010101010101L;
	}
	
	/**
	 * remaps the bit values in R1 to be the bit values of DA0 
	 * (ascending diagonal)
	 * the output is empty except for DA0.  
	 * 
	 * @param x : the bitboard
	 * @return new bitboard 
	 */
	private static long mapR1toDA0(int x) {
		x |= (x & 0xAA) << 8;
		x |= (x & 0x8844) << 16;
		long z = (long)x | ((long)(x & 0x80402010) << 32);
		
		return z & 0x8040201008040201L;
	}
	
	/**
	 * remaps the bit values in R1 to be the bit values of DD0 
	 * (descending diagonal)
	 * the output is empty except for DD0.  
	 * 
	 * @param x : the bitboard
	 * @return new bitboard 
	 */
	private static long mapR1toDD0(int x) {
		x |= (x & 0x55) << 8;
		x |= (x & 0x1122) << 16;
		long z = (long)x | ((long)(x & 0x1020408) << 32);
		
		return z & 0x0102040810204080L;
	}
	
	/**
	 * empties the board
	 */
	public void clear() {
		white = 0L;
		black = 0L;
	}

	/**
	 * @param state : set as either WHITE or BLACK
	 * @return the number of one color's pieces on the board
	 */
	public int countPieces(int state) {
		if (state == WHITE) {
			return BitUtil.countSetBits(white);
		} else {
			return BitUtil.countSetBits(black);
		}
	}
	
	/**
	 * @param state : set as either WHITE or BLACK
	 * @return true if this player can make a move
	 */
	public boolean canMove(int state) {
		for (long toTry = generateLikelyMoves(state); toTry != 0; toTry &= toTry-1) {
			int square = BitUtil.ulog2(BitUtil.lowSetBit(toTry));
			if (moveIsLegal(xyTox(square & 7), xyToy(square >> 3), state)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * @return true if the game is over (neither player can move)
	 */
	public boolean gameIsSet() {
		return canMove(WHITE) || canMove(BLACK);
	}

	/**
	 * Simply sets the state of the square (x, y). 
	 * Does not verify the legality of the move in any way
	 * 
	 * @param x : horizontal coordinate (0-7)
	 * @param y : vertical coordinate (0-7)
	 */
	public int getSquare(int x, int y) {
		if (((1L << xyMerge(x, y)) & white) != 0) return WHITE;
		if (((1L << xyMerge(x, y)) & black) != 0) return BLACK;
		return EMPTY;
	}

	/**
	 * Performs a move and all updating involved in an in-game move.
	 * The move is assumed to be legal.
	 * 
	 * @param x : proposed horizontal coordinate (0-7)
	 * @param y : proposed vertical coordinate (0-7)
	 * @param state : set as either WHITE or BLACK
	 */
	public void makeMove(int x, int y, int state) {
		OthelloBitBoard newBoard = copyAndMakeMove(x, y, state);
		white = newBoard.white;
		black = newBoard.black;
	}
	
	/**
	 * Creates a copy of the game with a move applied.
	 * Performs a move and all updating involved in an in-game move.
	 * The move is assumed to be legal.
	 * Aggressively optimized for speed.
	 * 
	 * @param x : proposed horizontal coordinate (0-7)
	 * @param y : proposed vertical coordinate (0-7)
	 * @param state : set as either WHITE or BLACK
	 * @return updated copy of the board
	 */
	public OthelloBitBoard copyAndMakeMove(int x, int y, int state) {
		long cColor;
		long eColor;
		
		if (state == WHITE) {
			cColor = white;
			eColor = black;
		} else {
			cColor = black;
			eColor = white;
		}
		
		int cRow;
		int eRow;

		//compute row modifications
		cRow = (byte)(cColor >>> (8*y));
		eRow = (byte)(eColor >>> (8*y));
		cRow = computeRowEffect(cRow, eRow, x);
		eRow &= ~cRow;
		cColor = (byte)(cRow << (8*y));
		eColor = (byte)(eRow << (8*y));
	
		//compute column modifications
		cRow = mapC1toR1(cColor >>> x);
		eRow = mapC1toR1(eColor >>> x);
		cRow = computeRowEffect(cRow, eRow, y);
		eRow &= ~cRow;
		cColor = mapR1toC1(cRow) << x;
		eColor = mapR1toC1(eRow) << x;
		
		//compute DA0 modifications
		byte shiftDistance = (byte)((x - y) << 3);
		cRow = mapDA0ToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDA0ToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		cRow = computeRowEffect(cRow, eRow, x);
		eRow &= ~cRow;
		cColor = BitUtil.signedLeftShift(mapR1toDA0(cRow), (byte)-shiftDistance);
		eColor = BitUtil.signedLeftShift(mapR1toDA0(eRow), (byte)-shiftDistance);
		
		//compute DD0 modifications
		shiftDistance = (byte)((7 - x - y) << 3); // distance needed to map to DD0
		cRow = mapDD0ToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDD0ToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		cRow = computeRowEffect(cRow, eRow, x);
		eRow &= ~cRow;
		cColor = BitUtil.signedLeftShift(mapR1toDD0(cRow), (byte)-shiftDistance);
		eColor = BitUtil.signedLeftShift(mapR1toDD0(eRow), (byte)-shiftDistance);
		
		if (state == WHITE) {
			return new OthelloBitBoard(cColor, eColor);
		} else {
			return new OthelloBitBoard(eColor, cColor);
		}
	}

	/**
	 * Test to see if player 'state' (BLACK or WHITE) can move at (x, y)
	 * Aggressively optimized for speed.
	 * 
	 * @param x: proposed x coordinate
	 * @param y: proposed y coordinate
	 * @param state: WHITE or BLACK
	 * @return true if move is legal
	 */
	public boolean moveIsLegal(int x, int y, int state) {
		long cColor;
		long eColor;
		
		if (state == WHITE) {
			cColor = white;
			eColor = black;
		} else {
			cColor = black;
			eColor = white;
		}
		
		int cRow;
		int eRow;
		
		//check for capture on row
		cRow = (byte)(cColor >>> (8*y));
		eRow = (byte)(eColor >>> (8*y));
		if (computeRowEffect(cRow, eRow, x) != 0) {
			return true;
		}
		
		//check for capture on column
		cRow = mapC1toR1(cColor >>> x);
		eRow = mapC1toR1(eColor >>> x);
		if (computeRowEffect(cRow, eRow, y) != 0) {
			return true;
		}
		
		//check for capture on ascending diagonal
		byte shiftDistance = (byte)((x - y) << 3); // distance needed to map to DA0
		cRow = mapDA0ToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDA0ToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		if (computeRowEffect(cRow, eRow, x) != 0) {
			return true;
		}
		
		//check for capture on descending diagonal
		shiftDistance = (byte)((7 - x - y) << 3); // distance needed to map to DD0
		cRow = mapDD0ToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDD0ToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		if (computeRowEffect(cRow, eRow, x) != 0) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * private table-lookup function. Given a row of friendly and enemy pieces,
	 * and a proposed square to move, what will happen to the row? The answer
	 * is stored in the table.
	 * 
	 * @param mRow : 8-bit bitboard corrosponding to friendly pieces
	 * @param eRow : 8-bit bitboard corrosponding to enemy pieces
	 * @param pos : proposed place to move
	 * @return the new mrow (friendly pieces) after making the move
	 */
	private byte computeRowEffect(int mRow, int eRow, int pos) {
		return Rom.ROWLOOKUP[mRow | (eRow << 8) | (pos << 16)];
	}

	/**
	 * load the starting position of the game
	 */
	public void newGame() {
		white = 0x0000000810000000L;
		black = 0x0000001008000000L;
	}

	/**
	 * Simply sets the state of the square (x, y). 
	 * Does not verify the legality of the move in any way
	 * 
	 * @param x : horizontal coordinate (0-7)
	 * @param y : vertical coordinate (0-7)
	 */
	public void setSquare(int x, int y, int state) {
		long v = (1L << xyMerge(x, y));
		
		switch (state) {
		case WHITE:
			white |= v;
			break;
		case BLACK:
			black |= v;
			break;
		case EMPTY:
			v = ~v;
			white &= v;
			black &= v;
			break;
		}
	}
	
	/**
	 * generates a bitboard where each bit is 'likely' a legal move.
	 * 
	 * @param state : BLACK or WHITE
	 * @return all bits that are empty squares and next to an enemy piece
	 */
	public long generateLikelyMoves(int state) {
		long emptySpace = ~(white | black);
		
		if (state == WHITE) {
			return fillNeighbors(black) & emptySpace;
		} else {
			return fillNeighbors(white) & emptySpace;
		}
	}
	
	/**
	 * @param v : original bitboard
	 * @return new bitboard that is all of the adjacent bits to every bit in v
	 */
	private long fillNeighbors(long v) {
		v |= (v << 1) & 0xFEFEFEFEFEFEFEFEL;
		v |= (v << 8);
		v |= (v >> 1) & 0x7F7F7F7F7F7F7F7FL;
		v |= (v >> 8);
		return v;
	}
	
	public String toString() {
		return "[" + Long.toHexString(white) + ", " + Long.toHexString(black) + "]";
	}

	/**
	 * test-drive the bitboard engine
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final int numTests = 100;
		
		OthelloBitBoard testBoardA = new OthelloBitBoard(0x00000010081C0000L, 0x0000000000020200L);
		OthelloBitBoard testBoardB = new OthelloBitBoard(0x1020000001801400L, 0x0018102204080000L);
		
		for (int i = 0; i < numTests; ++i) {
			Object output = null;
			Object expectedOutput = null;
			
			switch (i) {
			case 0:
				output = new Boolean(testBoardA.moveIsLegal(5, 5, BLACK));
				expectedOutput = new Boolean(true);
				break;
			case 1:
				output = new Boolean(testBoardA.moveIsLegal(5, 2, BLACK));
				expectedOutput = new Boolean(true);
				break;
			case 2:
				output = new Boolean(testBoardA.moveIsLegal(4, 2, BLACK));
				expectedOutput = new Boolean(false);
				break;
			case 3:
				output = new Boolean(testBoardA.moveIsLegal(2, 4, BLACK));
				expectedOutput = new Boolean(false);
				break;
			case 4:
				output = new Boolean(testBoardB.moveIsLegal(2, 5, WHITE));
				expectedOutput = new Boolean(true);
				break;
			case 5:
				output = new Boolean(testBoardB.moveIsLegal(3, 7, WHITE));
				expectedOutput = new Boolean(false);
				break;
			case 6:
				output = new Boolean(testBoardB.moveIsLegal(4, 4, WHITE));
				expectedOutput = new Boolean(true);
				break;
			case 7:
				output = new Boolean(testBoardB.moveIsLegal(0, 5, WHITE));
				expectedOutput = new Boolean(true);
				break;
			default:
				continue;
			}
			
			boolean fail = false;
			System.out.println("Test " + i + ":");
			
			if (!output.equals(expectedOutput)) {
				fail = true;
				System.out.println("\tOutput: " + output.toString());
				System.out.println("\tExpected Output: " + expectedOutput.toString());
			}
			
			if (!fail) {
				System.out.println("\tPassed!");
			}
			
		}
		
	}

}
