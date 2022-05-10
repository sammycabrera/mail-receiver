/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.validatexml;

import java.security.Key;
import java.security.cert.X509Certificate;
import javax.xml.crypto.KeySelectorResult;

/**
 *
 * @author scabrera
 */
public class MyKeySelectorResult implements KeySelectorResult{
private Key key;
private X509Certificate certificate;
public MyKeySelectorResult(Key key, X509Certificate certificate) {
    super();
    this.key = key;
    this.certificate = certificate;
}

public MyKeySelectorResult() {
    super();
}


public void setKey(Key key) {
    this.key = key;
}

public Key getKey() {
    return key;
}

public void setCertificate(X509Certificate certificate) {
    this.certificate = certificate;
}

public X509Certificate getCertificate() {
    return certificate;
}
}
