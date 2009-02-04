/**
 * Class that contains a library of bit manipulation algorithms
 * 
 * @author Nicholas Ver Hoeve
 */
public class BitUtil {
	/**
	 * counts set bits by doing parallel simultaneous sums
	 * 
	 * @param val : long to analyze
	 * @return the number of set bits in val
	 */
	static byte countSetBits(long val) {
		val = (val & 0x5555555555555555L) + ((val >>> 1 ) & 0x5555555555555555L);
		val = (val & 0x3333333333333333L) + ((val >>> 2 ) & 0x3333333333333333L);
		val = (val & 0x0F0F0F0F0F0F0F0FL) + ((val >>> 4 ) & 0x0F0F0F0F0F0F0F0FL);
		val = (val & 0x00FF00FF00FF00FFL) + ((val >>> 8 ) & 0x00FF00FF00FF00FFL);
		val = (val & 0x0000FFFF0000FFFFL) + ((val >>> 16) & 0x0000FFFF0000FFFFL);
		val = (val & 0x00000000FFFFFFFFL) + ((val >>> 32));
	    return (byte)val;
	}
	
	/**
	 * @param val : a 64bit value, treated as unsigned
	 * @return the index of the highest set bit in val
	 */
	static byte ulog2 (long val) {
		byte k = 0;
		if((val & 0xFFFFFFFF00000000L) != 0) {val>>>=32; k = 32;}
		if(val > 0x000000000000FFFFL) {val>>>=16; k|= 16;}
		if(val > 0x00000000000000FFL) {val>>>= 8; k|=  8;}
		k |= LOG2LOOKUP[(short)val];

		return k;
	}
	
	/**
	 * Determines which way to shift based on the sign of the amount to shift
	 * 
	 * @param x : a long
	 * @param signedAmount : amount to shift - can be negative!
	 * @return return x << signedAmount, or x >>> -signedAmount
	 */
	static long signedLeftShift(long x, byte signedAmount) {
		return signedAmount >= 0 ? x << signedAmount : x >>> -signedAmount;
	}
	
	static byte ulog2 (byte val) {
		return LOG2LOOKUP[(int)val & 0xFF];
	}
	
	static int lowSetBit(int x) {
		return (x & (x-1)) ^ x;
	}
	
	//TODO: replace with lookup table?
	public static byte inbetweenZone(byte x) {
		return  (byte)(((1 << ulog2(x)) - 1) & (~x ^ (x-1)));
	}
	
	public static int highSetBit(int val) {
		if (val == 0) return 0; 

		int ret = 1;
		if((val & 0xFFFF0000) != 0) {val>>>=16; ret = 0x10000;}
		if(val > 0x000000FF) {val>>>= 8; ret <<= 8;}
		ret <<= LOG2LOOKUP[val];
		return ret;
	}
	
	static private byte LOG2LOOKUP[] = new byte[] {
		0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
		5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};


}
