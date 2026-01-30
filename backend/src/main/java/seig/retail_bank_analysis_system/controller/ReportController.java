package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.dao.RiskRecordMapper;
import seig.retail_bank_analysis_system.entity.RiskRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private BankCustomerMapper customerMapper;

    @Autowired
    private RiskRecordMapper riskRecordMapper;

    /**
     * 导出PDF格式报告
     */
    @GetMapping("/export")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Bank_Analysis_Report.pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        document.setFont(font);

        document.add(new Paragraph("零售银行数据分析月度报告").setFontSize(20).setBold());
        document.add(new Paragraph("报告生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(new Paragraph("\n"));

        Map<String, Object> stats = customerMapper.getGlobalStats();
        document.add(new Paragraph("一、业务运行概览").setBold());
        document.add(new Paragraph("管理客户总数：" + stats.get("totalCustomers") + " 人"));
        document.add(new Paragraph("全行管理资产 (AUM)：￥" + stats.get("totalAssets")));
        document.add(new Paragraph("当前信贷余额：￥" + stats.get("totalLoans")));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("二、风险监控摘要").setBold());

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 5, 2}));
        table.useAllAvailableWidth();
        table.addHeaderCell("预警客户");
        table.addHeaderCell("风险描述");
        table.addHeaderCell("等级");

        riskRecordMapper.selectList(null).stream().limit(10).forEach(risk -> {
            table.addCell(risk.getCustName() != null ? risk.getCustName() : "未知");
            table.addCell(risk.getDescription() != null ? risk.getDescription() : "无描述");
            table.addCell(risk.getRiskLevel() != null ? risk.getRiskLevel() : "中");
        });

        document.add(table);
        document.add(new Paragraph("\n报告结论：本月资产状况运行平稳，部分高负债风险已标记，建议市场部针对潜力客户群体开展定向营销。"));
        document.close();
    }

    /**
     * 【新增】导出Word格式报告
     */
    @GetMapping("/export-word")
    public void exportWord(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=Bank_Analysis_Report.docx");

        XWPFDocument document = new XWPFDocument();

        // 标题
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("零售银行数据分析月度报告");
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setFontFamily("宋体");

        // 生成时间
        XWPFParagraph timePara = document.createParagraph();
        timePara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun timeRun = timePara.createRun();
        timeRun.setText("报告生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeRun.setFontFamily("宋体");
        timeRun.setFontSize(10);

        // 业务运行概览
        XWPFParagraph section1 = document.createParagraph();
        XWPFRun section1Run = section1.createRun();
        section1Run.setText("一、业务运行概览");
        section1Run.setBold(true);
        section1Run.setFontSize(14);
        section1Run.setFontFamily("宋体");
        section1Run.addBreak();

        Map<String, Object> stats = customerMapper.getGlobalStats();

        XWPFParagraph statsPara = document.createParagraph();
        XWPFRun statsRun = statsPara.createRun();
        statsRun.setText("管理客户总数：" + stats.get("totalCustomers") + " 人");
        statsRun.setFontFamily("宋体");
        statsRun.addBreak();
        statsRun.setText("全行管理资产 (AUM)：￥" + stats.get("totalAssets"));
        statsRun.addBreak();
        statsRun.setText("当前信贷余额：￥" + stats.get("totalLoans"));
        statsRun.addBreak();

        // 风险监控摘要
        XWPFParagraph section2 = document.createParagraph();
        XWPFRun section2Run = section2.createRun();
        section2Run.setText("二、风险监控摘要");
        section2Run.setBold(true);
        section2Run.setFontSize(14);
        section2Run.setFontFamily("宋体");
        section2Run.addBreak();

        // 创建表格
        XWPFTable table = document.createTable();

        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("预警客户");
        headerRow.addNewTableCell().setText("风险描述");
        headerRow.addNewTableCell().setText("等级");

        List<RiskRecord> risks = riskRecordMapper.selectList(
                new LambdaQueryWrapper<RiskRecord>().last("LIMIT 10")
        );

        for (RiskRecord risk : risks) {
            XWPFTableRow dataRow = table.createRow();
            dataRow.getCell(0).setText(risk.getCustName() != null ? risk.getCustName() : "未知");
            dataRow.getCell(1).setText(risk.getDescription() != null ? risk.getDescription() : "无描述");
            dataRow.getCell(2).setText(risk.getRiskLevel() != null ? risk.getRiskLevel() : "中");
        }

        // 结论
        XWPFParagraph conclusion = document.createParagraph();
        XWPFRun conclusionRun = conclusion.createRun();
        conclusionRun.addBreak();
        conclusionRun.setText("报告结论：");
        conclusionRun.setBold(true);
        conclusionRun.setFontFamily("宋体");
        conclusionRun.addBreak();
        conclusionRun.setText("本月资产状况运行平稳，部分高负债风险已标记，建议市场部针对潜力客户群体开展定向营销。");
        conclusionRun.setFontFamily("宋体");

        document.write(response.getOutputStream());
        document.close();
    }
}