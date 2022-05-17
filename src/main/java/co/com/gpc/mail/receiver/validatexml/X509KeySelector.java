/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import java.io.InputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.util.Enumeration;
import java.util.Iterator;
import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dom.*;
import javax.xml.crypto.dsig.keyinfo.*;

/**
 * @author scabrera
 */
public class X509KeySelector extends KeySelector {

    //TODO: sacar "DSA" y "RSA" como constantes en esta clase
    static boolean algEqualss(String algURI, String algName) {
        //TODO: simplemente simplifiqué el if, usando sugerencia de intellij
        return (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
                || (algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase("RSA"));
    }

    static boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase("DSA")
                && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return true;
        } else if (algName.equalsIgnoreCase("RSA")
                && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return true;
        }
        //TODO: simplemente simplifiqué el if, usando sugerencia de intellij
        else return algName.equalsIgnoreCase("RSA")
                && algURI.equalsIgnoreCase("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    }

    public KeySelectorResult select(KeyInfo keyInfo,
                                    KeySelector.Purpose purpose,
                                    AlgorithmMethod method,
                                    XMLCryptoContext context) throws KeySelectorException {
        if (keyInfo == null) {
            throw new KeySelectorException("ERROR: KeyInfo object null!");
        }

        //TODO: usando sugerencia de intellij se remplaza por enhaced for
        for (Object o : keyInfo.getContent()) {
            XMLStructure hX509Data = (XMLStructure) o;
            if (!(hX509Data instanceof X509Data)) {
                continue;
            }
            X509Data x509Data = (X509Data) hX509Data;
            //TODO: usando sugerencia de intellij se remplaza por enhaced for
            for (Object oX509Certificate : x509Data.getContent()) {
                if (!(oX509Certificate instanceof X509Certificate)) {
                    continue;
                }
                final X509Certificate x509Certificate = ((X509Certificate) oX509Certificate);
                final PublicKey key = x509Certificate.getPublicKey();
                // System.out.println(x509Certificate.getSubjectDN());
                if (algEquals(method.getAlgorithm(), key.getAlgorithm())) {
                    return new MyKeySelectorResult() {
                        public Key getKey() {
                            return key;
                        }

                        public X509Certificate getCertificate() {
                            return x509Certificate;
                        }
                    };
                }
            }
        }
        throw new KeySelectorException("ERROR: No X509Certificate found!");
    }
}
