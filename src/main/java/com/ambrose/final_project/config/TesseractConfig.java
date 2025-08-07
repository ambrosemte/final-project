package com.ambrose.final_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;

@Configuration
public class TesseractConfig {

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath("/usr/local/Cellar/tesseract/5.5.1/share/tessdata/");
        tesseract.setLanguage("eng");

        tesseract.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-");

        tesseract.setPageSegMode(8); // Single word mode
        tesseract.setOcrEngineMode(1); // LSTM only

        tesseract.setVariable("tessedit_preserve_interword_spaces", "1");
        tesseract.setVariable("tessedit_adapt_to_char_fragments", "1");
        tesseract.setVariable("tessedit_adaption_mode", "1");

        return tesseract;
    }
}
