/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XML utils
 */
public class XMLValDSign {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLValDSign.class);

    public static boolean validateXmlDSig(Document doc) {
        try {

            NodeList signatureNodeList = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

            NodeList referenceNodeList = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Reference");

            IntStream
                    .range(0, referenceNodeList.getLength())
                    .mapToObj(i -> (Element) referenceNodeList.item(i))
                    .forEach(value -> value.setAttribute("URI", ""));
            
            if (signatureNodeList.getLength() == 0) {
                throw new Exception("Cannot find Signature element");
            } 
            
            Key validationKey = null;
                X509Certificate cert = null;
                MyKeySelectorResult ksResult;
                try {
                    ksResult = X509KeySelectorXades.getX509FromXadesFile(doc, "RSA");
                    validationKey = ksResult.getKey();
                    cert = ksResult.getCertificate();
                    if (validationKey == null) {
                        throw new XMLSignatureException("the keyselector did " + "not find a validation key");
                    }
                } catch (KeySelectorException kse) {
                    throw new XMLSignatureException("cannot find validation " + "key", kse);
                }
            
            
            DOMValidateContext valContext = new DOMValidateContext(cert.getPublicKey(), signatureNodeList.item(0));
            
            XMLSignatureFactory factory
                    = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature
                    = factory.unmarshalXMLSignature(valContext);
            boolean coreValidity = false;


                Iterator<?> i = signature.getSignedInfo().getReferences().iterator();
                for (int j = 0; i.hasNext(); j++) {
                    boolean refValid = ((Reference) i.next()).validate(valContext);
                    if(refValid){
                        coreValidity= true;
                    }
                }
           
            return coreValidity;
        } catch (Exception e) {
           return false;
        }
        
    }
    
    
    
    

 public static boolean nsRegister(String ns, ArrayList<String> list){
     if(list!=null){
         if(list.contains(ns)){
             return true;
         }
     }
     return false;
 }  
    
 public static String extractNamespace(String xmlFile){
     Pattern p = Pattern.compile("xmlns:[^=]+=\"[^\"]+\"");
     Matcher m = p.matcher(xmlFile);
     StringBuilder data = new StringBuilder();
     ArrayList<String> list = new ArrayList<>();
     while (m.find()) {
         if(!nsRegister(m.group(), list)){
            data.append(m.group());
            data.append(" "); 
            list.add(m.group());
         }

     }
    return data.toString();
 }   
    
public static String extractSubXML(String fileXml, String tagName) 
           throws DocumentException{
        String nameSpacesXml = extractNamespace(fileXml);
        System.out.println(nameSpacesXml);
        if(fileXml.contains("<"+tagName)){
            int beginPos = fileXml.indexOf("<"+tagName);
            int endPos = fileXml.indexOf("</"+tagName+">");
            String subXml = "<Documento " +nameSpacesXml+ "> "+fileXml.substring(beginPos+tagName.length(), endPos)+" </Documento>";
            return (subXml == null ? "" : subXml);               
        }   
        return "";
   }      
    


}
