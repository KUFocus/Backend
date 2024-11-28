package org.focus.logmeet.repository;

import org.focus.logmeet.domain.elasticsearch.MinutesDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MinutesSearchRepository extends ElasticsearchRepository<MinutesDocument, Long> {
    @Query("""
    {
      "bool": {
        "filter": [
          {
            "terms": {
              "id": ?1
            }
          }
        ],
        "should": [
          {
            "match_phrase": {
              "content": {
                "query": "?0",
                "analyzer": "nori"
              }
            }
          },
          {
            "match_phrase": {
              "title": {
                "query": "?0",
                "analyzer": "nori"
              }
            }
          }
        ],
        "minimum_should_match": 1
      }
    }
    """)
    List<MinutesDocument> searchByQuery(@Param("query") String query, @Param("ids") List<Long> ids);
}
