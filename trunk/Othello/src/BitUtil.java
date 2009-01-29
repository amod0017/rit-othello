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
		val = (val & 0x5555555555555555L) + ((val >> 1 ) & 0x5555555555555555L);
		val = (val & 0x3333333333333333L) + ((val >> 2 ) & 0x3333333333333333L);
		val = (val & 0x0F0F0F0F0F0F0F0FL) + ((val >> 4 ) & 0x0F0F0F0F0F0F0F0FL);
		val = (val & 0x00FF00FF00FF00FFL) + ((val >> 8 ) & 0x00FF00FF00FF00FFL);
		val = (val & 0x0000FFFF0000FFFFL) + ((val >> 16) & 0x0000FFFF0000FFFFL);
		val = (val & 0x00000000FFFFFFFFL) + ((val >> 32));
	    return (byte)val;
	}
	
	/**
	 * @param val : a 64bit value, treated as unsigned
	 * @return the index of the highest set bit in val
	 */
	static byte ulog2 (long val) {
		byte k = 0;
		if(val - 0x00000000FFFFFFFFL > 0) {val>>>=32; k = 32;}
		if(val > 0x000000000000FFFFL) {val>>=16; k|= 16;}
		if(val > 0x00000000000000FFL) {val>>= 8; k|=  8;}
		k |= LOG2LOOKUP[(byte)val];

		return k;
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
