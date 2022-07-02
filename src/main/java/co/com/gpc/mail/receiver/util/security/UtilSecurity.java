/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.util.security;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 *
 * @author scabrera
 */
public class UtilSecurity {

    private UtilSecurity() {
        throw new IllegalStateException("UtilSecurity class");
    }
    
    public static StringEncryptor stringEncryptor(String secretKey,String algorithm, String ivgeneratorclassname) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(secretKey);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName(ivgeneratorclassname);
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    public static String encrypt(String text, String secretKey,String algorithm, String ivgeneratorclassname) {
        StringEncryptor textEncryptor = stringEncryptor(secretKey, algorithm, ivgeneratorclassname);
        return textEncryptor.encrypt(text);
    }

    public static String decrypt(String text, String secretKey, String algorithm, String ivgeneratorclassname) {
        StringEncryptor textEncryptor = stringEncryptor(secretKey, algorithm, ivgeneratorclassname);
        return textEncryptor.decrypt(text);
    }  
    
}
