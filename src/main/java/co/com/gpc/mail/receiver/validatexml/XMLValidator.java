/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class XMLValidator {

    public static final String XML_FILE = "invoice2.xml";
    public static String SCHEMA_FILE = "UBL-Invoice-2.1.xsd";

    public static void main(String[] args) throws DocumentException {
        XMLValidator XMLValidator = new XMLValidator();
        SCHEMA_FILE = "DIAN_UBL_Structures.xsd,    UBL-ApplicationResponse-2.1.xsd,    UBL-AttachedDocument-2.1.xsd,    UBL-CreditNote-2.1.xsd,    UBL-DebitNote-2.1.xsd,    UBL-Invoice-2.1.xsd,        CCTS_CCT_SchemaModule-2.1.xsd,    UBL-CommonAggregateComponents-2.1.xsd,    UBL-CommonBasicComponents-2.1.xsd,    UBL-CommonExtensionComponents-2.1.xsd,    UBL-CommonSignatureComponents-2.1.xsd,    UBL-CoreComponentParameters-2.1.xsd,    UBL-ExtensionContentDataType-2.1.xsd,    UBL-QualifiedDataTypes-2.1.xsd,    UBL-SignatureAggregateComponents-2.1.xsd,    UBL-SignatureBasicComponents-2.1.xsd,    UBL-UnqualifiedDataTypes-2.1.xsd,    UBL-XAdESv132-2.1.xsd,    UBL-XAdESv141-2.1.xsd,    UBL-xmldsig-core-schema-2.1.xsd";
        boolean valid = XMLValidator.validate(XML_FILE, SCHEMA_FILE);

        System.out.printf("%s validation = %b.", XML_FILE, valid);
    }

    public boolean validate(String xmlFile, String schemaFile) throws DocumentException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {

            List<String> xsdList = new ArrayList<>(Arrays.asList(schemaFile.split(",")));
            Source[] sourceList = new Source[xsdList.size()];

            for (int i = 0; i < xsdList.size(); i++) {
                sourceList[i] = new StreamSource(getClass().getClassLoader().getResource(xsdList.get(i).trim()).toExternalForm());
            }
            Schema schema = schemaFactory.newSchema(sourceList);

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(getResource(xmlFile))));
            SAXReader sax = new SAXReader();// Crea un objeto SAXReader
            Document document = sax.read(new File(getResource(xmlFile)));// Obtenga el objeto del documento, si el documento no tiene nodos, se lanzará una excepción para finalizar antes
            System.out.println(document.asXML());
            String dataResponse =  extractSubXML(document.asXML(), "cac:Response") ;
            System.out.println("RESPONSE: "+dataResponse);
            if(dataResponse.length() > 0){
                Document documentResponse = DocumentHelper.parseText(dataResponse);
                System.out.println(documentResponse.asXML());
                Element rootResponse = documentResponse.getRootElement();
                Node nodeResponse = rootResponse.selectSingleNode("//cbc:ResponseCode");
                String responseCode = (nodeResponse == null ? "" : nodeResponse.getText()); 
                System.out.println(responseCode);
            }
            
            return true;
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getResource(String filename) throws FileNotFoundException {
        URL resource = getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
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
         //String nsClean= m.group().replace("e>", "");
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
            //String nsClean= nameSpacesXml.replace("e>", "");
            String subXml = "<Documento " +nameSpacesXml+ "> "+fileXml.substring(beginPos+tagName.length(), endPos)+" </Documento>";
            return (subXml == null ? "" : subXml);               
        }   
        return "";
   }     
}
