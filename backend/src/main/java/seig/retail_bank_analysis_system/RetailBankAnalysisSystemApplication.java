package seig.retail_bank_analysis_system;

import org.mybatis.spring.annotation.MapperScan; // 必须导入这个
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 告诉 Spring 去哪里找你的 Mapper 接口
@MapperScan("seig.retail_bank_analysis_system.dao")
public class RetailBankAnalysisSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetailBankAnalysisSystemApplication.class, args);
	}

}