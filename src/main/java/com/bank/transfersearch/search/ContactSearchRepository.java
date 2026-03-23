package com.bank.transfersearch.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import com.bank.transfersearch.entity.ContactDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ContactSearchRepository {

        private final ElasticsearchOperations elasticsearchOperations;

        public List<ContactDocument> searchContacts(Long userId, String keyword, Set<String> recentContactNames) {

                // 1. MUST clause (userId)
                BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                boolQueryBuilder.must(m -> m.term(t -> t.field("userId").value(userId)));

                // 2. SHOULD clause (match keyword against fields)
                boolQueryBuilder.should(s -> s.match(m -> m.field("contactName").query(keyword)));
                boolQueryBuilder.should(s -> s.match(m -> m.field("contactPinyin").query(keyword)));
                boolQueryBuilder.should(s -> s.term(t -> t.field("contactInitial").value(keyword)));
                boolQueryBuilder.should(s -> s.match(m -> m.field("bankName").query(keyword)));
                boolQueryBuilder.should(s -> s.prefix(p -> p.field("phone").value(keyword)));

                boolQueryBuilder.minimumShouldMatch("1");

                // 3. Function Score Query for custom scoring
                List<FunctionScore> functionScores = new ArrayList<>();

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("contactName").query(keyword)))
                                .weight(5.0)));

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("contactPinyin").query(keyword)))
                                .weight(3.0)));

                functionScores.add(FunctionScore.of(f -> f
                                .filter(q -> q.match(m -> m.field("bankName").query(keyword)))
                                .weight(2.0)));

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

                // 6. Map results and attach highlights
                return searchHits.getSearchHits().stream().map(hit -> {
                        ContactDocument document = hit.getContent();
                        List<String> highlightValues = hit.getHighlightField("contactName");
                        if (highlightValues != null && !highlightValues.isEmpty()) {
                                document.setHighlightName(highlightValues.get(0));
                        } else {
                                document.setHighlightName(document.getContactName());
                        }
                        return document;
                }).collect(Collectors.toList());
        }

        public void save(ContactDocument document) {
                elasticsearchOperations.save(document, IndexCoordinates.of("transfer_contact_index"));
        }
}
