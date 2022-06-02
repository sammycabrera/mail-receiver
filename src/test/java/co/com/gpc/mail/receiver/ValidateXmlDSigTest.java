/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver;

import co.com.gpc.mail.receiver.validatexml.XMLValDSign;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 *
 * @author scabrera
 */
public class ValidateXmlDSigTest {

    public static void main(String[] args) throws Exception {

        ValidateXmlDSigTest v = new ValidateXmlDSigTest();
        String XML_FILE = "invoice2.xml";

        XMLValDSign xml = new XMLValDSign();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document docu = dbf.newDocumentBuilder().parse(new FileInputStream(v.getResource(XML_FILE)));
        boolean resp = xml.validateXmlDSig(docu);
        System.out.println("CONTENIDO: " + resp);

    }

    private String getResource(String filename) throws FileNotFoundException {
        URL resource = getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }
}
