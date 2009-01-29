/**
 * 
 */

/**
 * @author Administrator
 *
 */
public class OthelloBitBoard implements OthelloBoard {
	long white;
	long black;
	
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
	
	static long mapC1toDiag(long x) {
		x = (x & 0x0000000001010101L) | ((x << 4) & 0x1010101000000000L);
		
		long y = (x & 0x1010000001010000L);
		x ^= y;
		x |= y << 2;
		
		y = (x & 0x4000100004000100L);
		x ^= y;
		x = y << 1;
		
		return x;
	}
	
	static long mapC1toR1(long x) {
		x = (x & 0x0000000001010101L) | ((x >> 28) & 0x1010101000000000L);
		
		long y = (x & 0x0000000011110000L);
		x ^= y;
		x |= y >> 14;
		
		y = (x & 0x0000000000005500L);
		x ^= y;
		x = y >> 7;
		
		return x;
	}
	
	@Override
	public void clear() {
		white = 0L;
		black = 0L;
	}

	@Override
	public int countPieces(int state) {
		if (state == WHITE) {
			return BitUtil.countSetBits(white);
		} else {
			return BitUtil.countSetBits(black);
		}
	}

	@Override
	public boolean gameIsSet() {

		return false;
	}

	@Override
	public int getSquare(int x, int y) {
		if (((1L << xyMerge(x, y)) & white) != 0) return WHITE;
		if (((1L << xyMerge(x, y)) & black) != 0) return BLACK;
		return EMPTY;
	}

	@Override
	public void makeMove(int x, int y, int state) {
		
	}

	@Override
	public boolean moveIsLegal(int x, int y, int state) {
		
		return true;
	}

	@Override
	public void newGame() {
		white = 0x0000000810000000L;
		black = 0x0000001008000000L;
	}

	@Override
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
	 * @param args
	 */
	public static void main(String[] args) {
		
	
	}

}
