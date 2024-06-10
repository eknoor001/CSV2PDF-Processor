package com.demo.config;

import java.io.FileOutputStream;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.demo.model.Loan;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

@Configuration
@EnableBatchProcessing
public class CsvToPdfConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public FlatFileItemReader<Loan> csvReader() {
        FlatFileItemReader<Loan> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("Loan_data.csv")); 
        reader.setLinesToSkip(1); 
        reader.setLineMapper(new DefaultLineMapper<Loan>() {
            {
                setLineTokenizer(new DelimitedLineTokenizer() {
                    {
                        setNames(new String[]{"loan_id", "status", "principal", "terms", "effective_date", "due_date", "paid_off_time", "past_due_days", "age", "education", "gender"});
                    }
                });
                setFieldSetMapper(new BeanWrapperFieldSetMapper<Loan>() {
                    {
                        setTargetType(Loan.class);
                    }
                });
            }
        });
        return reader;
    }

    
    @Bean
    public ItemWriter<Loan> pdfWriter() {
        return items -> {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream("output.pdf")); 

            document.open();
            for (Loan loan : items) {
                String loanData = String.format("Loan ID: %s%nStatus: %s%nPrincipal: %d%nTerms: %d%nEffective Date: %s%nDue Date: %s%nPaid Off Time: %s%nPast Due Days: %d%nAge: %d%nEducation: %s%nGender: %s%n%n",
                        loan.getLoan_id(), loan.getStatus(), loan.getPrincipal(), loan.getTerms(), loan.getEffective_date(), loan.getDue_date(),
                        loan.getPaid_off_time(), loan.getPast_due_days(), loan.getAge(), loan.getEducation(), loan.getGender());

                document.add(new Paragraph(loanData));
            }
            document.close();
        };
    }



    @Bean
    public ItemProcessor<Loan, Loan> loanProcessor() {
        return loan -> {
            return loan;
        };
    }

    
    @Bean
    public Step csvToPdfStep(ItemReader<Loan> reader, ItemProcessor<Loan, Loan> processor, ItemWriter<Loan> writer) {
        return stepBuilderFactory.get("csvToPdfStep")
                .<Loan, Loan>chunk(Integer.MAX_VALUE)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job csvToPdfJob(Step csvToPdfStep) {
        return jobBuilderFactory.get("csvToPdfJob")
                .incrementer(new RunIdIncrementer())
                .flow(csvToPdfStep)
                .end()
                .build();
    }
}
