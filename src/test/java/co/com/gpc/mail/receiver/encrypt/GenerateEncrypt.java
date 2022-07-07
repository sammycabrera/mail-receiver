/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.encrypt;

import co.com.gpc.mail.receiver.util.security.UtilSecurity;

/**
 *
 * @author Sammy
 */
public class GenerateEncrypt {
    private static String text= "emailpassword";
    private static String passEmail ="XF/MDZm/w7pkRQzCkAV/jQs20/l/l74HiSXyGclSzycklitE+NFc7w==";
    private static String algorithm= "PBEWithMD5AndDES";
    private static String generatorClassname= "org.jasypt.iv.RandomIvGenerator";
    
    public static void main (String args[]){
        String decryptPass = UtilSecurity.decrypt(text, passEmail, algorithm, generatorClassname);
        
        System.out.println(decryptPass);
    }
    //Ejecutar en terminal
    //    mvn jasypt:decrypt-value -Djasypt.encryptor.password="emailpassword" -Djasypt.plugin.value="XF/MDZm/w7pkRQzCkAV/jQs20/l/l74HiSXyGclSzycklitE+NFc7w=="
    //    mvn jasypt:encrypt-value -Djasypt.encryptor.password="emailpassword" -Djasypt.plugin.value="facturacionfegpc31"


}
