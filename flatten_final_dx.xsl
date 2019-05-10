<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 
  Converts quotation marks to &#182; (paragraph mark).
  Pipe through sed to get quotation marks escaped for
  Excel: sed 's/\xB6/""/g'.
-->

<!--
  Adds plus sign (+) to beginning of every row. Use
  grep to remove lines that don't start with + from
  xsltproc output (xsltproc is confused by multi-line
  elements?): grep -e "^+".
-->

<xsl:output method="text" encoding="iso-8859-1"/>

<xsl:strip-space elements="*"/>

<xsl:template match="/">
  <xsl:text>+Pt-SourceNumber, Pt-SourceMrn, Pt-EMPI, Case-MRN, Case-AccessionNumber, Case-FinalText&#xa;</xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="/CoPathDump/Patient/Case">
  <xsl:for-each select=".">
    <xsl:text>+</xsl:text><xsl:value-of select="../@sourceRecord"/>
    <xsl:text>,</xsl:text><xsl:value-of select="../@empi"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@mrn"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@accessionNumber"/>
    <xsl:text>,"</xsl:text><xsl:value-of select="translate(FinalText, '&quot;&#183;&#160;', '&#182;  ')"/><xsl:text>"</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
