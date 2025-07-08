package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QViewHistory is a Querydsl query type for ViewHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QViewHistory extends EntityPathBase<ViewHistory> {

    private static final long serialVersionUID = 1144547940L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QViewHistory viewHistory = new QViewHistory("viewHistory");

    public final NumberPath<Long> contentId = createNumber("contentId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    public final EnumPath<ViewHistory.ContentType> type = createEnum("type", ViewHistory.ContentType.class);

    public final DateTimePath<java.time.LocalDateTime> viewedAt = createDateTime("viewedAt", java.time.LocalDateTime.class);

    public QViewHistory(String variable) {
        this(ViewHistory.class, forVariable(variable), INITS);
    }

    public QViewHistory(Path<? extends ViewHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QViewHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QViewHistory(PathMetadata metadata, PathInits inits) {
        this(ViewHistory.class, metadata, inits);
    }

    public QViewHistory(Class<? extends ViewHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

