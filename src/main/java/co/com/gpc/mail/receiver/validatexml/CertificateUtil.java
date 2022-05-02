/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Decoder;;

public class CertificateUtil {
 /**
  * 
  * @param encodedString
  *            base64 encoded string
  * 
  * @return X509Certificate from the encodedString, null on failure cases
  */
 public static X509Certificate getX509Certificate(String encodedString) {
  if (encodedString == null) {
   return null;
  }

  Decoder decoder = Base64.getDecoder();
  byte[] decodedData = decoder.decode(encodedString);

  try (InputStream inputStream = new ByteArrayInputStream(decodedData)) {
   CertificateFactory cf = CertificateFactory.getInstance("X.509");

   java.security.cert.Certificate certificate = cf.generateCertificate(inputStream);

   if (certificate instanceof X509Certificate) {
       X509Certificate cet = (X509Certificate) certificate;
       System.out.println("Issuer Name : " + cet.getIssuerX500Principal());
    return cet;
   }

  } catch (Exception e) {
   e.printStackTrace();
  }

  return null;

 }
}