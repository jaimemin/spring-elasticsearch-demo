package com.tistory.jaimemin.indexer.helper;

import java.io.IOException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ESHelper {

	public static void doBulk(ElasticsearchClient elasticsearchClient, BulkRequest.Builder br) throws IOException {
		BulkResponse result = elasticsearchClient.bulk(br.build());

		if (result.errors()) {
			log.error("ESHelper doBulk errors");

			for (BulkResponseItem item : result.items()) {
				if (item.error() != null) {
					log.error(item.error().reason());
				}
			}
		}
	}
}
