package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPointHistory is a Querydsl query type for PointHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPointHistory extends EntityPathBase<PointHistory> {

    private static final long serialVersionUID = 615436111L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPointHistory pointHistory = new QPointHistory("pointHistory");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> earnedAt = createDateTime("earnedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    public final StringPath reason = createString("reason");

    public QPointHistory(String variable) {
        this(PointHistory.class, forVariable(variable), INITS);
    }

    public QPointHistory(Path<? extends PointHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPointHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPointHistory(PathMetadata metadata, PathInits inits) {
        this(PointHistory.class, metadata, inits);
    }

    public QPointHistory(Class<? extends PointHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

