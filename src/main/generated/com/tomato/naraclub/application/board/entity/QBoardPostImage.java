package com.tomato.naraclub.application.board.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardPostImage is a Querydsl query type for BoardPostImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardPostImage extends EntityPathBase<BoardPostImage> {

    private static final long serialVersionUID = -1394187237L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardPostImage boardPostImage = new QBoardPostImage("boardPostImage");

    public final com.tomato.naraclub.common.audit.QAudit _super = new com.tomato.naraclub.common.audit.QAudit(this);

    public final QBoardPost boardPost;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QBoardPostImage(String variable) {
        this(BoardPostImage.class, forVariable(variable), INITS);
    }

    public QBoardPostImage(Path<? extends BoardPostImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardPostImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardPostImage(PathMetadata metadata, PathInits inits) {
        this(BoardPostImage.class, metadata, inits);
    }

    public QBoardPostImage(Class<? extends BoardPostImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.boardPost = inits.isInitialized("boardPost") ? new QBoardPost(forProperty("boardPost"), inits.get("boardPost")) : null;
    }

}

