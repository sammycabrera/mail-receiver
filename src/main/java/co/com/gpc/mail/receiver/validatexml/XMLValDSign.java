/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static co.com.gpc.mail.receiver.util.Constants.RG_EXTRACT_NS;

/**
 * XML utils
 */
@Slf4j
public class XMLValDSign {

    public static boolean validateXmlDSig(Document doc) throws XMLSignatureException {
        try {
            //TODO: si ya esta importado no es necesario poner todo el paquete en la declaracion
            DOMReader reader = new DOMReader();
            org.dom4j.Document document = reader.read(doc);
            String dataSignatureExtension = extractSubXML(document.asXML(), "ext:UBLExtensions");
            org.dom4j.Document dom4jDoc = DocumentHelper.parseText(dataSignatureExtension);
            org.w3c.dom.Document w3cDoc = new DOMWriter().write(dom4jDoc);

            NodeList signatureNodeList = w3cDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            NodeList referenceNodeList = w3cDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Reference");

            IntStream
                    .range(0, referenceNodeList.getLength())
                    .mapToObj(i -> (Element) referenceNodeList.item(i))
                    .forEach(value -> value.setAttribute("URI", ""));

            if (signatureNodeList.getLength() == 0) {
                log.error("Cannot find Signature element");
                throw new Exception("Cannot find Signature element");
            }

            Key validationKey = null;
            X509Certificate cert = null;
            MyKeySelectorResult ksResult;
            try {
                ksResult = X509KeySelectorXades.getX509FromXadesFile(w3cDoc, "RSA");
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

            return signature.getSignedInfo().getReferences().size() > 0;
        } catch (Exception e) {
            log.error("cannot complete validation ", e);
            throw new XMLSignatureException("cannot complete validation ", e);
        }

    }

    public static boolean nsRegister(String ns, ArrayList<String> list) {
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
            if (!nsRegister(m.group(), list)) {
                data.append(m.group());
                data.append(" ");
                list.add(m.group());
            }

        }
        return data.toString();
    }

    public static String extractSubXML(String fileXml, String tagName)
            throws DocumentException {
        String nameSpacesXml = extractNamespace(fileXml);
        log.debug(nameSpacesXml);
        if (fileXml.contains("<" + tagName)) {
            int beginPos = fileXml.indexOf("<" + tagName);
            int endPos = fileXml.indexOf("</" + tagName + ">");
            return "<Documento " + nameSpacesXml + "> " + fileXml.substring(beginPos + tagName.length(), endPos) + " </Documento>";
        }
        return "";
    }
}
