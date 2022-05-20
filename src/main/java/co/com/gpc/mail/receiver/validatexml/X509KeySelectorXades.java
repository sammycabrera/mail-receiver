/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author scabrera
 */
public class X509KeySelectorXades {

    public static MyKeySelectorResult getX509FromXadesFile(Document doc, String algorithmeMethod) throws Exception {
        // Find Signature element
        NodeList nl = doc.getElementsByTagName("ds:Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }


        /*
         * ---------------
         */
        DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), nl.item(0));
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        fac.newDigestMethod(DigestMethod.SHA256, null);
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        KeyInfo keyInfo = signature.getKeyInfo();

        return select(keyInfo, algorithmeMethod);
    }

    public static MyKeySelectorResult getX509FromXadesFile(InputStream fileInput, String algorithmeMethod) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(fileInput);

        return getX509FromXadesFile(doc, algorithmeMethod);
    }

    public static MyKeySelectorResult select(KeyInfo keyInfo, String method) throws KeySelectorException {
        if (keyInfo == null) {
            throw new KeySelectorException("ERROR: KeyInfo object null!");
        }
        for (Object o : keyInfo.getContent()) {
            XMLStructure hX509Data = (XMLStructure) o;
            if (!(hX509Data instanceof X509Data))
                continue;
            X509Data x509Data = (X509Data) hX509Data;
            for (Object oX509Certificate : x509Data.getContent()) {
                if (!(oX509Certificate instanceof X509Certificate))
                    continue;
                final X509Certificate x509Certificate = ((X509Certificate) oX509Certificate);
                final PublicKey key = x509Certificate.getPublicKey();
                if (algEquals(method, key.getAlgorithm())) {
                    return new MyKeySelectorResult(key, x509Certificate);
                }
            }
        }
        throw new KeySelectorException("ERROR: No X509Certificate found!");
    }

    static boolean algEquals(String algURI, String algName) {
        return (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
                || (algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase("RSA"));
    }
}
