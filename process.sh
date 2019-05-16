#!/bin/sh

split -l 10000 --additional-suffix=.csv MRN_list.csv MRN_list_

cat MRN_list_aa.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _aa > MRN_list_aa.log 2>&1 &
cat MRN_list_ab.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _ab > MRN_list_ab.log 2>&1 &
cat MRN_list_ac.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _ac > MRN_list_ac.log 2>&1 &
cat MRN_list_ad.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _ad > MRN_list_ad.log 2>&1 &
cat MRN_list_ae.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _ae > MRN_list_ae.log 2>&1 &
cat MRN_list_af.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _af > MRN_list_af.log 2>&1 &
cat MRN_list_ag.csv | java -Xmx4g -cp ~/NetBeansProjects/copathdump/target/uber-copathdump-1.0-SNAPSHOT.jar tech.gsmith.copathdump.DumpUtility "jdbc:sqlserver" _ag > MRN_list_ag.log 2>&1 &

xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_aa.xml  | sed 's/\xB6/""/g' > copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_ab.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_ac.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_ad.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_ae.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_af.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_part.xsl copathdump_ag.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_part.csv

xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_aa.xml  | sed 's/\xB6/""/g' > copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_ab.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_ac.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_ad.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_ae.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_af.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_synoptic.xsl copathdump_ag.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_synoptic.csv

xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_aa.xml  | sed 's/\xB6/""/g'  > copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_ab.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_ac.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_ad.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_ae.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_af.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
xsltproc ~/NetBeansProjects/copathdump/flatten_final_dx.xsl copathdump_ag.xml  | sed 's/\xB6/""/g' | tail +2 >> copathdump_final_dx.csv
