package com.tomato.naraclub.application.board.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardPostLike is a Querydsl query type for BoardPostLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardPostLike extends EntityPathBase<BoardPostLike> {

    private static final long serialVersionUID = -1014719273L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardPostLike boardPostLike = new QBoardPostLike("boardPostLike");

    public final com.tomato.naraclub.common.audit.QAudit _super = new com.tomato.naraclub.common.audit.QAudit(this);

    public final QBoardPost boardPost;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QBoardPostLike(String variable) {
        this(BoardPostLike.class, forVariable(variable), INITS);
    }

    public QBoardPostLike(Path<? extends BoardPostLike> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardPostLike(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardPostLike(PathMetadata metadata, PathInits inits) {
        this(BoardPostLike.class, metadata, inits);
    }

    public QBoardPostLike(Class<? extends BoardPostLike> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.boardPost = inits.isInitialized("boardPost") ? new QBoardPost(forProperty("boardPost"), inits.get("boardPost")) : null;
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

