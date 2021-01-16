<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 
  Converts quotation marks to &#182; (paragraph mark).
  Pipe through sed to get quotation marks escaped for
  Excel: sed 's/\xB6/""/g'.
-->

<xsl:output method="text" encoding="iso-8859-1"/>

<xsl:strip-space elements="*"/>

<xsl:template match="/">
  <xsl:text>+,Pt-EMPI, Case-AccessionNumber, Case-AccessionDate, Case-SignoutDate, FirstPart-CollectionDate, Case-FinalText&#xa;</xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="/CoPathDump/Patient/Case">
  <xsl:for-each select=".">
    <xsl:text>+</xsl:text>
    <xsl:text>,</xsl:text><xsl:value-of select="../@empi"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@accessionNumber"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@accessionDate"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@signoutDate"/>
    <xsl:text>,</xsl:text><xsl:value-of select="CasePart[1]/@collectionDate"/>
    <xsl:text>,"</xsl:text><xsl:value-of select="translate(CaseFinalText, '&quot;&#183;&#160;', '&#182;  ')"/><xsl:text>"</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="/CoPathDump/Patient/Case/CaseFinalText">
</xsl:template>

</xsl:stylesheet>
