/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import static co.com.gpc.mail.receiver.parserxml.XMLUtil.*;
import static co.com.gpc.mail.receiver.util.Constants.CHAR_BEFORE_NS;

import co.com.gpc.mail.receiver.exception.ErrorCodes;
import co.com.gpc.mail.receiver.exception.SignatureNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import static co.com.gpc.mail.receiver.util.Constants.*;

/**
 * XML utils
 */
@Slf4j
public class XMLValDSign {

    private XMLValDSign() {
        throw new IllegalStateException("XMLValDSign class");
    }


    public static boolean validateXmlDSig(Document doc) throws XMLSignatureException {
        try {
            String dataSignatureExtension = extractSubXML(getStringFromDocument(doc), UBLEXTENSIONS_NODE);
            Document w3cDoc = convertStringToDocument(dataSignatureExtension);

            NodeList signatureNodeList = w3cDoc.getElementsByTagNameNS(XMLSignature.XMLNS, SIGNATURE_ELEMENT);
            NodeList referenceNodeList = w3cDoc.getElementsByTagNameNS(XMLSignature.XMLNS, REFERENCE_ELEMENT);

            IntStream
                    .range(0, referenceNodeList.getLength())
                    .mapToObj(i -> (Element) referenceNodeList.item(i))
                    .forEach(value -> value.setAttribute("URI", ""));

            if (signatureNodeList.getLength() == 0) {
                log.error("Cannot find Signature element");
                throw new SignatureNotFoundException("Cannot find Signature element", ErrorCodes.SIGNATURE_NOTFOUND_ERROR);
            }

            Key validationKey = null;
            X509Certificate cert = null;
            MyKeySelectorResult ksResult;
            try {
                ksResult = X509KeySelectorXades.getX509FromXadesFile(w3cDoc, ALG_RSA);
                validationKey = ksResult.getKey();
                cert = ksResult.getCertificate();
                if (validationKey == null) {
                    log.error("the keyselector did not find a validation key");
                    throw new XMLSignatureException("the keyselector did not find a validation key");
                }
            } catch (KeySelectorException kse) {
                log.error("cannot find validation key ", kse);
                throw new XMLSignatureException("cannot find validation key", kse);
            }

            DOMValidateContext valContext = new DOMValidateContext(cert.getPublicKey(), signatureNodeList.item(0));
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = factory.unmarshalXMLSignature(valContext);

            return !signature.getSignedInfo().getReferences().isEmpty();
        } catch (Exception e) {
            log.error("cannot complete validation ", e);
            throw new XMLSignatureException("cannot complete validation ", e);
        }

    }

    public static boolean nsRegister(String ns, List<String> list) {
        if (list != null) {
            return list.contains(ns);
        }
        return false;
    }

    public static String extractNamespace(String xmlFile) {
        Pattern p = Pattern.compile(RG_EXTRACT_NS);
        Matcher m = p.matcher(xmlFile);
        StringBuilder data = new StringBuilder();
        ArrayList<String> list = new ArrayList<>();
        while (m.find()) {
            
            if (!nsRegister(m.group().substring(0, m.group().indexOf(CHAR_BEFORE_NS)), list)) {
                data.append(m.group());
                data.append(" ");
                list.add(m.group().substring(0, m.group().indexOf(CHAR_BEFORE_NS)));
            }

        }
        return data.toString();
    }
   
    
}
