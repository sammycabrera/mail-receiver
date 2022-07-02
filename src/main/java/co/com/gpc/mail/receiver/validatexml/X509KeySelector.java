/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import static co.com.gpc.mail.receiver.util.Constants.*;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.keyinfo.*;

/**
 * @author scabrera
 */
public class X509KeySelector extends KeySelector {
    

    static boolean algEqualss(String algURI, String algName) {

        return (algName.equalsIgnoreCase(ALG_DSA) && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
                || (algName.equalsIgnoreCase(ALG_RSA) && algURI.equalsIgnoreCase(ALG_RSA));
    }

    static boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase(ALG_DSA)
                && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return true;
        } else if (algName.equalsIgnoreCase(ALG_RSA)
                && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return true;
        }
        else return algName.equalsIgnoreCase(ALG_RSA)
                && algURI.equalsIgnoreCase(URI_RSA);
    }

    public KeySelectorResult select(KeyInfo keyInfo,
                                    KeySelector.Purpose purpose,
                                    AlgorithmMethod method,
                                    XMLCryptoContext context) throws KeySelectorException {
        if (keyInfo == null) {
            throw new KeySelectorException("ERROR: KeyInfo object null!");
        }

        for (Object o : keyInfo.getContent()) {
            XMLStructure hX509Data = (XMLStructure) o;
            if (!(hX509Data instanceof X509Data)) {
                continue;
            }
            X509Data x509Data = (X509Data) hX509Data;
            for (Object oX509Certificate : x509Data.getContent()) {
                if (!(oX509Certificate instanceof X509Certificate)) {
                    continue;
                }
                final X509Certificate x509Certificate = ((X509Certificate) oX509Certificate);
                final PublicKey key = x509Certificate.getPublicKey();

                if (algEquals(method.getAlgorithm(), key.getAlgorithm())) {
                    return new MyKeySelectorResult() {
                        @Override
                        public Key getKey() {
                            return key;
                        }

                        @Override
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
