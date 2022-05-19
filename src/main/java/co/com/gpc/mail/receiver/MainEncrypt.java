/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver;


import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 *
 * @author scabrera
 */
public class MainEncrypt {
    
    public static StringEncryptor stringEncryptor(String secretKey) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(secretKey);
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    public static String encrypt(String text, String secretKey) {
        StringEncryptor textEncryptor = stringEncryptor(secretKey);
        String encryptedText = textEncryptor.encrypt(text);
        return encryptedText;
    }

    public static String decrypt(String text, String secretKey) {
        StringEncryptor textEncryptor = stringEncryptor(secretKey);
        String decryptedText = textEncryptor.decrypt(text);
        return decryptedText;
    } 

public static void main(String[] args) {
    //System.out.println(encrypt("facturacionfegpc31","emailpassword"));
    System.out.println(decrypt("XF/MDZm/w7pkRQzCkAV/jQs20/l/l74HiSXyGclSzycklitE+NFc7w==","emailpassword"));
}    
}
