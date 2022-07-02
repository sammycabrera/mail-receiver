/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.util;

/**
 * @author scabrera
 */
public final class Constants {

    private Constants() {
    }

    public static final String DOWNLOAD_FOLDER = "data";

    public static final String DOWNLOADED_MAIL_FOLDER = "DOWNLOADED";

    public static final String REJECTED_MAIL_FOLDER = "REJECTED";

    public static final String XML_CONTENT = "XMLContent";

    public static final String XML_PART = "XMLPart";

    public static final String PDF_PART = "PDFPart";

    public static final String XML_FILE = "XMLFile";

    public static final String EXTENSION_ZIP = "zip";

    public static final String SPLIT_CHAR = ",";

    public static final String SPLIT_CHAR_SUBJECT = ";";

    public static final String RESPONSE_CODE_OK = "02";
    
    public static final String RESPONSE_DESC_OK = "Documento validado por la DIAN";    

    public static final String VALIDATOR_NAME_FILE = "ValidateMessage";
    
    public static final String VALIDATOR_EXTENSION_FILE = ".txt";
    
    public static final String VALIDATOR_FORMATDATE_FILE = "MMddyyyyHHmmss";

    public static final String RG_EXTRACT_NS = "xmlns:[^=]+=\"[^\"]+\"";
    
    public static final String CHAR_BEFORE_NS = "=";
    
    public static final String ALG_DSA = "DSA";
    
    public static final String ALG_RSA = "RSA";
    
    public static final String URI_RSA= "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    
    public static final String FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    
    public static final String RECEIVER_PARTY_NODE = "cac:ReceiverParty";
    
    public static final String COMPANYID_ELEMENT = "//cbc:CompanyID";
    
    public static final String SENDER_PARTY_NODE = "cac:SenderParty";
    
    public static final String RESPONSE_NODE = "cac:Response";
    
    public static final String RESPONSE_CODE_ELEMENT = "//cbc:ResponseCode";
    
    public static final String RESPONSE_DESC_ELEMENT = "//cbc:Description";
    
    public static final String SIGNATURE_NODE = "ds:Signature";    
    
    public static final String UBLEXTENSIONS_NODE = "ext:UBLExtensions";
    
    public static final String SIGNATURE_ELEMENT = "Signature";
    
    public static final String REFERENCE_ELEMENT = "Reference";
    
    
    
    
}