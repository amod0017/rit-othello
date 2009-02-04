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
	/*
	static byte mapC1toR1(long x) {
		x = (x & 0x0000000001010101L) | ((x >> 28) & 0x1010101000000000L);
		
		long y = (x & 0x0000000011110000L);
		x ^= y;
		x |= y >> 14;
		
		y = (x & 0x0000000000005500L);
		x ^= y;
		x = y >> 7;
		
		return (byte)x;
	}*/
	
	private static int mapC1toR1(long x) {
		x &= 0x0101010101010101L;
		x |= x >> 28;
		x |= x >> 14;
		x |= x >> 7;
		
		return (int)x;
	}
	
	private static int mapDiagToR1(long x) {
		x &= 0x8040201008040201L;
		x |= x >> 32;
		x |= x >> 16;
		x |= x >> 8;
		return (int)x;
	}
	
	private static long mapR1toC1(int x) {
		x |= (x & 0xAA) << 7;
		x |= (x & 0x4444) << 14;
		long z = (long)x | ((long)(x & 0x10101010) << 28);
		
		return z & 0x0101010101010101L;
	}
	
	private static long mapR1toDiag(int x) {
		x |= (x & 0xAA) << 8;
		x |= (x & 0x8844) << 16;
		long z = (long)x | ((long)(x & 0x80402010) << 32);
		
		return z & 0x8040201008040201L;
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
		
		//compute UL diagonal modifications
		byte shiftDistance = (byte)((x - y) << 3);
		cRow = mapDiagToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDiagToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		cRow = computeRowEffect(cRow, eRow, x);
		eRow &= ~cRow;
		cColor = BitUtil.signedLeftShift(mapR1toDiag(cRow), (byte)-shiftDistance);
		eColor = BitUtil.signedLeftShift(mapR1toDiag(eRow), (byte)-shiftDistance);
		
		if (state == WHITE) {
			white = cColor;
			black = eColor;
		} else {
			white = eColor;
			black = cColor;
		}
	}

	@Override
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
		
		//test row placement placement
		cRow = (byte)(cColor >>> (8*y));
		eRow = (byte)(eColor >>> (8*y));
		if (computeRowEffect(cRow, eRow, x) != 0) {
			return true;
		}
		
		//test Column placement
		cRow = mapC1toR1(cColor >>> x);
		eRow = mapC1toR1(eColor >>> x);
		if (computeRowEffect(cRow, eRow, y) != 0) {
			return true;
		}
		
		//test Diagonal placement
		byte shiftDistance = (byte)((x - y) << 3);
		cRow = mapDiagToR1(BitUtil.signedLeftShift(cColor, shiftDistance));
		eRow = mapDiagToR1(BitUtil.signedLeftShift(eColor, shiftDistance));
		if (computeRowEffect(cRow, eRow, x) != 0) {
			return true;
		}
		
		return false;
	}
	
	private byte computeRowEffect(int mRow, int eRow, int pos) {
		return Rom.ROWLOOKUP[mRow | (eRow << 8) | (pos << 16)];
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
