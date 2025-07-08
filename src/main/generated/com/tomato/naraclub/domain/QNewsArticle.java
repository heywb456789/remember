package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNewsArticle is a Querydsl query type for NewsArticle
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNewsArticle extends EntityPathBase<NewsArticle> {

    private static final long serialVersionUID = 911282904L;

    public static final QNewsArticle newsArticle = new QNewsArticle("newsArticle");

    public final StringPath content = createString("content");

    public final StringPath externalId = createString("externalId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> publishedAt = createDateTime("publishedAt", java.time.LocalDateTime.class);

    public final StringPath source = createString("source");

    public final StringPath title = createString("title");

    public final StringPath url = createString("url");

    public QNewsArticle(String variable) {
        super(NewsArticle.class, forVariable(variable));
    }

    public QNewsArticle(Path<? extends NewsArticle> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNewsArticle(PathMetadata metadata) {
        super(NewsArticle.class, metadata);
    }

}

