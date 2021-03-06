package de.dfki.lt.mdparser.outputformat;

public class XMLString {

  private String xmlString;


  public XMLString(String string) {
    this.xmlString = string.replaceAll("&", "&amp;");
    this.xmlString = this.xmlString.replaceAll("\"", "&quot;");
    //this.xmlString = xmlString.replaceAll("'", "&apos;");
    this.xmlString = this.xmlString.replaceAll("<", "&lt;");
    this.xmlString = this.xmlString.replaceAll(">", "&gt;");
    this.xmlString = this.xmlString.replaceAll("Ä", "&#196;");
    this.xmlString = this.xmlString.replaceAll("Ö", "&#214;");
    this.xmlString = this.xmlString.replaceAll("Ü", "&#220;");
    this.xmlString = this.xmlString.replaceAll("ä", "&#228;");
    this.xmlString = this.xmlString.replaceAll("ö", "&#246;");
    this.xmlString = this.xmlString.replaceAll("ü", "&#252;");
    this.xmlString = this.xmlString.replaceAll("ß", "&#223;");
  }


  public static String toNormalString(String xmlString) {

    String string = xmlString.replaceAll("&amp;", "&");
    string = string.replaceAll("&quot;", "\"");
    string = string.replaceAll("&apos;", "'");
    string = string.replaceAll("&lt;", "<");
    string = string.replaceAll("&gt;", ">");
    string = string.replaceAll("&#196;", "Ä");
    string = string.replaceAll("&#214;", "Ö");
    string = string.replaceAll("&#220;", "Ü");
    string = string.replaceAll("&#228;", "ä");
    string = string.replaceAll("&#246;", "ö");
    string = string.replaceAll("&#252;", "ü");
    string = string.replaceAll("&#223;", "ß");
    return string;
  }


  public String getXmlString() {

    return this.xmlString;
  }
}
