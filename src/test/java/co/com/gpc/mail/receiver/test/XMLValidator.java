/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.test;

import static co.com.gpc.mail.receiver.parserxml.XMLUtil.*;
import co.com.gpc.mail.receiver.validatexml.XMLValDSign;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
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

    public static void main(String[] args) throws DocumentException, Exception {
        XMLValidator XMLValidator = new XMLValidator();
        SCHEMA_FILE = "DIAN_UBL_Structures.xsd,    UBL-ApplicationResponse-2.1.xsd,    UBL-AttachedDocument-2.1.xsd,    UBL-CreditNote-2.1.xsd,    UBL-DebitNote-2.1.xsd,    UBL-Invoice-2.1.xsd,        CCTS_CCT_SchemaModule-2.1.xsd,    UBL-CommonAggregateComponents-2.1.xsd,    UBL-CommonBasicComponents-2.1.xsd,    UBL-CommonExtensionComponents-2.1.xsd,    UBL-CommonSignatureComponents-2.1.xsd,    UBL-CoreComponentParameters-2.1.xsd,    UBL-ExtensionContentDataType-2.1.xsd,    UBL-QualifiedDataTypes-2.1.xsd,    UBL-SignatureAggregateComponents-2.1.xsd,    UBL-SignatureBasicComponents-2.1.xsd,    UBL-UnqualifiedDataTypes-2.1.xsd,    UBL-XAdESv132-2.1.xsd,    UBL-XAdESv141-2.1.xsd,    UBL-xmldsig-core-schema-2.1.xsd";
        boolean valid = XMLValidator.validate(XML_FILE, SCHEMA_FILE);

        System.out.printf("%s validation = %b.", XML_FILE, valid);
    }

    public boolean validate(String xmlFile, String schemaFile) throws DocumentException, Exception {
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
            Document document = sax.read(new File(getResource(xmlFile)));// Obtenga el objeto del documento, si el documento no tiene nodos, se lanzar?? una excepci??n para finalizar antes
            System.out.println(document.asXML());
            String dataResponse = extractSubXML(document.asXML(), "cac:Response");
            System.out.println("RESPONSE: " + dataResponse);
            if (dataResponse.length() > 0) {
                Document documentResponse = DocumentHelper.parseText(dataResponse);
                System.out.println(documentResponse.asXML());
                Element rootResponse = documentResponse.getRootElement();
                Node nodeResponse = rootResponse.selectSingleNode("//cbc:ResponseCode");
                String responseCode = (nodeResponse == null ? "" : nodeResponse.getText());
                System.out.println(responseCode);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                org.w3c.dom.Document docu = dbf.newDocumentBuilder().parse(new FileInputStream(getResource(XML_FILE)));
                boolean resp = XMLValDSign.validateXmlDSig(docu);
                System.out.println("CONTENIDO: " + resp);

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

}
