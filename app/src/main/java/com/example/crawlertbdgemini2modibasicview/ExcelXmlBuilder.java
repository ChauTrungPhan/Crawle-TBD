package com.example.crawlertbdgemini2modibasicview;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExcelXmlBuilder {

    public void buildExcelXml(List<List<String>> data, List<Integer> indexColors, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

        writer.write("<?xml version=\"1.0\"?>\n");
        writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ...>\n");

        for (int i = 0; i < data.size(); i++) {
            List<String> row = data.get(i);
            int colorIndex = indexColors.get(i);

            writer.write("<Row>\n");
            for (String cell : row) {
                writer.write("<Cell><Data ss:Type=\"String\">" + escapeXml(cell) + "</Data></Cell>\n");
            }
            writer.write("</Row>\n");
        }

        writer.write("</Workbook>");
        writer.close();
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

