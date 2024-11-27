package org.focus.logmeet.repository;

import org.focus.logmeet.domain.elasticsearch.MinutesDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface MinutesSearchRepository extends ElasticsearchRepository<MinutesDocument, Long> {
        @Query("""
                {
                  "bool": {
                    "must": [
                      {
                        "terms": { "id": ?1 }
                      },
                      {
                        "bool": {
                          "should": [
                            { "match": { "title": "?0" } },
                            { "match": { "content": "?0" } }
                          ]
                        }
                      }
                    ]
                  }
                }
                """)
    List<MinutesDocument> searchByQuery(String query, List<Long> ids);
}
