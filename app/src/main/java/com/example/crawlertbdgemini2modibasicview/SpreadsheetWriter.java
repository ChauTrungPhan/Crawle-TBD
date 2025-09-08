package com.example.crawlertbdgemini2modibasicview;

import java.io.IOException;
import java.io.Writer;

public class SpreadsheetWriter {    //Dùng XML trực tiếp tạo sheet
    private final Writer writer;
    private static final String[] BG_COLORS = {"", "#FFFF99", "#FFCCCC"};

    public SpreadsheetWriter(Writer writer) {
        this.writer = writer;
    }

    public void beginWorkbook() throws Exception {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
        writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        writer.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
    }

    public void writeStyles() throws Exception {
        writer.write("<Styles>\n");
        writer.write(" <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n");
        writer.write("  <Alignment ss:Vertical=\"Center\"/>\n");
        writer.write("  <Borders/>\n");
        writer.write("  <Font ss:FontName=\"Arial\" ss:Size=\"10\"/>\n");
        writer.write("  <Interior/>\n");
        writer.write("  <NumberFormat/>\n");
        writer.write("  <Protection/>\n");
        writer.write(" </Style>\n");
        writer.write(" <Style ss:ID=\"Yellow\">\n");
        writer.write("  <Interior ss:Color=\"#FFFF99\" ss:Pattern=\"Solid\"/>\n");  // Vàng
        writer.write(" </Style>\n");
        writer.write(" <Style ss:ID=\"Red\">\n");
        writer.write("  <Interior ss:Color=\"#FF9999\" ss:Pattern=\"Solid\"/>\n");  // Đỏ
        writer.write(" </Style>\n");
        writer.write("  <Style ss:ID=\"Green\"><Interior ss:Color=\"#99FF99\" ss:Pattern=\"Solid\"/></Style>\n"); // Xanh lá
        writer.write("</Styles>\n");


        // Các màu nền tùy theo index_color
//        writer.write("  <Style ss:ID=\"RowColor1\"><Interior ss:Color=\"#FFFF99\" ss:Pattern=\"Solid\"/></Style>\n"); // Vàng
//        writer.write("  <Style ss:ID=\"RowColor2\"><Interior ss:Color=\"#FF9999\" ss:Pattern=\"Solid\"/></Style>\n"); // Đỏ
//        writer.write("  <Style ss:ID=\"RowColor3\"><Interior ss:Color=\"#99FF99\" ss:Pattern=\"Solid\"/></Style>\n"); // Xanh lá
//        writer.write("  <Style ss:ID=\"RowColor0\"/> <!-- Không màu -->\n");
//        writer.write(" </Styles>\n");
    }
    public void beginWorksheet(String sheetName) throws Exception {
        writer.write("<Worksheet ss:Name=\"" + sheetName + "\">\n");
    }

    public void beginTable(int columnCount, int rowCount) throws Exception {
        writer.write("<Table ss:ExpandedColumnCount=\"" + columnCount + "\" ss:ExpandedRowCount=\"" + rowCount + "\">\n");
    }

    public void writeRowStart(String styleId) throws Exception {
        if (styleId != null && !styleId.isEmpty()) {
            writer.write("<Row ss:StyleID=\"" + styleId + "\">\n");
        } else {
            writer.write("<Row>\n");
        }
    }

    public void writeCell(String value) throws Exception {
        writer.write("<Cell><Data ss:Type=\"String\">" + value + "</Data></Cell>\n");
    }

    public void writeHyperlinkCell(String display, String link) throws Exception {
        writer.write("<Cell><Data ss:Type=\"String\"><![CDATA[=HYPERLINK(\"" + link + "\",\"" + display + "\")]]></Data></Cell>\n");
    }

    public void endRow() throws Exception {
        writer.write("</Row>\n");
    }

    public void endTable() throws Exception {
        writer.write("</Table>\n");
    }

    public void endWorksheet() throws Exception {
        writer.write("</Worksheet>\n");
    }

    public void endWorkbook() throws Exception {
        writer.write("</Workbook>\n");
    }

    public void close() throws Exception {
        writer.close();
    }


    /// ///////////
    public void beginSheet() throws IOException {

        //out = writer
//        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
//        writer.write("<sheetData>\n");
//        // Bổ sung
        // --- 1. Ghi phần Header XML và định nghĩa Styles ---
        writer.write("<?xml version=\"1.0\"?>\n");
        writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
        writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");

        writer.write(" <Styles>\n");
        writer.write("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n");
        writer.write("   <Alignment ss:Vertical=\"Center\"/>\n");
        writer.write("   <Borders/>\n");
        writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\"/>\n");
        writer.write("   <Interior/>\n");
        writer.write("   <NumberFormat/>\n");
        writer.write("   <Protection/>\n");
        writer.write("  </Style>\n");

        // Style cho ô có nền vàng (Yellow)
        writer.write(" <Style ss:ID=\"Yellow\">\n");
        writer.write("  <Interior ss:Color=\"#FFFF99\" ss:Pattern=\"Solid\"/>\n");
        writer.write(" </Style>\n");
        // Style cho ô có nền đỏ (Red)
        writer.write(" <Style ss:ID=\"Red\">\n");
        writer.write("  <Interior ss:Color=\"#FF9999\" ss:Pattern=\"Solid\"/>\n");
        writer.write(" </Style>\n");
        writer.write("</Styles>\n");

        // Các màu nền tùy theo index_color
        writer.write("  <Style ss:ID=\"RowColor1\"><Interior ss:Color=\"#FFFF99\" ss:Pattern=\"Solid\"/></Style>\n"); // Vàng
        writer.write("  <Style ss:ID=\"RowColor2\"><Interior ss:Color=\"#FF9999\" ss:Pattern=\"Solid\"/></Style>\n"); // Đỏ
        writer.write("  <Style ss:ID=\"RowColor3\"><Interior ss:Color=\"#99FF99\" ss:Pattern=\"Solid\"/></Style>\n"); // Xanh lá
        writer.write("  <Style ss:ID=\"RowColor0\"/> <!-- Không màu -->\n");
        writer.write(" </Styles>\n");

    }

    public void endSheet() throws IOException {
        writer.write("</sheetData>\n</worksheet>");
    }

    public void insertRow(int rownum, int indexColor) throws IOException {
        String attr = "";
        if (indexColor > 0) {
            attr = " s=\"" + indexColor + "\"";
        }
        writer.write("<row r=\"" + rownum + "\">");
    }

//    public void endRow() throws IOException {
//        writer.write("</row>\n");
//    }

    public void createCell(int columnIndex, String value) throws IOException {
        String cellRef = getCellRef(columnIndex);
        writer.write("<c r=\"" + cellRef + "\" t=\"inlineStr\">");
        writer.write("<is><t>" + escape(value) + "</t></is></c>");
    }

    private String getCellRef(int col) {
        return Character.toString((char)('A' + col));
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}

