<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text" encoding="iso-8859-1"/>

<xsl:strip-space elements="*"/>

<xsl:template match="/">
  <xsl:text>+Pt-SourceNumber, Pt-SourceMrn, Pt-EMPI&#xa;</xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="/CoPathDump/Patient">
  <xsl:for-each select=".">
    <xsl:text>+</xsl:text><xsl:value-of select="@sourceRecord"/>
    <xsl:text>,</xsl:text><xsl:value-of select="@empi"/>
    <xsl:text>&#xa;</xsl:text>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
