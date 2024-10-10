package com.demo;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

	
	import javax.crypto.Cipher;
	import javax.crypto.spec.SecretKeySpec;
	import java.util.Base64;

	public class EncryptCredentials {
		
	    private static final String ALGORITHM = "AES";
	    private static final String SECRET_KEY = "pm#dds$rxz4jhsdt";  // 16 characters for AES-128

	    public static void main(String[] args) throws Exception {
	    	
	        //String username = "Administrator";
	       //String password = "Admin@123";
	        
	        String username = "vijay.nikam@ext.jiofinance.in";
	        String password="Clover#2024";

	        // Encrypt the credentials
	        String encryptedUsername = encrypt(username, SECRET_KEY);
	        String encryptedPassword = encrypt(password, SECRET_KEY);

	        System.out.println("Encrypted Username: " + encryptedUsername);
	        System.out.println("Encrypted Password: " + encryptedPassword);

	        // Decrypt the credentials
	        String decryptedUsername = decrypt(encryptedUsername, SECRET_KEY);
	        String decryptedPassword = decrypt(encryptedPassword, SECRET_KEY);

	        System.out.println("Decrypted Username: " + decryptedUsername);
	        System.out.println("Decrypted Password: " + decryptedPassword);
	    }

	    // AES Encryption with Base64 encoding
	    public static String encrypt(String data, String secretKey) throws Exception {
	        Cipher cipher = Cipher.getInstance(ALGORITHM);
	        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
	        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
	        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
	        return Base64.getEncoder().encodeToString(encryptedBytes); // Convert to Base64 string
	    }

	    // AES Decryption with Base64 decoding
	    public static String decrypt(String encryptedData, String secretKey) throws Exception {
	        Cipher cipher = Cipher.getInstance(ALGORITHM);
	        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
	        cipher.init(Cipher.DECRYPT_MODE, keySpec);
	        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData); // Decode from Base64 string
	        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
	        return new String(decryptedBytes); // Convert to String
	    }
	

}
