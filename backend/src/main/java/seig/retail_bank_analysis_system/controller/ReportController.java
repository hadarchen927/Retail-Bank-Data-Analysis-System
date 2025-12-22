package seig.retail_bank_analysis_system.controller;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.dao.RiskRecordMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private BankCustomerMapper customerMapper;
    @Autowired
    private RiskRecordMapper riskRecordMapper;

    @GetMapping("/export")
    public void exportPdf(HttpServletResponse response) throws Exception {
        // 1. 设置响应头
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Bank_Analysis_Report.pdf");

        // 2. 创建 PDF 文档
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 3. 解决中文乱码 (使用内置的亚洲字体)
        PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        document.setFont(font);

        // 4. 写入标题和时间
        document.add(new Paragraph("零售银行数据分析月度报告").setFontSize(20).setBold());
        document.add(new Paragraph("报告生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(new Paragraph("\n"));

        // 5. 写入核心业务指标
        Map<String, Object> stats = customerMapper.getGlobalStats();
        document.add(new Paragraph("一、业务运行概览").setBold());
        document.add(new Paragraph("管理客户总数：" + stats.get("totalCustomers") + " 人"));
        document.add(new Paragraph("全行管理资产 (AUM)：￥" + stats.get("totalAssets")));
        document.add(new Paragraph("当前信贷余额：￥" + stats.get("totalLoans")));
        document.add(new Paragraph("\n"));

        // 6. 写入风险预警摘要 (表格形式)
        document.add(new Paragraph("二、风险监控摘要").setBold());

        // 创建一个 3 列的表格，比例为 3:5:2
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 5, 2}));

        // 【核心修复点】使用此方法让表格铺满页面宽度
        table.useAllAvailableWidth();

        table.addHeaderCell("预警客户");
        table.addHeaderCell("风险描述");
        table.addHeaderCell("等级");

        // 从数据库查出前 10 条记录填充表格
        riskRecordMapper.selectList(null).stream().limit(10).forEach(risk -> {
            table.addCell(risk.getCustName() != null ? risk.getCustName() : "未知");
            table.addCell(risk.getDescription() != null ? risk.getDescription() : "无描述");
            table.addCell(risk.getRiskLevel() != null ? risk.getRiskLevel() : "中");
        });

        document.add(table);

        document.add(new Paragraph("\n报告结论：本月资产状况运行平稳，部分高负债风险已标记，建议市场部针对潜力客户群体开展定向营销。"));

        document.close();
    }
}