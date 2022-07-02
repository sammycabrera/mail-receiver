/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.parserxml;

import static co.com.gpc.mail.receiver.validatexml.XMLValDSign.extractNamespace;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;

/**
 *
 * @author scabrera
 */
@Slf4j
public class XMLUtil {

    private XMLUtil() {
        throw new IllegalStateException("XMLUtil class");
    }

    public static String extractSubXML(String fileXml, String tagName)
            throws DocumentException {
        String nameSpacesXml = extractNamespace(fileXml);
        log.debug(nameSpacesXml);
        if (fileXml.contains("<" + tagName)) {
            int beginPos = fileXml.indexOf("<" + tagName);
            int endPos = fileXml.indexOf("</" + tagName + ">");
            StringBuilder documentXML = new StringBuilder();            
            return documentXML.append("<Documento ").append(nameSpacesXml).append("> ").append(fileXml.substring(beginPos + tagName.length(), endPos)).append(" </Documento>").toString();
        }
        return "";
    }
    
    //method to convert Document to String
    public static String getStringFromDocument(Document doc)
    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           return writer.toString();
        }
        catch(TransformerException ex)
        {
            log.error("cannot convert Document W3C to String ", ex);
            return null;
        }
    }     
    
    public static Document convertStringToDocument(String dataSignatureExtension) {
        try(ByteArrayInputStream inputStream =new ByteArrayInputStream(dataSignatureExtension.getBytes(StandardCharsets.UTF_8))){
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().parse(inputStream);
        }catch(Exception ex){
            log.error("cannot convert Strin to Document W3C ", ex);
            return null;        
        }
    }     
}
