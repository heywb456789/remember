package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShareHistory is a Querydsl query type for ShareHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QShareHistory extends EntityPathBase<ShareHistory> {

    private static final long serialVersionUID = -1038651872L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShareHistory shareHistory = new QShareHistory("shareHistory");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    public final DateTimePath<java.time.LocalDateTime> sharedAt = createDateTime("sharedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<ShareHistory.ShareTargetType> targetType = createEnum("targetType", ShareHistory.ShareTargetType.class);

    public QShareHistory(String variable) {
        this(ShareHistory.class, forVariable(variable), INITS);
    }

    public QShareHistory(Path<? extends ShareHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShareHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShareHistory(PathMetadata metadata, PathInits inits) {
        this(ShareHistory.class, metadata, inits);
    }

    public QShareHistory(Class<? extends ShareHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

