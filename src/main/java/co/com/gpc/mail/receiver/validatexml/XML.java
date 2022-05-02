/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;
 
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.security.Key;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
 import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.production.DataObjectReference;
import xades4j.production.SignedDataObjects;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.production.XadesTSigningProfile;
import xades4j.properties.DataObjectDesc;
import xades4j.providers.CertificateValidationProvider;
import xades4j.providers.KeyingDataProvider;
import xades4j.providers.impl.AuthenticatedTimeStampTokenProvider;
import xades4j.providers.impl.DefaultMessageDigestProvider;
import xades4j.providers.impl.FileSystemKeyStoreKeyingDataProvider;
import xades4j.providers.impl.PKIXCertificateValidationProvider;
import xades4j.providers.impl.TSAHttpAuthenticationData;
import xades4j.utils.DOMHelper;
import xades4j.utils.FileSystemDirectoryCertStore;
import xades4j.verification.XadesVerificationProfile;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tw.com.softleader.vxmldsig.Verify;
import xades4j.verification.XAdESVerificationResult;
import xades4j.verification.XadesVerificationProfile;
 
 
/**
 * XML utils
 */
public class XML {
    
    private static final Logger Logger = LoggerFactory.getLogger(XML.class);
 
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return dbf;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
 
    public static DocumentBuilder newDocumentBuilder() {
        try {
            return newDocumentBuilderFactory().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
 
    /**
     * Serialize to XML String
     * 
     * @param document
     *            The DOM document
     * @return The XML String
     */
    public static String serialize(Document document) {
        StringWriter writer = new StringWriter();
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(writer);
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            throw new RuntimeException("Error when serializing XML document.", e);
        }
        return writer.toString();
    }
 
    /**
     * Parse an XML file to DOM
     * 
     * @param file
     *            The XML file
     * @return null if an error occurs during parsing.
     *
     */
    public static Document getDocument(File file) {
        try {
            return newDocumentBuilder().parse(file);
        } catch (SAXException e) {
            Logger.warn("Parsing error when building Document object from xml file '" + file + "'.", e);
        } catch (IOException e) {
            Logger.warn("Reading error when building Document object from xml file '" + file + "'.", e);
        }
        return null;
    }
 
    /**
     * Parse an XML string content to DOM
     * 
     * @param xml
     *            The XML string
     * @return null if an error occurs during parsing.
     */
    public static Document getDocument(String xml) {
        InputSource source = new InputSource(new StringReader(xml));
        try {
            return newDocumentBuilder().parse(source);
        } catch (SAXException e) {
            Logger.warn("Parsing error when building Document object from xml data.", e);
        } catch (IOException e) {
            Logger.warn("Reading error when building Document object from xml data.", e);
        }
        return null;
    }
 
    /**
     * Parse an XML coming from an input stream to DOM
     * 
     * @param stream
     *            The XML stream
     * @return null if an error occurs during parsing.
     */
    public static Document getDocument(InputStream stream) {
        try {
            return newDocumentBuilder().parse(stream);
        } catch (SAXException e) {
            Logger.warn("Parsing error when building Document object from xml data.", e);
        } catch (IOException e) {
            Logger.warn("Reading error when building Document object from xml data.", e);
        }
        return null;
    }
 
    /**
     * Check the xmldsig signature of the XML document.
     * 
     * @param document
     *            the document to test
     * @param publicKey
     *            the public key corresponding to the key pair the document was signed with
     * @return true if a correct signature is present, false otherwise
     */
    public static boolean validSignature(Document document, Key publicKey) {
        Node signatureNode = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        KeySelector keySelector = KeySelector.singletonKeySelector(publicKey);
 
        try {
            String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
            DOMValidateContext valContext = new DOMValidateContext(keySelector, signatureNode);
 
            XMLSignature signature = fac.unmarshalXMLSignature(valContext);
            return signature.validate(valContext);
        } catch (Exception e) {
            Logger.warn("Error validating an XML signature.", e);
            return false;
        }
    }
 
    /**
     * Sign the XML document using xmldsig.
     * 
     * @param document
     *            the document to sign; it will be modified by the method.
     * @param publicKey
     *            the public key from the key pair to sign the document.
     * @param privateKey
     *            the private key from the key pair to sign the document.
     * @return the signed document for chaining.
     */
    public static Document sign(Document document, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();
 
        try {
            Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
            DOMSignContext dsc = new DOMSignContext(privateKey, document.getDocumentElement());
            KeyValue keyValue = keyInfoFactory.newKeyValue(publicKey);
            KeyInfo ki = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);
        } catch (Exception e) {
            Logger.warn("Error while signing an XML document.", e);
        }
 
        return document;
    }
 
    
      private String getResource(String filename) throws FileNotFoundException {
        URL resource = getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }
      
    public boolean validateXmlDSig(String signed){
        
    try {
        String initialString = "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";
    
        //String data = "-----BEGIN CERTIFICATE-----MIIDbTCCAlWgAwIBAgIEHUj86jANBgkqhkiG9w0BAQsFADBnMQswCQYDVQQGEwJrcjEQMA4GA1UECBMHa3Jpc2huYTEQMA4GA1UEBxMHa3Jpc2huYTEQMA4GA1UEChMHa3Jpc2huYTEQMA4GA1UECxMHa3Jpc2huYTEQMA4GA1UEAxMHa3Jpc2huYTAeFw0xODAyMDkwNDEwMjJaFw0xOTAyMDkwNDEwMjJaMGcxCzAJBgNVBAYTAmtyMRAwDgYDVQQIEwdrcmlzaG5hMRAwDgYDVQQHEwdrcmlzaG5hMRAwDgYDVQQKEwdrcmlzaG5hMRAwDgYDVQQLEwdrcmlzaG5hMRAwDgYDVQQDEwdrcmlzaG5hMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqoDP8q/LerAIb6WBx0nx3i670p7uwRbTQ/NeK54BEsV3pkjT8GlPsTP2ai6Xs038oYkgSxldlfhgpdXsWqh95+YdqDPJV2iF4LAET2Z9qrUChp6MkmHQr8HRi1+2UOS6WI0IX4fZasXapL1cQCsGTYem1b4kJmYPXczhtOMKwJXbdA0YvzE+4m2pX1wjMufi2eX8a4quojQjJrURnq74/XJepNnA0yWRuczdLG3XpPFftQaCiAj+Oa/3bKUYXRg1ODdl5VTaxUkqL/2odw4vvFs2fhSnouALsc+wyAKaBH38ZICUCoSiX6mU9hCvBpbHFk6ToQcPTo3CkWGSzBiPrQIDAQABoyEwHzAdBgNVHQ4EFgQUT1S1ED2sUYPCi8Thmz/tEqiSLIIwDQYJKoZIhvcNAQELBQADggEBAKabXk+LifQMnA8eJvDkn8xZ3FKr/9osmIcJjkO4i0vtnGOSxQ4+IyATxJ/4nrXAZwBI4+q4l13GNFw+S6ebKoYfNWEvZHUbjLALr98+nhHsURY73TIV2nw75bWOvh3QRpDDPiP/3Fzs9XjENxeUXcUV+mLGETKGa6szfcZ0Huaeva5nxDt7U+4/3xyMO9CWuYglhC8act1pd3RfZdZJaDN3fUy/+tocKXMmo0s7oLiAlyfLNhng2aqHwHu/sAoHjYyLhqz7401MmqHYX8hkOJ6FBXjJZz62zfrFUVoM10zNkByIbdL4WzNf/d8z0X57IgHg08IPpYJOfenSIqF3Rmo=-----END CERTIFICATE-----";
       // X509Certificate cert = generateX509Certificate(data);
        
        //InputStream targetStream = new ByteArrayInputStream(data.getBytes());
    
        //CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        //X509Certificate cert = (X509Certificate) certFactory.generateCertificate(targetStream);
        
        X509Certificate cert = CertificateUtil.getX509Certificate(initialString);
        
        DocumentBuilderFactory dbf = 
                  DocumentBuilderFactory.newInstance(); 
        dbf.setNamespaceAware(true);

        DocumentBuilder builder = dbf.newDocumentBuilder();  
        Document doc1 = builder.parse(new ByteArrayInputStream(signed.getBytes("utf-8")));
      
    String XML_FILE = "invoice2.xml";
        org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(new File(getResource(XML_FILE))));
        
        //printDocument(doc, System.out);
        NodeList signatureNodeList = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        //NodeList bodyNodeList = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        
        
        Node n=null;
Element eElement=null;

for (int i = 0; i < signatureNodeList.getLength(); i++) {           
  System.out.println(signatureNodeList.getLength());     
  n= signatureNodeList.item(i);                            
  System.out.println("\nCurrent Element :" + n.getNodeName());


  if (n.getNodeType() == Node.ELEMENT_NODE) {
    eElement = (Element) n.getChildNodes();
    System.out.println("\nCurrent Element: " + n.getNodeName());
   // name = eElement.getElementsByTagName("name").item(i).getTextContent(); //here throws null pointer exception after printing staff1 tag
      if (signatureNodeList.getLength() == 0) {
          throw new Exception("Cannot find Signature element");
        }else{
            nodeToString(n);
        }
        CertificateValidationProvider provider = new PKIXCertificateValidationProvider(
                ks, false, cert.);
      XadesVerificationProfile profile = new XadesVerificationProfile(provider);
      // Element sigElem = (Element) nl.item(0);
        XAdESVerificationResult r = profile.newVerifier().verify(eElement, null);
      
      DOMValidateContext valContext = new DOMValidateContext(cert.getPublicKey(), n);
        
        
        //valContext.setIdAttributeNS((Element)bodyNodeList.item(0),"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd","Id");
        

        
        XMLSignatureFactory factory = 
                  XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = 
                  factory.unmarshalXMLSignature(valContext);
        boolean coreValidity = signature.validate(valContext);
        System.out.println("Signature passed core validation "+coreValidity);
  }
  n.getNextSibling();
}

//
//
//        if (signatureNodeList.getLength() == 0) {
//          throw new Exception("Cannot find Signature element");
//        }else{
//            System.out.println(doc.toString());
//            nodeToString(signatureNodeList.item(0));
//        }
//        DOMValidateContext valContext = new DOMValidateContext(cert.getPublicKey(), signatureNodeList.item(0));
//        //valContext.setIdAttributeNS((Element)bodyNodeList.item(0),"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd","Id");
//        
//
//        
//        XMLSignatureFactory factory = 
//                  XMLSignatureFactory.getInstance("DOM");
//        XMLSignature signature = 
//                  factory.unmarshalXMLSignature(valContext);
//        boolean coreValidity = signature.validate(valContext); 
//        
//         
//        //detailed validation - use when solving validity problems
////        boolean sv = signature.getSignatureValue().validate(valContext);
////        Iterator<Reference> i = signature.getSignedInfo().getReferences().iterator();
////        for (int j=0; i.hasNext(); j++) {
////          boolean refValid = ( i.next()).validate(valContext);
////        } 
//        
//        //    XMLSignature signature = fac.unmarshalXMLSignature(valContext);
//
//    
//
//    // Check core validation status
////    if (coreValidity == false) {
////      System.err.println("Signature failed core validation");
////      boolean sv = signature.getSignatureValue().validate(valContext);
////      System.out.println("signature validation status: " + sv);
////      // check the validation status of each Reference
////      Iterator<?> i = signature.getSignedInfo().getReferences().iterator();
////      for (int j = 0; i.hasNext(); j++) {
////        boolean refValid = ((Reference) i.next()).validate(valContext);
////        System.out.println("ref[" + j + "] validity status: " + refValid);
////      }
////    } else {
////      System.out.println("Signature passed core validation");
////    }
//        
//        return coreValidity;
    }
    catch (Exception e){
        throw new IllegalArgumentException("validation failes", e);
    }
    return false;
}
    
private String nodeToString(Node node) {
    System.out.println("nodeToString");
    StringWriter sw = new StringWriter();
    try {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
    } catch (TransformerException te) {
        System.out.println("nodeToString Transformer Exception");
    }
    return sw.toString();
}
public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    transformer.transform(new DOMSource(doc), 
         new StreamResult(new OutputStreamWriter(out, "UTF-8")));
}    
    
    
public static X509Certificate generateX509Certificate(String certEntry) throws IOException {
 
        InputStream in = null;
        X509Certificate cert = null;
        try {
            byte[] certEntryBytes = certEntry.getBytes();
            in = new ByteArrayInputStream(certEntryBytes);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
 
            cert = (X509Certificate) certFactory.generateCertificate(in);
        } catch (CertificateException ex) {
 
        } finally {
            if (in != null) {
                    in.close();
            }
        }
        return cert;
    }
    
    
}
