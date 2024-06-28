# spring-elasticsearch-demo

## es-index-platform

크롤링을 통해 데이터를 받아온 뒤 Elasticsearch에 색인하는 플랫폼

### 1. CrawlJob

resources > product_sample.csv로부터 데이터를 크롤링해 products 테이블에 넣어주는 배치 잡

#### products 테이블 DDL

```.sql
create table db.products
(
    id                   int auto_increment primary key,
    asin                 varchar(255)                        not null,
    title                text                                null,
    img_url              text                                null,
    product_url          text                                null,
    stars                float                               null,
    reviews              int                                 null,
    price                decimal(10, 2)                      null,
    list_price           decimal(10, 2)                      null,
    category_id          int                                 null,
    is_best_seller       tinyint(1)                          null,
    bought_in_last_month int                                 null,
    is_recommend_seller  tinyint(1)                          null,
    created_at           timestamp default CURRENT_TIMESTAMP null,
    updated_at           timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint unique_asin unique (asin)
);
```

### 2. StaticIndexJob

* 새로운 인덱스를 생성하고 전체 데이터를 넣음 (staticIndexJobStep)
* 정적 색인이 진행되는 동안 변경된 데이터를 넣음 (staticIndexJobStep2)
* 전체 데이터가 전부 색인되면 해당 인덱스로 alias 변경 (staticIndexJobStep3)

#### index template

```.json
PUT _index_template/products_template
{
  "index_patterns": [
    "products_*"
  ],
  "template": {
    "settings": {
      "index": {
        "number_of_shards": 1,
        "number_of_replicas": 1,
        "routing": {
          "allocation": {
            "include": {
              "_tier_preference": "data_content"
            }
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "asin": {
          "type": "keyword"
        },
        "title": {
          "type": "text",
          "analyzer": "english"
        },
        "img_url": {
          "type": "keyword",
          "index": false
        },
        "product_url": {
          "type": "keyword",
          "index": false
        },
        "stars": {
          "type": "scaled_float",
          "scaling_factor": 10
        },
        "reviews": {
          "type": "integer"
        },
        "price": {
          "type": "scaled_float",
          "scaling_factor": 100
        },
        "list_price": {
          "type": "scaled_float",
          "scaling_factor": 100
        },
        "category_id": {
          "type": "keyword"
        },
        "is_best_seller": {
          "type": "boolean"
        },
        "bought_in_last_month": {
          "type": "integer"
        },
        "is_recommend_seller": {
          "type": "boolean"
        },
        "created_at": {
          "type": "date"
        },
        "updated_at": {
          "type": "date"
        }
      }
    }
  },
  "version": 1
}
```

### 3. DynamicIndexJob

* 데이터가 지속적으로 추가되거나 변경되는 환경을 대응하기 위해 동적 색인 구현
* 1분 단위로 체크하여 새로운 데이터가 추가되었을 경우 새로운 데이터에 대해서만 색인 갱신

---

## es-search-platform

es-index-platform에서 색인한 데이터를 정제해서 rest api로 제공해주는 플랫폼

### categories 테이블 DDL

```.sql
create table db.categories
(
    id            int          not null primary key,
    category_name varchar(255) not null
);
```

### categories 테이블 내 데이터

<img width="236" alt="image" src="https://github.com/jaimemin/spring-elasticsearch-demo/assets/24821151/2407e636-24e3-4aa5-a1b5-d0e6876c49e8">

### Elasticsearch 쿼리

```.json
GET products/_search
{
  "search_after": [1.48, "B08J4FKXJ2"],
  "query": {
    "function_score": {
      "query": {
        "match": {
          "title": "pants"
        }
      },
      "score_mode": "sum",
      "boost_mode": "replace", 
      "functions": [
        {
          "filter": {
            "match": {
              "title": "pants"
            }
          },
          "weight": 1
        },
        {
          "field_value_factor": {
            "field": "stars"
          },
          "weight": 0.1
        },
        {
          "filter": {
            "match": {
              "is_recommend_seller": true
            }
          },
          "weight": 0.2
        }
      ]
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    },
    {
      "asin": {
        "order": "desc"
      }
    }
  ]
}
```
