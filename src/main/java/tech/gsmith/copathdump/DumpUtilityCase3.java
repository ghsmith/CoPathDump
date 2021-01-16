package tech.gsmith.copathdump;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import tech.gsmith.copathdump.data.generated.CoPathDump;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport.CPSRItem;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport.CPSRItem.CPSRItemValue;

/**
 *
 * @author Geoffrey H. Smith
 */
public class DumpUtilityCase3 {

    public static void main(String[] args) throws Exception {

        SimpleDateFormat sdfExcel = new SimpleDateFormat("MM/dd/yyyy");
    
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  (select universal_mednum_stripped from dbo.r_pat_demograph pd where patdemog_id = s.patdemog_id) as empi, "
          + "  (select abbr from dbo.c_d_client where id = s.client_id) as mrn_facility, "
          + "  (select max(medrec_num_stripped) from dbo.r_medrec where client_id = s.client_id and patdemog_id = s.patdemog_id) as mrn, "
          + "  s.specnum_formatted as accession_no, "
          + "  p.part_designator as part_designator, "
          + "  dp.abbr as part_type, "
          + "  dp.name as part_type_name, "
          + "  p.part_description as part_description, "
          + "  dsw.abbr as worksheet, "
          + "  dsw.version as worksheet_version, "
          + "  dsw.wstitle as worksheet_name, "
          + "  ssd.inst as item_no, "
          + "  dsc.abbr as item, "
          + "  dsc.name as item_name, "
          + "  dsv.abbr as val, "
          + "  dsv.name as val_name, "
          + "  dsv.fillin_type as val_freetext_type, "
          + "  ssd.fillin_char as val_freetext_char, "
          + "  ssd.fillin_number as val_freetext_number, "
          + "  dsv.snomed_id as snomed_id, "
          + "  (select count(*) from dbo.r_medrec where client_id = s.client_id and patdemog_id = s.patdemog_id) as mrn_count, "
          + "  (select text_data from dbo.c_spec_text where specimen_id = s.specimen_id and texttype_id = '$final') as final_text, "
          + "  s.accession_date, "
          + "  p.datetime_taken, "
          + "  s.signout_date "
          + "from "
          + "  dbo.c_specimen s "
          + "  join dbo.p_part p on(p.specimen_id = s.specimen_id) "
          + "  join dbo.c_d_parttype dp on(dp.id = p.parttype_id) "
          + "  left outer join "
          + "  ( "
          + "    dbo.c_spec_synoptic_ws ssw "
          + "    join dbo.c_d_synoptic_ws dsw on(dsw.id = ssw.worksheet_id) "
          + "    left outer join "
          + "    ( "
          + "      dbo.c_spec_synoptic_dx ssd "
          + "      join dbo.c_d_synoptic_value dsv on(dsv.id = ssd.synoptic_value_id) "
          + "      join dbo.c_d_synoptic_category dsc on(dsc.id = dsv.category_id) "
          + "    ) on(ssd.specimen_id = ssw.specimen_id and ssd.worksheet_inst = ssw.ws_inst) "
          + "  ) on(ssw.specimen_id = s.specimen_id and ssw.part_inst = p.part_inst) "
          + "where "
          + "  s.patdemog_id in (select patdemog_id from dbo.r_pat_demograph where universal_mednum_stripped = ?) "
          + "order by s.accession_date, 4, 5, 9, cast(ssd.inst as int), 15 "
        );

        CoPathDump coPathDump = new CoPathDump();
        coPathDump.setDumpDate(sdfExcel.format(new Date()));
        coPathDump.setVersion("0.3");
        
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String empiLine;

        int count = 0;
        
        while ((empiLine = stdIn.readLine()) != null && empiLine.length() != 0) {
        
            String recNo = empiLine.split(",")[0];
            String empi = empiLine.split(",")[1];
            
            Patient patient = null;
            Case case_ = null;
            CasePart casePart = null;
            CasePartSynopticReport casePartSynopticReport = null;
            CPSRItem cpsrItem = null;
            CPSRItemValue cpsrItemValue = null;

            String lastAccessionNo = null;
            pstmt.clearParameters();
            pstmt.setString(1, empi);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {

                if(lastAccessionNo == null || !lastAccessionNo.equals(rs.getString("accession_no"))) {
                    System.out.println();
                    System.out.print(String.format("%5d [%s - %s]", ++count, recNo, rs.getString("accession_no")));
                }
                lastAccessionNo = rs.getString("accession_no");
                
                System.out.print(".");
                
                if(patient == null || !rs.getString("empi").equals(patient.getEmpi())) {
                    patient = new Patient();
                    case_ = null;
                    casePart = null;
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    coPathDump.getPatient().add(patient);
                    patient.setEmpi(rs.getString("empi"));
                    //patient.setSourceRecord(facilityMrnCsv);
                }
                
                if(case_ == null || !rs.getString("accession_no").equals(case_.getAccessionNumber())) {
                    case_ = new Case();
                    casePart = null;
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    patient.getCase().add(case_);
                    case_.setAccessionNumber(rs.getString("accession_no"));
                    //case_.setMrn(rs.getString("mrn_facility") + "_" + rs.getString("mrn"));
                    case_.setMrnCount(rs.getInt("mrn_count"));
                    case_.setCaseFinalText(
                        rs.getString("final_text") != null
                        ? rs.getString("final_text")
                            .replace("\u0008", "") // there are ASCII 08 (backspace?) characters in this column
                            .replace("\r", "")
                            .replaceAll("\\s+$","")
                        : null
                    );
                    case_.setAccessionDate(sdfExcel.format(rs.getTimestamp("accession_date")));
                    case_.setSignoutDate(rs.getTimestamp("signout_date") != null ? sdfExcel.format(rs.getTimestamp("signout_date")) : null);
                }
                
                if(casePart == null || !rs.getString("part_designator").equals(casePart.getDesignator())) {
                    casePart = new CasePart();
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    case_.getCasePart().add(casePart);
                    casePart.setDesignator(rs.getString("part_designator"));
                    casePart.setPartType(rs.getString("part_type"));
                    casePart.setPartTypeDisp(rs.getString("part_type_name"));
                    casePart.setDescription(rs.getString("part_description"));
                    if(rs.getString("final_text") != null) {
                        String finalDxPart = getFinalDxByParts(rs.getString("final_text")).get(rs.getString("part_designator"));
                        casePart.setPartFinalText(
                            finalDxPart != null
                            ? finalDxPart
                                .replace("\u0008", "") // there are ASCII 08 (backspace?) characters in this column
                                .replace("\r", "")
                                .replaceAll("\\s+$","")
                            : null
                        );
                    }
                    casePart.setCollectionDate(rs.getTimestamp("datetime_taken") != null ? sdfExcel.format(rs.getTimestamp("datetime_taken")) : null);
                }
                
                if(rs.getString("worksheet") != null) {
                
                    if(casePartSynopticReport == null || !rs.getString("worksheet").equals(casePartSynopticReport.getSrName())) {
                        casePartSynopticReport = new CasePartSynopticReport();
                        cpsrItem = null;
                        cpsrItemValue = null;
                        casePart.getCasePartSynopticReport().add(casePartSynopticReport);
                        casePartSynopticReport.setSrName(rs.getString("worksheet"));
                        casePartSynopticReport.setSrVersion(rs.getString("worksheet_version"));
                        casePartSynopticReport.setSrNameDisp(rs.getString("worksheet_name"));
                    }

                    if(cpsrItem == null || rs.getInt("item_no") != cpsrItem.getNumber()) {
                        cpsrItem = new CPSRItem();
                        cpsrItemValue = null;
                        casePartSynopticReport.getCPSRItem().add(cpsrItem);
                        cpsrItem.setNumber(rs.getInt("item_no"));
                        cpsrItem.setItemName(rs.getString("item"));
                        cpsrItem.setItemNameDisp(rs.getString("item_name"));
                    }

                    {
                        cpsrItemValue = new CPSRItemValue();
                        cpsrItem.getCPSRItemValue().add(cpsrItemValue);
                        cpsrItemValue.setValName(rs.getString("val"));
                        cpsrItemValue.setValNameDisp(rs.getString("val_name"));
                        cpsrItemValue.setSnomedConceptId(rs.getString("snomed_id"));
                        cpsrItemValue.setValue(rs.getString("val_freetext_char") != null ? rs.getString("val_freetext_char") : rs.getString("val_freetext_number"));
                    }
                    
                }
                
            }
            
        }

        System.out.println();
        
        pstmt.close();
        conn.close();

        // fix-ups:
        // 1. if there is only one part, just set the part dx to the entire dx
        // 2. strip leading/trailing newline(s) from narratives
        // 3. add ONE leading and ONE trailing newline to narratives
        {
            for(Patient patient : coPathDump.getPatient()) {
                for(Case case_ : patient.getCase()) {
                    if(case_.getCaseFinalText() != null) {
                        while(case_.getCaseFinalText().length() > 0 && case_.getCaseFinalText().startsWith("\n")) {
                            case_.setCaseFinalText(case_.getCaseFinalText().substring(1));
                        }
                        while(case_.getCaseFinalText().length() > 0 && case_.getCaseFinalText().endsWith("\n")) {
                            case_.setCaseFinalText(case_.getCaseFinalText().substring(0, case_.getCaseFinalText().length() - 1));
                        }
                        case_.setCaseFinalText("\n" + case_.getCaseFinalText() + "\n");
                    }
                    if(case_.getCasePart().size() == 1) {
                        case_.getCasePart().get(0).setPartFinalText(case_.getCaseFinalText());
                    }
                    else {
                        for(CasePart casePart : case_.getCasePart()) {
                            if(casePart.getPartFinalText() != null) {
                                while(casePart.getPartFinalText().length() > 0 && casePart.getPartFinalText().startsWith("\n")) {
                                    casePart.setPartFinalText(casePart.getPartFinalText().substring(1));
                                }
                                while(casePart.getPartFinalText().length() > 0 && casePart.getPartFinalText().endsWith("\n")) {
                                    casePart.setPartFinalText(casePart.getPartFinalText().substring(0, casePart.getPartFinalText().length() - 1));
                                }
                                casePart.setPartFinalText("\n" + casePart.getPartFinalText() + "\n");
                            }
                        }
                    }
                }
            }
        }
        
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] {CoPathDump.class});
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(coPathDump, new FileOutputStream(new File("copathdump.xml")));
        }

    }
        
    public static Map<String, String> getFinalDxByParts(String finalDx) throws IOException {
        
        Map<String, String> finalDxByParts = new HashMap<>();
        
        BufferedReader finalDxReader = new BufferedReader(new StringReader(finalDx));
        String partDesignator = null;
        StringBuffer finalDxPart = null;
        String finalDxLine;
        Pattern patternFinalDx = Pattern.compile("^\\s*([A-Za-z])\\..*$");
        
        while ((finalDxLine = finalDxReader.readLine()) != null) {        
            
            Matcher matcherFinalDx = patternFinalDx.matcher(finalDxLine);
            
            if(matcherFinalDx.matches()) {
                if(partDesignator != null) {
                    finalDxByParts.put(partDesignator, finalDxPart.toString());
                }
                partDesignator = matcherFinalDx.group(1).toUpperCase();
                finalDxPart = new StringBuffer();
                finalDxPart.append(finalDxLine + "\n");
            }
            else {
                if(partDesignator != null) {
                    finalDxPart.append(finalDxLine + "\n");
                }
            }
            
        }

        if(partDesignator != null) {
            finalDxByParts.put(partDesignator, finalDxPart.toString());
        }
        
        return finalDxByParts;
        
    }
    
}
