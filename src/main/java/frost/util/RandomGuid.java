/*
 RandomGuid.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; either version 2 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package frost.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * This class is used to generate random GUIDs of the type
 * F45C47D0-FF4E-11D8-9669-0800200C9A66 (38 characters long, 
 * including dashes).
 * 
 * @author $Author: bback $
 * @version $Revision: 3134 $
 */
public class RandomGuid {

	private static Random random = new Random();
	private String guid;
	
	/**
	 * This creates a new instance of RandomGuid. 
	 * @throws NoSuchAlgorithmException if the instance could not be created because
	 * 					the "MD5" algorithm is not available
	 */
	public RandomGuid() throws NoSuchAlgorithmException {
		generateGuid();
	}
	
	/**
	 * This method generates the random guid
	 */
	private void generateGuid() throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		StringBuilder stringToDigest = new StringBuilder();
		
		long time = System.currentTimeMillis();
		long rand = random.nextLong();
		
		stringToDigest.append(time);
		stringToDigest.append("-");
		stringToDigest.append(rand);
		
		md5.update(stringToDigest.toString().getBytes());
		byte[] digestBytes = md5.digest();	//This is always 128 bits long.
		StringBuilder digest = new StringBuilder();
		for (int i = 0; i < digestBytes.length; ++i) {
			int b = digestBytes[i] & 0xFF;
			if (b < 0x10) {
				digest.append('0');
			}
			digest.append(Integer.toHexString(b));
		}
		guid = digest.toString();
	}
	
	/** 
	 * This method returns a String representation of the guid in
	 * the standard format for GUIDs, like F45C47D0-FF4E-11D8-9669-0800200C9A66
	 * @see Object#toString()
	 */	
	@Override
    public String toString() {
		String guidUpperCase = guid.toUpperCase();
		StringBuilder sb = new StringBuilder();
		
		sb.append(guidUpperCase.substring(0, 8));
		sb.append("-");
		sb.append(guidUpperCase.substring(8, 12));
		sb.append("-");
		sb.append(guidUpperCase.substring(12, 16));
		sb.append("-");
		sb.append(guidUpperCase.substring(16, 20));
		sb.append("-");
		sb.append(guidUpperCase.substring(20));

		return sb.toString();
	}
	
}
