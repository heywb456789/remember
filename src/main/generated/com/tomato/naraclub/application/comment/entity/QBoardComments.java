package com.tomato.naraclub.application.comment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardComments is a Querydsl query type for BoardComments
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardComments extends EntityPathBase<BoardComments> {

    private static final long serialVersionUID = -976289445L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardComments boardComments = new QBoardComments("boardComments");

    public final com.tomato.naraclub.common.audit.QAudit _super = new com.tomato.naraclub.common.audit.QAudit(this);

    public final com.tomato.naraclub.application.member.entity.QMember author;

    public final com.tomato.naraclub.application.board.entity.QBoardPost boardPost;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QBoardComments(String variable) {
        this(BoardComments.class, forVariable(variable), INITS);
    }

    public QBoardComments(Path<? extends BoardComments> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardComments(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardComments(PathMetadata metadata, PathInits inits) {
        this(BoardComments.class, metadata, inits);
    }

    public QBoardComments(Class<? extends BoardComments> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
        this.boardPost = inits.isInitialized("boardPost") ? new com.tomato.naraclub.application.board.entity.QBoardPost(forProperty("boardPost"), inits.get("boardPost")) : null;
    }

}

