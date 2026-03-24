package com.bank.transfersearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import java.util.List;

import java.time.LocalDateTime;

@Data
@Document(indexName = "transfer_contact_index")
@Setting(settingPath = "es-settings.json")
@Routing("userId")
public class ContactDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String contactName;

    @Field(type = FieldType.Text, analyzer = "pinyin_analyzer", searchAnalyzer = "standard")
    private String contactPinyin;

    @Field(type = FieldType.Keyword)
    private String contactInitial;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String bankName;

    @Field(type = FieldType.Keyword)
    private String phone;

    @CompletionField
    private List<String> contactSuggest;

    // Highlight field to pass the highlighted name during search
    private String highlightName;

    @Field(type = FieldType.Date, format = org.springframework.data.elasticsearch.annotations.DateFormat.date_hour_minute_second_millis)
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createTime;

}
