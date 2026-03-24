package com.bank.transfersearch.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.dto.SearchResponseDTO;
import com.bank.transfersearch.entity.ContactDocument;
import com.bank.transfersearch.mapper.ContactMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ContactSearchRepository {

        private final ElasticsearchOperations elasticsearchOperations;
        private final ElasticsearchClient elasticsearchClient;
        private final ContactMapper contactMapper;

        public SearchResponseDTO searchContacts(Long userId, String keyword, Set<String> recentContactNames) {
                String safeKeyword = keyword == null ? "" : keyword;
                String pinyinPart = safeKeyword.replaceAll("[\\u4e00-\\u9fa5]", "").toLowerCase();
                String chinesePart = safeKeyword.replaceAll("[a-zA-Z]", "");
                boolean hasChinese = !chinesePart.isEmpty();
                boolean hasPinyin = !pinyinPart.isEmpty();

                // 1. MUST clause (userId)
                BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                boolQueryBuilder.must(m -> m.term(t -> t.field("userId").value(userId)));

                // 2. SHOULD clause (match keyword against fields)
                boolQueryBuilder.should(s -> s.match(m -> m.field("contactName").query(safeKeyword).queryName("contactName")));
                boolQueryBuilder.should(s -> s.match(m -> m.field("contactPinyin").query(safeKeyword).queryName("contactPinyin")));
                boolQueryBuilder.should(s -> s.prefix(p -> p.field("contactPinyin").value(safeKeyword.toLowerCase()).queryName("contactPinyin_prefix")));
                boolQueryBuilder.should(s -> s.term(t -> t.field("contactInitial").value(safeKeyword.toLowerCase()).queryName("contactInitial")));
                boolQueryBuilder.should(s -> s.match(m -> m.field("contactPinyin").query(safeKeyword).fuzziness("AUTO").queryName("contactPinyin_fuzziness")));
                boolQueryBuilder.should(s -> s.match(m -> m.field("bankName").query(safeKeyword).queryName("bankName")));
                boolQueryBuilder.should(s -> s.prefix(p -> p.field("phone").value(safeKeyword).queryName("phone")));

                // Mixed input logic
                if (hasChinese && hasPinyin) {
                        boolQueryBuilder.should(s -> s.bool(b -> b
                                .must(m -> m.match(match -> match.field("contactName").query(chinesePart)))
                                .must(m -> m.prefix(p -> p.field("contactPinyin").value(pinyinPart)))
                                .queryName("mixed_input")
                        ));
                }

                boolQueryBuilder.minimumShouldMatch("1");

                // 3. Function Score Query for custom scoring
                List<FunctionScore> functionScores = new ArrayList<>();

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("contactName").query(safeKeyword)))
                                .weight(5.0)));

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.term(t -> t.field("contactInitial").value(safeKeyword.toLowerCase())))
                                .weight(4.0)));

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("contactPinyin").query(safeKeyword)))
                                .weight(3.0)));

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("bankName").query(safeKeyword)))
                                .weight(1.0)));

                // Add boost for recent contacts
                if (recentContactNames != null && !recentContactNames.isEmpty()) {
                        for (String recentName : recentContactNames) {
                                functionScores.add(FunctionScore.of(f -> f
                                                .filter(q -> q.term(
                                                                t -> t.field("contactName.keyword").value(recentName)))
                                                .weight(10.0) // High boost for recent contacts
                                ));
                        }
                }

                FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(f -> f
                                .query(boolQueryBuilder.build()._toQuery())
                                .functions(functionScores)
                                .boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Sum)
                                .scoreMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode.Sum));

                // 4. Highlighting
                HighlightFieldParameters parameters = HighlightFieldParameters.builder()
                                .withPreTags("<em>")
                                .withPostTags("</em>")
                                .build();
                org.springframework.data.elasticsearch.core.query.highlight.HighlightField highlightField = new org.springframework.data.elasticsearch.core.query.highlight.HighlightField(
                                "contactName", parameters);
                Highlight highlight = new Highlight(List.of(highlightField));
                HighlightQuery highlightQuery = new HighlightQuery(highlight, ContactDocument.class);

                // Build Native Query
                NativeQuery nativeQuery = new NativeQueryBuilder()
                                .withQuery(functionScoreQuery._toQuery())
                                .withHighlightQuery(highlightQuery)
                                .withRoute(String.valueOf(userId))
                                .build();

                // 5. Execute search
                SearchHits<ContactDocument> searchHits = elasticsearchOperations.search(
                                nativeQuery,
                                ContactDocument.class,
                                IndexCoordinates.of("transfer_contact_index"));

                // 6. Map results and attach highlights/scores/matchedFields
                List<ContactDTO> results = searchHits.getSearchHits().stream().map(hit -> {
                        ContactDocument document = hit.getContent();
                        ContactDTO dto = contactMapper.toDTO(document);
                        
                        List<String> highlightValues = hit.getHighlightField("contactName");
                        if (highlightValues != null && !highlightValues.isEmpty()) {
                                dto.setHighlightName(highlightValues.get(0));
                        } else {
                                dto.setHighlightName(document.getContactName());
                        }

                        dto.setScore((double) hit.getScore());
                        dto.setMatchedFields(hit.getMatchedQueries());
                        
                        return dto;
                }).collect(Collectors.toList());

                SearchResponseDTO response = new SearchResponseDTO();
                response.setResults(results);
                response.setTook(0L); // SearchHits in Spring Data ES doesn't easily expose 'took'
                
                // Extract DSL (approximate since NativeQuery doesn't expose it directly as JSON string easily)
                response.setDsl(nativeQuery.getQuery() != null ? nativeQuery.getQuery().toString() : "");

                return response;
        }

        public List<String> analyze(String text, String analyzer) {
                try {
                        AnalyzeRequest request = AnalyzeRequest.of(a -> a
                                .index("transfer_contact_index")
                                .analyzer(analyzer != null ? analyzer : "pinyin_analyzer")
                                .text(text)
                        );
                        AnalyzeResponse response = elasticsearchClient.indices().analyze(request);
                        return response.tokens().stream()
                                .map(AnalyzeToken::token)
                                .collect(Collectors.toList());
                } catch (IOException e) {
                        log.error("Failed to analyze text: {}", text, e);
                        return new ArrayList<>();
                }
        }

        public List<String> suggestContacts(String prefix) {
                co.elastic.clients.elasticsearch.core.search.Suggester suggester = co.elastic.clients.elasticsearch.core.search.Suggester.of(s -> s
                        .suggesters("contact-suggest", co.elastic.clients.elasticsearch.core.search.FieldSuggester.of(fs -> fs
                                .prefix(prefix)
                                .completion(c -> c.field("contactSuggest"))
                        ))
                );

                NativeQuery nativeQuery = new NativeQueryBuilder()
                        .withSuggester(suggester)
                        .build();

                SearchHits<ContactDocument> searchHits = elasticsearchOperations.search(
                        nativeQuery,
                        ContactDocument.class,
                        IndexCoordinates.of("transfer_contact_index"));

                List<String> results = new ArrayList<>();
                if (searchHits.hasSuggest() && searchHits.getSuggest().getSuggestion("contact-suggest") != null) {
                        searchHits.getSuggest().getSuggestion("contact-suggest").getEntries().forEach(entry -> {
                                entry.getOptions().forEach(option -> {
                                        results.add(option.getText());
                                });
                        });
                }
                return results.stream().distinct().collect(Collectors.toList());
        }

        public void save(ContactDocument document) {
                elasticsearchOperations.save(document, IndexCoordinates.of("transfer_contact_index"));
        }
}
