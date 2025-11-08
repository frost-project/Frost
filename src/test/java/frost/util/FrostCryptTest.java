package frost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import frost.Core;
import frost.identities.LocalIdentity;
import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class FrostCryptTest {

	private static final String message = "GNU GENERAL PUBLIC LICENSE" + "\n" + "Version 3, 29 June 2007" + "\n"
			+ "Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>" + "\n"
			+ "Everyone is permitted to copy and distribute verbatim copies " + "\n"
			+ "of this license document, but changing it is not allowed." + "\n" + "Preamble" + "\n"
			+ "The GNU General Public License is a free, copyleft license for" + "\n"
			+ "software and other kinds of works.";

	private LocalIdentity getIdentityBob() {
		String[] keys = {
				"AM9dfGLZBxOcLPniJaAE5HQuScpw8ONJa+jXGc/iYUQ3Va5r0gwsIlti4wEuCVJhE5RqE58LWt7bsqcFGKP94tlZik7LXpCZfUATFRzOzrB1O08YvUznRONL3DuRXryvSCQkN+fYpxXPkimi8wIIG96V0W/Q0mlxmZk02bs6SlXF:CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:AXzXAf2lsIdBL24fv0DKBvzazHtaCprEuYYj+a7/XVElhdifwNtWDtuCSLyRCPWMzJWWolrZncIHA4Jy9yk6WUTf2qI/qAC6J+DFyOPFNQpArcUXS09Pz2aX0rBwfHOIKm0E9V5PGbMWTX6WA5zmtos71w+ZMrCntzJxskMFsqU=:AP8r3QWF01ayhm55qqo4X3wqLFglhR721wMkiKkohfwcW6zGqQ/U1FqTxLnSiGq2fulJNndeIU4HnhjVsrXpIgM=:ANAJ4P4INlZIFA7v9nZal8G4J/dnoNxDI7rTMgSO8dePIM+BfvsVH0s0X0mkmjn6mEpt1ox6FzoImxYriy6rwpc=:H0Enypp86FuN0J6i8W701gh3siN8jI79Pc4dFg+KHpWrTtBWwsGtuopTKVu8kR4Sxy6ZiLgQOXRwW7ClWSacbw==:bnFa8NlG7XxUWNnVAwxlvdL6MTQWD3jDmvbkLGzj35BdXINhCPCD/G5WxZp7FyC9IIYdw3ta4x9Ausgvn6gxCw==:AO+J1C/3mv8zAbid9/NeY8QYPHpt17JJ1Kcqct46UtIzXXS96DoU2F8r0OLiwDlUacSSjOEnq+PpmOdh1S1pXlI=",
				"CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:AM9dfGLZBxOcLPniJaAE5HQuScpw8ONJa+jXGc/iYUQ3Va5r0gwsIlti4wEuCVJhE5RqE58LWt7bsqcFGKP94tlZik7LXpCZfUATFRzOzrB1O08YvUznRONL3DuRXryvSCQkN+fYpxXPkimi8wIIG96V0W/Q0mlxmZk02bs6SlXF" };
		return new LocalIdentity("Bob@krg2VX7skEZAbePHJ3RrzqBT+dk", keys);
	}

	@Test
	public void encryptAndDecryptMessage() {
		LocalIdentity bob = getIdentityBob();

		// Alice sends a message to Bob
		byte[] encryptedMessage = Core.getCrypto().encrypt(message.getBytes(), bob.getPublicKey());

		// Bob receives the message from Alice
		byte[] decryptedMessage = Core.getCrypto().decrypt(encryptedMessage, bob.getPrivateKey());

		assertEquals(message, new String(decryptedMessage, StandardCharsets.UTF_8));
	}

	@Test
	public void signAndVerifyMessage() {
		LocalIdentity bob = getIdentityBob();

		// Bob signs a message
		String signature = Core.getCrypto().detachedSign(message, bob.getPrivateKey());

		// Alice verifies the signature
		Boolean isSignatureValid = Core.getCrypto().detachedVerify(message, bob.getPublicKey(), signature);
		assertEquals(true, isSignatureValid);

		// Somebody changes the message. Alice verifies the signature again
		isSignatureValid = Core.getCrypto().detachedVerify(message + "test", bob.getPublicKey(), signature);
		assertEquals(false, isSignatureValid);
	}
}
