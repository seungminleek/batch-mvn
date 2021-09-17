package com.example.batchmvn;

import com.example.batchmvn.dto.ExchangeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor // 간편하게 생성자 주입을 위한 Lombok 사용
@Configuration
public class HelloBatchJob {

    private final String BATCH_NAME = "배치 시작합니다 by 승민";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final String bankApiUrl = "https://quotation-api-cdn.dunamu.com/v1/forex/recent?codes=FRX.KRWUSD";   // 환율정보 api
    private final RestTemplate restTemplate;
    private List<ExchangeDto> exchangeData = null; // 최종고시 환율정보 데이터
    private String testData = "";
    
    @Bean
    public Job job(){
        return jobBuilderFactory.get(BATCH_NAME)
                .start(step1())
                .next(step2())
                .build();
    }

    /**
     * 하나은행 rest api 호출 부분
     * @return
     */
    @Bean
    public Step step1() {
        ResponseEntity<ExchangeDto[]> response = restTemplate.getForEntity(bankApiUrl, ExchangeDto[].class );
        // 하나은행에서 받아온 json 데이터를 엔티티로 변환
        ExchangeDto[] resultData = response.getBody();
        List<ExchangeDto> dtoList = Arrays.asList(resultData);

        if (dtoList.size() > 0) {
            testData = dtoList.get(0).getName();
        }

        return stepBuilderFactory.get(BATCH_NAME + "Step1")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info(BATCH_NAME + "@@@@@@@ Step1 Started :: testData :::::" + testData);
                    return RepeatStatus.FINISHED;
                }).build();
    }

    @Bean
    public Step step2() {
        return stepBuilderFactory.get(BATCH_NAME + "Step2")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info((BATCH_NAME + "@@@@@@@  Step2 Started"));
                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * 최종 고시된 환율정보 데이터 유무 확인
     * @return boolean
     */
    private boolean isNotInitialized() {
        return this.exchangeData == null;
    }

}
